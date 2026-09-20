package com.manguonmo.popworld.service.impl;

import com.manguonmo.popworld.service.ProductService;
import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.repository.ProductRepository;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Optional;

@Service
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepo;

    public ProductServiceImpl( ProductRepository productRepo) {
        this.productRepo = productRepo;

    }

    @Override
    public List<Product> getFeaturedProducts() {
       return productRepo.findByIsFeaturedTrueAndActiveTrue();
    }

    @Override
    public List<Product> getNewReleases() {
        return productRepo.findByIsNewReleaseTrueAndActiveTrue();
    }

    @Override
    public Optional<Product> getProductBySlug(String slug) {
        return productRepo.findBySlug(slug);
    }

    @Override
    public List<Product> getProductsByCategorySlug(String categorySlug) {
        return productRepo.findByCategorySlugAndActiveTrue(categorySlug);
    }

    @Override
    public List<Product> getProductsByCharacterIp(Long characterIpId) {
        return productRepo.findByCharacterIpIdAndActiveTrue(characterIpId);
    }

    @Override
    public List<Product> searchProducts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return productRepo.findByActiveTrue();
        }
        return productRepo.findByNameContainingIgnoreCaseAndActiveTrue(keyword.trim());
    }

    @Override
    public List<Product> getAllActiveProducts() {
        return productRepo.findByActiveTrue();
    }
}
