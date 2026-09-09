package com.manguonmo.popworld.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Đánh giá số sao: 1 đến 5 sao
    @Column(name = "rating", nullable = false)
    private Integer rating;

    // Nhận xét mở hộp / unboxing
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    // Ảnh chụp thực tế sau khi đập hộp
    @Column(name = "review_image_url", length = 255)
    private String reviewImageUrl;

    // Admin kiểm duyệt trước khi hiển thị ra ngoài trang chủ
    @Builder.Default
    @Column(name = "approved")
    private Boolean approved = false;
}