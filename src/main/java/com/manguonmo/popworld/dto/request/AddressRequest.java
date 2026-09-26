package com.manguonmo.popworld.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressRequest {

    @NotBlank(message = "Tên người nhận không được để trống")
    private String recipientName;

    @NotBlank(message = "Số điện thoại người nhận không được để trống")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Số điện thoại không đúng định dạng")
    private String recipientPhone;

    @NotBlank(message = "Tỉnh / Thành phố không được để trống")
    private String provinceCity;

    @NotBlank(message = "Quận / Huyện không được để trống")
    private String district;

    private String ward;

    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    private String detailedAddress;

    @Builder.Default
    private Boolean isDefault = false;
}
