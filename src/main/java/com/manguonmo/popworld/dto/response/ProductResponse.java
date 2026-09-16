package com.manguonmo.popworld.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;

    private String name;

    private String slug;

    private String description;

    private BigDecimal singlePrice;

    private BigDecimal wholeSetPrice;

    private Integer stockQuantity;

    private String packagingType;

    private String secretRatio;

    private String material;

    private String sizeDimensions;

    private String categoryName;

    private String seriesName;

    private String mainImageUrl;

    private Boolean isFeatured;

    private Boolean isNewRelease;
}
