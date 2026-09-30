package com.manguonmo.popworld.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "box_reservations", indexes = {
        @Index(name = "idx_reservation_code", columnList = "reservation_code", unique = true),
        @Index(name = "idx_res_user_status", columnList = "user_id, status"),
        @Index(name = "idx_res_product_box", columnList = "product_id, box_index, status"),
        @Index(name = "idx_res_status_expires", columnList = "status, expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoxReservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "reservation_code", nullable = false, unique = true, length = 64)
    private String reservationCode;

    // Vị trí hộp trong khay (Ví dụ: 1 đến 12 trong nguyên set case)
    @Column(name = "box_index")
    private Integer boxIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id")
    private BlindBoxSlot slot;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.RESERVED;

    @Column(name = "reserved_at", nullable = false)
    private LocalDateTime reservedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // Mã đơn hàng nếu đã chuyển đổi sang Order
    @Column(name = "order_code", length = 50)
    private String orderCode;

    @Builder.Default
    @Column(name = "has_shaken")
    private Boolean hasShaken = false;

    @Builder.Default
    @Column(name = "has_used_hint_card")
    private Boolean hasUsedHintCard = false;

    // Danh sách tên nhân vật đã bị loại trừ bởi Shake & Hint Card (phân cách bằng dấu phẩy)
    @Column(name = "eliminated_characters", length = 255)
    private String eliminatedCharacters;

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}
