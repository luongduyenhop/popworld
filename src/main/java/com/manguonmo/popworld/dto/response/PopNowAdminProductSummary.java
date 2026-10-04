package com.manguonmo.popworld.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PopNowAdminProductSummary {

    private Long productId;
    private String productName;
    private String productSlug;
    private String categoryName;
    private Integer productStock;
    private Boolean productActive;
    private long totalItems;
    private long activeItems;
    private long availableSlots;
    private long heldSlots;
    private long soldSlots;
    private long totalSlots;
    private Integer boxesPerSet;

    public boolean isConfigReady() {
        int requiredSlots = boxesPerSet != null && boxesPerSet > 0 ? boxesPerSet : 6;
        return Boolean.TRUE.equals(productActive)
                && productStock != null && productStock > 0
                && activeItems > 0
                && totalSlots >= requiredSlots;
    }
}
