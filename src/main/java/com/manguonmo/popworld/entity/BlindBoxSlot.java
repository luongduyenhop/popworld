package com.manguonmo.popworld.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "blind_box_slots",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_product_slot", columnNames = {"product_id", "slot_index"})
        },
        indexes = {
                @Index(name = "idx_slot_product_status", columnList = "product_id, status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlindBoxSlot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Vị trí ô hộp trong set/khay (ví dụ: 1 đến 12)
    @Column(name = "slot_index", nullable = false)
    private Integer slotIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SlotStatus status = SlotStatus.AVAILABLE;

    // Phiếu giữ hộp hiện tại đang chiếm giữ ô này (nếu có)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_reservation_id")
    private BoxReservation currentReservation;
}
