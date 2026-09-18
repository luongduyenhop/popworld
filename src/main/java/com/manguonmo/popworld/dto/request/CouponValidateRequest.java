package com.manguonmo.popworld.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    private BigDecimal subtotal;
}
