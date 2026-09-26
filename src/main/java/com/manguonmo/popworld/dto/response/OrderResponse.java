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
    private BigDecimal pointsDiscount;
    private Integer pointsUsed;
    private Integer pointsEarned;

    private BigDecimal totalAmount;


    private String status;

    private String paymentMethod;

    private String detailedAddress;

    private String ward;

    private String district;

    private String provinceCity;

    private LocalDateTime paidAt;

    private LocalDateTime createdAt;

    private List<OrderItemResponse> items;

}
