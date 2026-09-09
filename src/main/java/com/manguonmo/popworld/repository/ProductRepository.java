package com.manguonmo.popworld.repository;

import com.manguonmo.popworld.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySlug(String slug);
    boolean existsBySlug(String slug);

    // Lọc sản phẩm nổi bật (Hot) cho trang chủ
    List<Product> findByIsFeaturedTrueAndActiveTrue();

    // Lọc hàng mới về (New Drop)
    List<Product> findByIsNewReleaseTrueAndActiveTrue();

    // Lọc theo danh mục
    List<Product> findByCategorySlugAndActiveTrue(String categorySlug);

    // Lọc theo Series
    List<Product> findBySeriesIdAndActiveTrue(Long seriesId);

    // Lấy toàn bộ sản phẩm đang active
    List<Product> findByActiveTrue();

    // Tìm kiếm theo từ khóa tên
    List<Product> findByNameContainingIgnoreCaseAndActiveTrue(String keyword);

    // Lọc theo Character IP
    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.series.characterIp.id = :characterIpId AND p.active = true")
    List<Product> findByCharacterIpIdAndActiveTrue(@org.springframework.data.repository.query.Param("characterIpId") Long characterIpId);
}