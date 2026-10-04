package com.manguonmo.popworld.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistToggleRequest {

    @NotNull(message = "ID sản phẩm không được để trống")
    private Long productId;
}
