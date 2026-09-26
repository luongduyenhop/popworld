package com.manguonmo.popworld.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlindBoxItemResponse {
    private Long id;
    private String name;
    private String rarity;
    private String imageUrl;
    private Boolean isSecret;
}
