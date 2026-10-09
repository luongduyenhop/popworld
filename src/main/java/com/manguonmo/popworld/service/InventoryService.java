package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.response.InventorySummaryResponse;
import com.manguonmo.popworld.entity.Product;

import java.util.List;

public interface InventoryService {

    InventorySummaryResponse getInventorySummary();

    InventorySummaryResponse getInventorySummary(int lowStockThreshold);

    List<Product> getInventoryProducts(String statusFilter, String keyword);

    List<Product> getInventoryProducts(String statusFilter, String keyword, int lowStockThreshold);

    Product quickRestock(Long productId, int quantityToAdd, String note);

    Product adjustStock(Long productId, int newStockQuantity, String reason);

    List<com.manguonmo.popworld.dto.response.InventoryLogResponse> getProductStockHistory(Long productId);

    void recordStockLog(Product product, String type, int change, int oldStock, int newStock, String reason, String actor);
}
