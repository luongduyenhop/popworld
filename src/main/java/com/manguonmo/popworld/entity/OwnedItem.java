package com.manguonmo.popworld.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "owned_items", indexes = {
        @Index(name = "idx_owned_user_status", columnList = "user_id, status"),
        @Index(name = "idx_owned_reservation", columnList = "reservation_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnedItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blind_box_item_id", nullable = false)
    private BlindBoxItem blindBoxItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Quan hệ 1-1 với BoxReservation đảm bảo mỗi phiếu bốc chỉ sinh ra duy nhất 1 vật phẩm sở hữu (Idempotency)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", unique = true)
    private BoxReservation reservation;

    @Column(name = "order_code", length = 50)
    private String orderCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private OwnedItemStatus status = OwnedItemStatus.IN_CABINET;

    @Column(name = "unboxed_at", nullable = false)
    private LocalDateTime unboxedAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;
}
