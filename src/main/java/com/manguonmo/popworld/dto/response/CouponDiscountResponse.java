package com.manguonmo.popworld.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponDiscountResponse {
    private String couponCode;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal discountAmount;
    private BigDecimal subtotal;
    private BigDecimal newTotal;
    private String message;            
}