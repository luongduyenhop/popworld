package com.manguonmo.popworld.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCreateRequest {

    @NotNull(message = "ID sản phẩm không được để trống!")
    private Long productId;

    @Builder.Default
    @Min(value = 1, message = "Số sao tối thiểu là 1 sao!")
    @Max(value = 5, message = "Số sao tối đa là 5 sao!")
    private Integer rating = 5;

    @Size(max = 1000, message = "Nội dung nhận xét tối đa 1000 ký tự!")
    private String comment;

    @Size(max = 255, message = "Đường dẫn ảnh unboxing không được vượt quá 255 ký tự!")
    @Pattern(
            regexp = "^$|^https?://[a-zA-Z0-9\\-._~:/?#\\[\\]@!$&'()*+,;%=]+$",
            message = "Đường dẫn ảnh chỉ chấp nhận URL tuyệt đối sử dụng giao thức http:// hoặc https:// hợp lệ!"
    )
    private String reviewImageUrl;
}
