package com.manguonmo.popworld.service.impl;

import com.manguonmo.popworld.service.PaymentService;
import com.manguonmo.popworld.dto.request.SePayWebhookRequest;
import com.manguonmo.popworld.entity.BoxReservation;
import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.OrderTimeline;
import com.manguonmo.popworld.entity.ReservationStatus;
import com.manguonmo.popworld.repository.BoxReservationRepository;
import com.manguonmo.popworld.repository.OrderRepository;
import com.manguonmo.popworld.repository.OrderTimelineRepository;
import com.manguonmo.popworld.service.PopNowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Pattern ORDER_CODE_PATTERN = Pattern.compile("PW-\\d+", Pattern.CASE_INSENSITIVE);
    private static final List<String> PAID_STATUSES = List.of(
            "PROCESSING", "SHIPPING", "DELIVERED", "SHIPPED", "COMPLETED"
    );

    @Value("${sepay.webhook.api-key}")
    private String apiKey;

    private final OrderRepository orderRepository;
    private final BoxReservationRepository boxReservationRepository;
    private final PopNowService popNowService;
    private final OrderTimelineRepository orderTimelineRepository;

    public PaymentServiceImpl(OrderRepository orderRepository,
                              BoxReservationRepository boxReservationRepository,
                              PopNowService popNowService,
                              OrderTimelineRepository orderTimelineRepository) {
        this.orderRepository = orderRepository;
        this.boxReservationRepository = boxReservationRepository;
        this.popNowService = popNowService;
        this.orderTimelineRepository = orderTimelineRepository;
    }

    @Override
    @Transactional
    public boolean processSePayWebhook(SePayWebhookRequest webhookData, String authorizationHeader) {
        // 1. Xác thực bảo mật: API Key và Authorization Header (chống bypass nếu apiKey null/rỗng, so khớp chính xác)
        if (apiKey == null || apiKey.trim().isEmpty() || authorizationHeader == null || authorizationHeader.trim().isEmpty()) {
            log.warn("SePay Webhook: Truy cập trái phép hoặc thiếu API Key hợp lệ.");
            return false;
        }

        String providedKey = authorizationHeader.trim();
        if (providedKey.regionMatches(true, 0, "Bearer ", 0, 7)) {
            providedKey = providedKey.substring(7).trim();
        } else if (providedKey.regionMatches(true, 0, "Apikey ", 0, 7)) {
            providedKey = providedKey.substring(7).trim();
        }

        if (!java.security.MessageDigest.isEqual(
                providedKey.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                apiKey.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            log.warn("SePay Webhook: Truy cập trái phép hoặc thiếu API Key hợp lệ.");
            return false;
        }

        // 2. Validate payload đầu vào
        if (webhookData == null) {
            log.warn("SePay Webhook: Dữ liệu payload null.");
            return false;
        }

        // 3. Trích xuất mã đơn hàng từ content hoặc description
        String contentSource = webhookData.getContent() != null ? webhookData.getContent() : webhookData.getDescription();
        if (contentSource == null || contentSource.isBlank()) {
            log.warn("SePay Webhook: Nội dung giao dịch trống, không thể xác định đơn hàng.");
            return false;
        }

        Matcher matcher = ORDER_CODE_PATTERN.matcher(contentSource);
        if (!matcher.find()) {
            log.warn("SePay Webhook: Không tìm thấy mã đơn hàng hợp lệ (PW-...) trong nội dung.");
            return false;
        }
        String orderCode = matcher.group().toUpperCase();

        // 4. Tìm kiếm đơn hàng trong CSDL
        Optional<Order> orderCheck = orderRepository.findByOrderCode(orderCode);
        if (orderCheck.isEmpty()) {
            log.warn("SePay Webhook: Không tìm thấy đơn hàng với mã {}", orderCode);
            return false;
        }
        Order order = orderCheck.get();

        // 5. Kiểm tra trạng thái đơn hàng & Tính bất biến (Idempotency)
        String currentStatus = order.getStatus() != null ? order.getStatus().toUpperCase() : "";

        if ("CANCELLED".equals(currentStatus) || "EXPIRED".equals(currentStatus)) {
            log.warn("SePay Webhook: Đơn hàng {} đã bị hủy hoặc hết hạn (trạng thái: {}). Không thể ghi nhận thanh toán.", orderCode, currentStatus);
            return false;
        }

        if (PAID_STATUSES.contains(currentStatus)) {
            log.info("SePay Webhook: Đơn hàng {} đã được xử lý thanh toán trước đó (trạng thái: {}). Bỏ qua xử lý lặp lại (Idempotent).", orderCode, currentStatus);
            if (boxReservationRepository != null && popNowService != null) {
                Optional<BoxReservation> resOpt = boxReservationRepository.findByOrderCode(orderCode);
                if (resOpt.isPresent()) {
                    BoxReservation res = resOpt.get();
                    if (res.getStatus() == ReservationStatus.EXPIRED || res.getStatus() == ReservationStatus.CANCELLED || res.isExpired()) {
                        log.error("SePay Webhook: Đơn hàng {} ở trạng thái {} nhưng phiếu giữ hộp {} đã bị hủy hoặc hết hạn (status: {}). Trả về false.",
                                orderCode, currentStatus, res.getReservationCode(), res.getStatus());
                        return false;
                    }
                    try {
                        popNowService.markPurchased(res.getReservationCode(), orderCode);
                        return true;
                    } catch (Exception e) {
                        log.error("SePay Webhook: Đơn hàng {} ở trạng thái {} nhưng markPurchased thất bại cho phiếu giữ hộp {}: {}",
                                orderCode, currentStatus, res.getReservationCode(), e.getMessage());
                        return false;
                    }
                }
            }
            return true;
        }

        if (!"TO_PAY".equals(currentStatus)) {
            log.warn("SePay Webhook: Đơn hàng {} ở trạng thái không hợp lệ để thanh toán: {}", orderCode, currentStatus);
            return false;
        }

        // 6. Đối soát số tiền (Amount Reconciliation)
        BigDecimal transferAmount = webhookData.getTransferAmount();
        BigDecimal expectedAmount = order.getTotalAmount();

        if (transferAmount == null || expectedAmount == null || transferAmount.compareTo(expectedAmount) < 0) {
            log.warn("SePay Webhook: Khách chuyển thiếu tiền hoặc số tiền không hợp lệ cho đơn {}. Cần thanh toán: {}, Nhận được: {}",
                    orderCode, expectedAmount, transferAmount);
            return false;
        }

        // 7. Đồng bộ thanh toán POP NOW trước khi cập nhật Order sang PROCESSING
        if (boxReservationRepository != null && popNowService != null) {
            Optional<BoxReservation> resOpt = boxReservationRepository.findByOrderCode(orderCode);
            if (resOpt.isPresent()) {
                BoxReservation res = resOpt.get();
                try {
                    popNowService.markPurchased(res.getReservationCode(), orderCode);
                    log.info("SePay Webhook: Đồng bộ thanh toán POP NOW thành công cho reservationCode={}", res.getReservationCode());
                } catch (Exception e) {
                    log.error("SePay Webhook: Thất bại khi hoàn tất thanh toán POP NOW cho đơn {}: {}", orderCode, e.getMessage());
                    order.setStatus("EXPIRED");
                    order.setNote("POP NOW: Thanh toán thất bại hoặc quá hạn giữ chỗ (" + e.getMessage() + ")");
                    orderRepository.save(order);
                    return false;
                }
            }
        }

        // 8. Cập nhật trạng thái đơn sang PROCESSING
        String referenceCode = webhookData.getReferenceCode();
        if (referenceCode == null || referenceCode.isBlank()) {
            referenceCode = webhookData.getCode();
        }
        if (referenceCode == null || referenceCode.isBlank()) {
            referenceCode = webhookData.getId() != null ? String.valueOf(webhookData.getId()) : "SEPAY-PAYMENT";
        }

        order.setStatus("PROCESSING");
        order.setPaidAt(LocalDateTime.now());
        order.setNote(referenceCode);
        Order savedOrder = orderRepository.save(order);

        if (orderTimelineRepository != null) {
            try {
                orderTimelineRepository.save(OrderTimeline.builder()
                        .order(savedOrder)
                        .fromStatus("TO_PAY")
                        .toStatus("PROCESSING")
                        .action("Xác nhận thanh toán SePay thành công")
                        .actor("SePay Gateway")
                        .note("Mã giao dịch: " + referenceCode + " | Số tiền chuyển: " + (webhookData.getTransferAmount() != null ? webhookData.getTransferAmount() : "0") + " đ")
                        .build());
            } catch (Exception e) {
                log.warn("Không thể lưu timeline thanh toán: {}", e.getMessage());
            }
        }

        log.info("SePay Webhook: Thanh toán thành công cho đơn hàng {}. Chuyển trạng thái sang PROCESSING.", orderCode);
        return true;
    }
}
