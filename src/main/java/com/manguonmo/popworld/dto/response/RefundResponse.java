package com.manguonmo.popworld.dto.response;

import com.manguonmo.popworld.entity.Refund;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundResponse {

    private Long id;
    private String refundCode;
    private Long orderId;
    private String orderCode;
    private BigDecimal amount;
    private String reason;
    private String processedBy;
    private LocalDateTime processedAt;
    private String status;
    private String refundMethod;

    public static RefundResponse fromEntity(Refund refund) {
        if (refund == null) {
            return null;
        }
        return RefundResponse.builder()
                .id(refund.getId())
                .refundCode(refund.getRefundCode())
                .orderId(refund.getOrder() != null ? refund.getOrder().getId() : null)
                .orderCode(refund.getOrder() != null ? refund.getOrder().getOrderCode() : null)
                .amount(refund.getAmount())
                .reason(refund.getReason())
                .processedBy(refund.getProcessedBy())
                .processedAt(refund.getProcessedAt())
                .status(refund.getStatus())
                .refundMethod(refund.getRefundMethod())
                .build();
    }
}
