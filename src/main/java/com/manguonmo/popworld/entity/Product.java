package com.manguonmo.popworld.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tên sản phẩm (ví dụ: Hộp Mù Hirono Little Mischief Series)
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    // Đường dẫn SEO URL (ví dụ: hirono-little-mischief-blind-box)
    @Column(name = "slug", nullable = false, unique = true, length = 200)
    private String slug;

    // Bài viết giới thiệu, câu chuyện sản phẩm
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Giá mua 1 hộp đơn lẻ (Single Box) - Bắt buộc lớn hơn 0
    @Column(name = "single_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal singlePrice;

    // Giá mua nguyên bộ cả thùng không trùng (Whole Set / Assorted Case)
    @Column(name = "whole_set_price", precision = 12, scale = 2)
    private BigDecimal wholeSetPrice;

    // Tồn kho (tính theo số hộp đơn)
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    // Quy cách đóng gói (ví dụ: 1 Box / 12 Boxes per Case)
    @Column(name = "packaging_type", length = 50)
    private String packagingType;

    // Tỉ lệ trúng mẫu hiếm Secret (ví dụ: 1/72 hoặc 1/144)
    @Column(name = "secret_ratio", length = 50)
    private String secretRatio;

    // Chất liệu (PVC / ABS / Vải bông Plush)
    @Column(name = "material", length = 100)
    private String material;

    // Kích thước (ví dụ: Height: 7.5cm - 9.5cm)
    @Column(name = "size_dimensions", length = 100)
    private String sizeDimensions;

    // Khóa ngoại: Thuộc danh mục nào (Bắt buộc)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // Khóa ngoại: Thuộc Series nào (Có thể null nếu là mẫu độc bản Mega)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id")
    private Series series;

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    private List<ProductImage> images = new ArrayList<>();

    // Cờ đánh dấu sản phẩm nổi bật (Hot / Featured) hiển thị trang chủ
    @Builder.Default
    @Column(name = "is_featured")
    private Boolean isFeatured = false;

    // Cờ đánh dấu hàng mới ra mắt (New Drop)
    @Builder.Default
    @Column(name = "is_new_release")
    private Boolean isNewRelease = true;

    // Trạng thái mở bán (true: đang bán, false: tạm ẩn)
    @Builder.Default
    @Column(name = "active")
    private Boolean active = true;

    public String getMainImageUrl() {
        if (images != null && !images.isEmpty()) {
            return images.stream()
                    .filter(img -> Boolean.TRUE.equals(img.getIsThumbnail()))
                    .map(ProductImage::getImageUrl)
                    .findFirst()
                    .orElse(images.get(0).getImageUrl());
        }
        return "https://placehold.co/400x400?text=No+Image";
    }
}