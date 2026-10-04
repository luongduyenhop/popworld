package com.manguonmo.popworld.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class InventorySummaryResponse {
    private long totalProductCount;
    private long totalStockUnits;
    private long outOfStockCount;
    private long lowStockCount;
    private long safeStockCount;
    private BigDecimal totalInventoryValue;
    private int lowStockThreshold;
}
