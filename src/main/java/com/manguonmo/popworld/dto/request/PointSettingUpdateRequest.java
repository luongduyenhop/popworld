package com.manguonmo.popworld.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointSettingUpdateRequest {

    @NotNull(message = "Tỷ lệ tích điểm không được để trống")
    @Min(value = 1000, message = "Số tiền mua hàng để tích 1 điểm phải tối thiểu 1.000 VNĐ")
    private Integer earnRate;

    @NotNull(message = "Tỷ lệ quy đổi điểm không được để trống")
    @Min(value = 1, message = "Giá trị quy đổi của 1 điểm phải tối thiểu 1 VNĐ")
    @Max(value = 10000, message = "Giá trị quy đổi của 1 điểm không được vượt quá 10.000 VNĐ")
    private Integer redeemRate;

    @NotNull(message = "Phần trăm giảm tối đa không được để trống")
    @Min(value = 0, message = "Phần trăm giảm tối đa không được âm")
    @Max(value = 100, message = "Phần trăm giảm tối đa không được vượt quá 100%")
    private Integer maxRedeemPercent;

    @Builder.Default
    private Boolean enabled = true;
}
