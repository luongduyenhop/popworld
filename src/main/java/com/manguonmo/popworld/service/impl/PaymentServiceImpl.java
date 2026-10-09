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
import com.manguonmo.popworld.security.crypto.HmacSignatureVerifier;
import lombok.extern.slf4j.Slf4j;
import com.manguonmo.popworld.entity.PaymentTransaction;
import com.manguonmo.popworld.repository.PaymentTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final Pattern ORDER_CODE_PATTERN = Pattern.compile("PW-?\\d+", Pattern.CASE_INSENSITIVE);
    private static final List<String> PAID_STATUSES = List.of(
            "PROCESSING", "SHIPPING", "DELIVERED", "SHIPPED", "COMPLETED"
    );

    @Value("${sepay.webhook.api-key:}")
    private String apiKey;

    @Value("${sepay.webhook.secret-key:${sepay.webhook.api-key:}}")
    private String webhookSecretKey;

    private HmacSignatureVerifier hmacSignatureVerifier;

    private final OrderRepository orderRepository;
    private final BoxReservationRepository boxReservationRepository;
    private final PopNowService popNowService;
    private final OrderTimelineRepository orderTimelineRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    public void setHmacSignatureVerifier(HmacSignatureVerifier hmacSignatureVerifier) {
        this.hmacSignatureVerifier = hmacSignatureVerifier;
    }

    public PaymentServiceImpl(OrderRepository orderRepository,
                              BoxReservationRepository boxReservationRepository,
                              PopNowService popNowService,
                              OrderTimelineRepository orderTimelineRepository) {
        this(orderRepository, boxReservationRepository, popNowService, orderTimelineRepository, null);
    }

    @Autowired
    public PaymentServiceImpl(OrderRepository orderRepository,
                              BoxReservationRepository boxReservationRepository,
                              PopNowService popNowService,
                              OrderTimelineRepository orderTimelineRepository,
                              PaymentTransactionRepository paymentTransactionRepository) {
        this.orderRepository = orderRepository;
        this.boxReservationRepository = boxReservationRepository;
        this.popNowService = popNowService;
        this.orderTimelineRepository = orderTimelineRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.hmacSignatureVerifier = new HmacSignatureVerifier();
    }

    @Override
    @Transactional
    public boolean processSePayWebhook(SePayWebhookRequest webhookData, String authorizationHeader) {
        return processSePayWebhook(webhookData, authorizationHeader, null, null);
    }

    @Override
    @Transactional
    public boolean processSePayWebhook(SePayWebhookRequest webhookData, String authorizationHeader, String rawPayload, String signatureHeader) {
        // 1. Xác thực bảo mật: Ưu tiên Chữ ký điện tử HMAC-SHA256, Fallback sang API Key
        boolean signatureVerified = false;
        if (signatureHeader != null && !signatureHeader.isBlank()) {
            HmacSignatureVerifier verifier = this.hmacSignatureVerifier != null ? this.hmacSignatureVerifier : new HmacSignatureVerifier();
            String effectiveSecret = (webhookSecretKey != null && !webhookSecretKey.isBlank()) ? webhookSecretKey : apiKey;

            boolean match = false;
            if (rawPayload != null && !rawPayload.isBlank()) {
                match = verifier.verifySignature(rawPayload, signatureHeader, effectiveSecret);
            }
            if (!match && webhookData != null) {
                String canonical = String.format("id=%s&amount=%s&content=%s",
                        webhookData.getId(),
                        webhookData.getTransferAmount(),
                        webhookData.getContent());
                match = verifier.verifySignature(canonical, signatureHeader, effectiveSecret);
            }

            if (match) {
                log.info("SePay Webhook: Xác thực chữ ký HMAC-SHA256 thành công.");
                signatureVerified = true;
            } else {
                log.warn("SePay Webhook: Chữ ký HMAC-SHA256 không hợp lệ hoặc payload bị chỉnh sửa!");
                return false;
            }
        }

        if (!signatureVerified) {
            if (apiKey == null || apiKey.trim().isEmpty() || authorizationHeader == null || authorizationHeader.trim().isEmpty()) {
                log.warn("SePay Webhook: Truy cập trái phép hoặc thiếu API Key / Signature hợp lệ.");
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
            log.warn("SePay Webhook: Không tìm thấy mã đơn hàng hợp lệ (PW-...) trong nội dung: {}", contentSource);
            return false;
        }
        String rawOrderCode = matcher.group().toUpperCase();
        String formattedWithHyphen = rawOrderCode.startsWith("PW-") ? rawOrderCode : "PW-" + rawOrderCode.substring(2);
        String formattedWithoutHyphen = rawOrderCode.startsWith("PW-") ? "PW" + rawOrderCode.substring(3) : rawOrderCode;

        // 4. Tìm kiếm đơn hàng trong CSDL (hỗ trợ cả định dạng có dấu gạch ngang và không có dấu gạch ngang do ngân hàng tự động lọc ký tự)
        Optional<Order> orderCheck = orderRepository.findByOrderCode(formattedWithHyphen);
        if (orderCheck.isEmpty()) {
            orderCheck = orderRepository.findByOrderCode(formattedWithoutHyphen);
        }
        if (orderCheck.isEmpty()) {
            orderCheck = orderRepository.findByOrderCode(rawOrderCode);
        }
        if (orderCheck.isEmpty()) {
            log.warn("SePay Webhook: Không tìm thấy đơn hàng với mã {} trong CSDL (đã đối soát cả {} và {})",
                    rawOrderCode, formattedWithHyphen, formattedWithoutHyphen);
            return false;
        }
        Order order = orderCheck.get();
        String orderCode = order.getOrderCode();

        // 5. Trích xuất mã giao dịch để bảo đảm tính bất biến lặp lại (Idempotency)
        String txCode = webhookData.getId() != null ? String.valueOf(webhookData.getId()) : webhookData.getReferenceCode();
        if (txCode == null || txCode.isBlank()) {
            txCode = webhookData.getCode();
        }

        // Nếu giao dịch này đã từng được xử lý -> Bỏ qua chống trùng lặp (Idempotent)
        if (txCode != null && !txCode.isBlank() && paymentTransactionRepository != null && paymentTransactionRepository.existsByTransactionCode(txCode)) {
            log.info("SePay Webhook: Giao dịch {} đã được ghi nhận trước đó. Bỏ qua chống trùng lặp (Idempotent).", txCode);
            return true;
        }

        BigDecimal transferAmount = webhookData.getTransferAmount();
        if (transferAmount == null || transferAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("SePay Webhook: Số tiền chuyển khoản không hợp lệ (null hoặc <= 0): {}", transferAmount);
            return false;
        }

        // 6. Kiểm tra trạng thái đơn hàng & Phân nhánh nghiệp vụ
        String currentStatus = order.getStatus() != null ? order.getStatus().toUpperCase() : "";

        if ("CANCELLED".equals(currentStatus) || "EXPIRED".equals(currentStatus)) {
            log.warn("SePay Webhook: Đơn hàng {} đã bị hủy hoặc hết hạn (trạng thái: {}). Không thể ghi nhận thanh toán tự động.", orderCode, currentStatus);
            savePaymentTransaction(order, webhookData, txCode, transferAmount, "EXPIRED_ORDER");
            if (orderTimelineRepository != null) {
                orderTimelineRepository.save(OrderTimeline.builder()
                        .order(order)
                        .fromStatus(currentStatus)
                        .toStatus(currentStatus)
                        .action("CẢNH BÁO: TIỀN VỀ ĐƠN HỦY/HẾT HẠN")
                        .actor("SePay Gateway")
                        .note("Nhận được " + transferAmount + " đ nhưng đơn đã " + currentStatus + ". Cần Admin đối soát hoàn tiền! Mã GD: " + txCode)
                        .build());
            }
            return false;
        }

        if (PAID_STATUSES.contains(currentStatus)) {
            log.info("SePay Webhook: Đơn hàng {} đã được xử lý thanh toán trước đó (trạng thái: {}). Bỏ qua xử lý lặp lại (Idempotent).", orderCode, currentStatus);

            // Nếu đây là giao dịch mới với mã txCode hợp lệ -> Lưu vết thanh toán thừa để Admin đối soát hoàn tiền
            savePaymentTransaction(order, webhookData, txCode, transferAmount, "OVERPAID");

            if (orderTimelineRepository != null && txCode != null && !txCode.isBlank()) {
                orderTimelineRepository.save(OrderTimeline.builder()
                        .order(order)
                        .fromStatus(currentStatus)
                        .toStatus(currentStatus)
                        .action("CẢNH BÁO: THANH TOÁN THỪA / LẶP LẠI (Cần hoàn tiền)")
                        .actor("SePay Gateway")
                        .note("Phát hiện giao dịch chuyển thêm: " + transferAmount + " đ khi đơn đã " + currentStatus + ". Mã GD: " + txCode + ". Đề nghị Admin đối soát hoàn tiền cho khách.")
                        .build());
            }

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

        // 7. Đối soát & Tích lũy số tiền (Cumulative Partial Payment)
        BigDecimal expectedAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal previousPaid = BigDecimal.ZERO;

        if (paymentTransactionRepository != null && order.getId() != null) {
            List<PaymentTransaction> prevTxs = paymentTransactionRepository.findByOrderIdOrderByCreatedAtDesc(order.getId());
            if (prevTxs != null) {
                for (PaymentTransaction pt : prevTxs) {
                    if (pt.getAmount() != null && "PARTIAL".equals(pt.getStatus())) {
                        previousPaid = previousPaid.add(pt.getAmount());
                    }
                }
            }
        }
        if (previousPaid.compareTo(BigDecimal.ZERO) == 0 && order.getPaidAmount() != null) {
            previousPaid = order.getPaidAmount();
        }

        BigDecimal newPaid = previousPaid.add(transferAmount);

        if (newPaid.compareTo(expectedAmount) < 0) {
            log.warn("SePay Webhook: Khách chuyển thiếu tiền cho đơn {}. Cần thanh toán: {}, Nhận lần này: {}, Tổng đã trả: {}",
                    orderCode, expectedAmount, transferAmount, newPaid);
            savePaymentTransaction(order, webhookData, txCode, transferAmount, "PARTIAL");

            if (orderTimelineRepository != null) {
                orderTimelineRepository.save(OrderTimeline.builder()
                        .order(order)
                        .fromStatus("TO_PAY")
                        .toStatus("TO_PAY")
                        .action("Thanh toán một phần (Chờ chuyển bù)")
                        .actor("SePay Gateway")
                        .note("Đã nhận: " + transferAmount + " đ. Lũy kế đã trả: " + newPaid + " / " + expectedAmount + " đ. Còn thiếu: " + expectedAmount.subtract(newPaid) + " đ. Mã GD: " + txCode)
                        .build());
            }
            return false;
        }

        // 8. Đã đủ tiền (newPaid >= expectedAmount) -> Đồng bộ POP NOW trước khi sang PROCESSING
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

        // 9. Cập nhật trạng thái đơn sang PROCESSING
        String referenceCode = webhookData.getReferenceCode();
        if (referenceCode == null || referenceCode.isBlank()) {
            referenceCode = webhookData.getCode();
        }
        if (referenceCode == null || referenceCode.isBlank()) {
            referenceCode = txCode != null ? txCode : "SEPAY-PAYMENT";
        }

        order.setStatus("PROCESSING");
        order.setPaidAt(LocalDateTime.now());
        order.setPaidAmount(newPaid);
        order.setNote(referenceCode);
        Order savedOrder = orderRepository.save(order);
        Order targetOrder = savedOrder != null ? savedOrder : order;

        boolean isOverpaid = newPaid.compareTo(expectedAmount) > 0;
        String txStatus = isOverpaid ? "OVERPAID" : "FULL";
        savePaymentTransaction(targetOrder, webhookData, txCode, transferAmount, txStatus);

        if (orderTimelineRepository != null) {
            String action = isOverpaid ? "Xác nhận thanh toán (Phát hiện chuyển thừa tiền)" : "Xác nhận thanh toán SePay thành công";
            String timelineNote = isOverpaid
                    ? "Mã giao dịch: " + referenceCode + " | Số tiền chuyển: " + transferAmount + " đ | Tổng đã nhận: " + newPaid + " đ (Thừa: " + newPaid.subtract(expectedAmount) + " đ cần hoàn tiền)"
                    : "Mã giao dịch: " + referenceCode + " | Số tiền chuyển: " + transferAmount + " đ | Lũy kế đã nhận: " + newPaid + " đ";

            try {
                orderTimelineRepository.save(OrderTimeline.builder()
                        .order(targetOrder)
                        .fromStatus("TO_PAY")
                        .toStatus("PROCESSING")
                        .action(action)
                        .actor("SePay Gateway")
                        .note(timelineNote)
                        .build());
            } catch (Exception e) {
                log.warn("Không thể lưu timeline thanh toán: {}", e.getMessage());
            }
        }

        log.info("SePay Webhook: Thanh toán thành công cho đơn hàng {}. Chuyển trạng thái sang PROCESSING. Lũy kế: {}/{}", orderCode, newPaid, expectedAmount);
        return true;
    }

    private void savePaymentTransaction(Order order, SePayWebhookRequest webhookData, String txCode, BigDecimal amount, String status) {
        if (paymentTransactionRepository != null && txCode != null && !txCode.isBlank()) {
            try {
                BigDecimal accumulated = (order != null && order.getPaidAmount() != null) ? order.getPaidAmount() : amount;
                paymentTransactionRepository.save(PaymentTransaction.builder()
                        .order(order)
                        .gateway("SEPAY")
                        .transactionCode(txCode)
                        .referenceCode(webhookData.getReferenceCode())
                        .accountNumber(webhookData.getAccountNumber())
                        .amount(amount)
                        .accumulatedAfter(accumulated)
                        .content(webhookData.getContent() != null ? webhookData.getContent() : webhookData.getDescription())
                        .status(status)
                        .transactionDate(LocalDateTime.now())
                        .build());
            } catch (Exception e) {
                log.warn("SePay Webhook: Không thể lưu PaymentTransaction: {}", e.getMessage());
            }
        }
    }
}
