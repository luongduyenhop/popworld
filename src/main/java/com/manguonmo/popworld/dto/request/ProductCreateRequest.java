package com.manguonmo.popworld.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCreateRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 200, message = "Tên sản phẩm tối đa 200 ký tự")
    private String name;

    @NotNull(message = "Danh mục bắt buộc phải chọn")
    private Long categoryId;

    private Long seriesId;

    @NotNull(message = "Giá hộp lẻ bắt buộc nhập")
    @DecimalMin(value = "1000", message = "Giá hộp lẻ tối thiểu 1.000 đ")
    private BigDecimal singlePrice;

    @DecimalMin(value = "1000", message = "Giá nguyên set tối thiểu 1.000 đ")
    private BigDecimal wholeSetPrice;

    @NotNull(message = "Số lượng tồn kho bắt buộc nhập")
    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    private Integer stockQuantity;

    private String packagingType;
    private String secretRatio;
    private String material;
    private String sizeDimensions;
    private String description;

    @Builder.Default
    private Boolean isFeatured = false;

    @Builder.Default
    private Boolean isNewRelease = true;

    @Builder.Default
    private Boolean active = true;

    private List<MultipartFile> imageFiles;
}
