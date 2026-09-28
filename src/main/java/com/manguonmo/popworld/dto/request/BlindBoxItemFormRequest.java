package com.manguonmo.popworld.dto.request;

import com.manguonmo.popworld.entity.RarityType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlindBoxItemFormRequest {

    @NotBlank(message = "Tên mô hình không được để trống")
    @Size(max = 150, message = "Tên mô hình tối đa 150 ký tự")
    private String name;

    @NotNull(message = "Vui lòng chọn cấp độ hiếm (Rarity)")
    private RarityType rarity;

    @NotBlank(message = "Đường dẫn ảnh không được để trống")
    @Size(max = 500, message = "Đường dẫn ảnh tối đa 500 ký tự")
    private String imageUrl;

    @NotNull(message = "Trọng số xác suất không được để trống")
    @Min(value = 1, message = "Trọng số xác suất phải lớn hơn hoặc bằng 1")
    private Integer probabilityWeight;

    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    private Integer stockQuantity;

    @Builder.Default
    private Boolean active = true;
}
