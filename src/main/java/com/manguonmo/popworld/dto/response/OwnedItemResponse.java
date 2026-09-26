package com.manguonmo.popworld.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnedItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private Long itemId;
    private String itemName;
    private String rarity;
    private String imageUrl;
    private String status;
    private LocalDateTime unboxedAt;
    private String reservationCode;
}
