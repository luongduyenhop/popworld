package com.manguonmo.popworld.service;

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
}
