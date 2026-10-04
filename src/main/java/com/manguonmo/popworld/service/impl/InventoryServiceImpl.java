package com.manguonmo.popworld.service.impl;

import com.manguonmo.popworld.dto.response.InventorySummaryResponse;
import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.repository.ProductRepository;
import com.manguonmo.popworld.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public InventorySummaryResponse getInventorySummary() {
        return getInventorySummary(10);
    }

    @Override
    @Transactional(readOnly = true)
    public InventorySummaryResponse getInventorySummary(int lowStockThreshold) {
        int threshold = lowStockThreshold > 0 ? lowStockThreshold : 10;
        List<Product> products = productRepository.findAll();

        long totalCount = products.size();
        long totalUnits = 0;
        long outOfStock = 0;
        long lowStock = 0;
        long safeStock = 0;
        BigDecimal totalVal = BigDecimal.ZERO;

        for (Product p : products) {
            int stock = p.getStockQuantity() != null ? p.getStockQuantity() : 0;
            totalUnits += stock;
            if (stock <= 0) {
                outOfStock++;
            } else if (stock <= threshold) {
                lowStock++;
            } else {
                safeStock++;
            }

            if (p.getSinglePrice() != null && stock > 0) {
                totalVal = totalVal.add(p.getSinglePrice().multiply(BigDecimal.valueOf(stock)));
            }
        }

        return InventorySummaryResponse.builder()
                .totalProductCount(totalCount)
                .totalStockUnits(totalUnits)
                .outOfStockCount(outOfStock)
                .lowStockCount(lowStock)
                .safeStockCount(safeStock)
                .totalInventoryValue(totalVal)
                .lowStockThreshold(threshold)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getInventoryProducts(String statusFilter, String keyword) {
        return getInventoryProducts(statusFilter, keyword, 10);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getInventoryProducts(String statusFilter, String keyword, int lowStockThreshold) {
        int threshold = lowStockThreshold > 0 ? lowStockThreshold : 10;
        List<Product> products = new ArrayList<>(productRepository.findAll());

        // Sort ascending by stock quantity so out-of-stock and low-stock items appear on top
        products.sort(Comparator.comparingInt(p -> (p.getStockQuantity() != null ? p.getStockQuantity() : 0)));

        String normKeyword = keyword != null ? keyword.trim().toLowerCase() : "";

        return products.stream()
                .filter(p -> {
                    if (normKeyword.isEmpty()) return true;
                    boolean matchesName = p.getName() != null && p.getName().toLowerCase().contains(normKeyword);
                    boolean matchesCategory = p.getCategory() != null && p.getCategory().getName() != null &&
                            p.getCategory().getName().toLowerCase().contains(normKeyword);
                    return matchesName || matchesCategory;
                })
                .filter(p -> {
                    int stock = p.getStockQuantity() != null ? p.getStockQuantity() : 0;
                    if ("OUT_OF_STOCK".equalsIgnoreCase(statusFilter)) {
                        return stock <= 0;
                    } else if ("LOW_STOCK".equalsIgnoreCase(statusFilter)) {
                        return stock > 0 && stock <= threshold;
                    } else if ("SAFE".equalsIgnoreCase(statusFilter)) {
                        return stock > threshold;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Product quickRestock(Long productId, int quantityToAdd, String note) {
        if (quantityToAdd <= 0) {
            throw new IllegalArgumentException("Số lượng nhập thêm phải lớn hơn 0");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm Art Toy ID=" + productId));

        int oldStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        int newStock = oldStock + quantityToAdd;
        product.setStockQuantity(newStock);

        Product saved = productRepository.save(product);
        log.info("Nhập kho bổ sung cho sản phẩm #{} ({}): {} -> {} (+{}). Ghi chú: {}",
                saved.getId(), saved.getName(), oldStock, newStock, quantityToAdd, note);

        return saved;
    }

    @Override
    @Transactional
    public Product adjustStock(Long productId, int newStockQuantity, String reason) {
        if (newStockQuantity < 0) {
            throw new IllegalArgumentException("Số lượng tồn kho không được nhỏ hơn 0");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm Art Toy ID=" + productId));

        int oldStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        product.setStockQuantity(newStockQuantity);

        Product saved = productRepository.save(product);
        log.info("Điều chỉnh kiểm kê tồn kho sản phẩm #{} ({}): {} -> {}. Lý do: {}",
                saved.getId(), saved.getName(), oldStock, newStockQuantity, reason);

        return saved;
    }
}
