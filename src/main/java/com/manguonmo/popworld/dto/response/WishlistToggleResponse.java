package com.manguonmo.popworld.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistToggleResponse {
    private boolean wishlisted;
    private long wishlistCount;
    private Long productId;
    private String message;
}
