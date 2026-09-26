package com.manguonmo.popworld.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class UnboxRequest {

    @NotBlank(message = "Mã giữ hộp (reservation code) không được để trống")
    private String reservationCode;
}
