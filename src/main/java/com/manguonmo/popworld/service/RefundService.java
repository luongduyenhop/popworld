package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.response.RefundResponse;
import com.manguonmo.popworld.entity.Refund;

import java.math.BigDecimal;
import java.util.List;

public interface RefundService {

    /**
     * Thực hiện hoàn tiền thủ công cho đơn hàng đủ điều kiện bởi Quản trị viên
     */
    Refund processManualRefund(String orderCode, BigDecimal amount, String reason, String processedBy);

    /**
     * Lấy lịch sử hoàn tiền theo mã đơn hàng
     */
    List<RefundResponse> getRefundsByOrderCode(String orderCode);

    /**
     * Lấy lịch sử hoàn tiền theo ID đơn hàng
     */
    List<RefundResponse> getRefundsByOrderId(Long orderId);

    /**
     * Tổng số tiền đã hoàn thành công cho đơn hàng
     */
    BigDecimal getTotalRefundedAmount(String orderCode);

    /**
     * Số tiền còn lại tối đa có thể hoàn
     */
    BigDecimal getRemainingRefundableAmount(String orderCode);

    /**
     * Kiểm tra đơn hàng có đủ điều kiện để hoàn tiền hay không
     */
    boolean isEligibleForRefund(String orderCode);
}
