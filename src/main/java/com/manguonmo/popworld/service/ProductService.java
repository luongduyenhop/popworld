package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.response.ProductStatsResponse;
import com.manguonmo.popworld.entity.Product;

import java.util.List;
import java.util.Optional;

public interface ProductService {
    List<Product> getFeaturedProducts();

    List<Product> getNewReleases();

    Optional<Product> getProductBySlug(String slug);
    List<Product> getProductsByCategorySlug(String categorySlug);
    List<Product> getProductsByCharacterIp(Long characterIpId);
    List<Product> searchProducts(String keyword);
    List<Product> getAllActiveProducts();

    // Các phương thức phục vụ Quản trị Kho Hàng (Admin)
    List<Product> getAdminProducts(Long categoryId, String keyword);
    ProductStatsResponse getProductStats();
    Product updateStock(Long id, Integer stockQuantity);
    Product toggleActive(Long id);
}
