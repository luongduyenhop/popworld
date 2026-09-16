package com.manguonmo.popworld.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private String orderCode;

    private String recipientName;

    private String recipientPhone;

    private String fullAddress;

    private BigDecimal subtotalAmount;

    private BigDecimal shippingFee;

    private BigDecimal discountAmount;

    private BigDecimal totalAmount;

    private String status;

    private String paymentMethod;

    private LocalDateTime createdAt;

    private List<OrderItemResponse> items;

}
