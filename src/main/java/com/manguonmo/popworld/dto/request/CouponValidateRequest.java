package com.manguonmo.popworld.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponValidateRequest {

    @NotBlank(message = "Vui lòng nhập mã giảm giá")
    private String couponCode;

    @NotNull(message = "Giá trị đơn hàng không được để trống")
    @PositiveOrZero(message = "Giá trị đơn hàng không được âm")
    private BigDecimal subtotal;
}
