package com.manguonmo.popworld.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "blind_box_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlindBoxItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "rarity", nullable = false, length = 20)
    @Builder.Default
    private RarityType rarity = RarityType.REGULAR;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    // Trọng số xác suất xuất hiện khi unbox (Mặc định 100 cho mẫu REGULAR, 10 cho mẫu SECRET)
    @Column(name = "probability_weight", nullable = false)
    @Builder.Default
    private Integer probabilityWeight = 100;

    // Giới hạn tồn kho riêng biệt cho mẫu này (nếu có, null nghĩa là lấy theo tổng tồn kho của Product)
    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;
}
