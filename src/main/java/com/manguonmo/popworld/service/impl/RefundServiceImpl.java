package com.manguonmo.popworld.service.impl;

import com.manguonmo.popworld.dto.response.RefundResponse;
import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.Refund;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.repository.OrderRepository;
import com.manguonmo.popworld.repository.RefundRepository;
import com.manguonmo.popworld.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final RefundRepository refundRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public Refund processManualRefund(String orderCode, BigDecimal amount, String reason, String processedBy) {
        if (orderCode == null || orderCode.trim().isEmpty()) {
            throw new BadRequestException("Mã đơn hàng không được để trống.");
        }

        Order order = orderRepository.findByOrderCode(orderCode.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderCode));

        // 1. Kiểm tra điều kiện thanh toán của đơn hàng
        if (order.getPaidAt() == null) {
            throw new BadRequestException("Đơn hàng chưa được thanh toán, không thể thực hiện hoàn tiền.");
        }

        String currentStatus = order.getStatus() != null ? order.getStatus().trim().toUpperCase() : "";
        if ("TO_PAY".equals(currentStatus) || "EXPIRED".equals(currentStatus)) {
            throw new BadRequestException("Đơn hàng ở trạng thái " + currentStatus + ", không thể thực hiện hoàn tiền.");
        }

        // 2. Kiểm tra số tiền hoàn
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Số tiền hoàn phải lớn hơn 0.");
        }

        BigDecimal totalRefunded = refundRepository.sumCompletedRefundAmountByOrderId(order.getId());
        if (totalRefunded == null) {
            totalRefunded = BigDecimal.ZERO;
        }

        BigDecimal orderTotal = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal remainingRefundable = orderTotal.subtract(totalRefunded);

        if (remainingRefundable.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Đơn hàng đã được hoàn tiền đầy đủ (100%), không thể hoàn thêm.");
        }

        if (amount.compareTo(remainingRefundable) > 0) {
            throw new BadRequestException(String.format(
                    "Số tiền hoàn (%,.0f đ) vượt quá số tiền còn lại có thể hoàn (%,.0f đ).",
                    amount, remainingRefundable));
        }

        // 3. Khởi tạo bản ghi hoàn tiền an toàn
        String adminUser = (processedBy != null && !processedBy.trim().isEmpty()) ? processedBy.trim() : "ADMIN";
        String cleanReason = (reason != null && !reason.trim().isEmpty()) ? reason.trim() : "Hoàn tiền thủ công bởi Quản trị viên";
        String refundCode = "RF-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 900 + 100);

        String refundMethod = "COD".equalsIgnoreCase(order.getPaymentMethod()) ? "MANUAL_OFFLINE" : "MANUAL_ONLINE";

        Refund refund = Refund.builder()
                .refundCode(refundCode)
                .order(order)
                .amount(amount)
                .reason(cleanReason)
                .processedBy(adminUser)
                .processedAt(LocalDateTime.now())
                .status("COMPLETED")
                .refundMethod(refundMethod)
                .build();

        Refund savedRefund = refundRepository.save(refund);

        log.info("Hoàn tiền thành công: refundCode={}, orderCode={}, amount={}, admin={}, method={}",
                refundCode, order.getOrderCode(), amount, adminUser, refundMethod);

        return savedRefund;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundResponse> getRefundsByOrderCode(String orderCode) {
        if (orderCode == null || orderCode.trim().isEmpty()) {
            return List.of();
        }
        return refundRepository.findByOrderOrderCodeOrderByProcessedAtDesc(orderCode.trim().toUpperCase())
                .stream()
                .map(RefundResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundResponse> getRefundsByOrderId(Long orderId) {
        if (orderId == null) {
            return List.of();
        }
        return refundRepository.findByOrderIdOrderByProcessedAtDesc(orderId)
                .stream()
                .map(RefundResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalRefundedAmount(String orderCode) {
        if (orderCode == null || orderCode.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = refundRepository.sumCompletedRefundAmountByOrderCode(orderCode.trim().toUpperCase());
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getRemainingRefundableAmount(String orderCode) {
        if (orderCode == null || orderCode.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        Order order = orderRepository.findByOrderCode(orderCode.trim().toUpperCase()).orElse(null);
        if (order == null || order.getTotalAmount() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal refunded = getTotalRefundedAmount(orderCode);
        BigDecimal remaining = order.getTotalAmount().subtract(refunded);
        return remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEligibleForRefund(String orderCode) {
        if (orderCode == null || orderCode.trim().isEmpty()) {
            return false;
        }
        Order order = orderRepository.findByOrderCode(orderCode.trim().toUpperCase()).orElse(null);
        if (order == null || order.getPaidAt() == null) {
            return false;
        }
        String status = order.getStatus() != null ? order.getStatus().trim().toUpperCase() : "";
        if ("TO_PAY".equals(status) || "EXPIRED".equals(status)) {
            return false;
        }
        return getRemainingRefundableAmount(orderCode).compareTo(BigDecimal.ZERO) > 0;
    }
}
