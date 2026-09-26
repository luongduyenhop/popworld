package com.manguonmo.popworld.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoxReservationRequest {

    @NotNull(message = "ID sản phẩm không được để trống")
    private Long productId;

    @Min(value = 1, message = "Vị trí hộp trong khay tối thiểu từ 1")
    private Integer boxIndex;
}
