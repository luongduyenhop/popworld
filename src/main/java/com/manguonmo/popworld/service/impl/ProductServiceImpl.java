package com.manguonmo.popworld.service.impl;

import com.manguonmo.popworld.dto.response.ProductStatsResponse;
import com.manguonmo.popworld.entity.Category;
import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.repository.CategoryRepository;
import com.manguonmo.popworld.repository.ProductRepository;
import com.manguonmo.popworld.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepository;

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

    @Override
    public List<Product> getAdminProducts(Long categoryId, String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return productRepo.findByNameContainingIgnoreCaseAndActiveTrue(keyword.trim());
        } else if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId).orElse(null);
            if (category != null) {
                return productRepo.findByCategorySlugAndActiveTrue(category.getSlug());
            }
        }
        return productRepo.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public ProductStatsResponse getProductStats() {
        return ProductStatsResponse.builder()
                .totalCount(productRepo.count())
                .activeCount(productRepo.countByActiveTrue())
                .lowStockCount(productRepo.countByStockQuantityLessThanEqual(10))
                .build();
    }

    @Override
    @Transactional
    public Product updateStock(Long id, Integer stockQuantity) {
        if (stockQuantity == null || stockQuantity < 0) {
            throw new BadRequestException("Số lượng tồn kho không hợp lệ!");
        }
        Product product = productRepo.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy sản phẩm với id: " + id)
        );
        product.setStockQuantity(stockQuantity);
        return productRepo.save(product);
    }

    @Override
    @Transactional
    public Product toggleActive(Long id) {
        Product product = productRepo.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy sản phẩm với id: " + id)
        );
        boolean newStatus = !Boolean.TRUE.equals(product.getActive());
        product.setActive(newStatus);
        return productRepo.save(product);
    }
}
