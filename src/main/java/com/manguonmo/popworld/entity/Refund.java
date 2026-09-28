package com.manguonmo.popworld.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refunds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Refund extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "refund_code", nullable = false, unique = true, length = 40)
    private String refundCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "processed_by", nullable = false, length = 100)
    private String processedBy;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 30)
    private String status = "COMPLETED";

    @Builder.Default
    @Column(name = "refund_method", length = 50)
    private String refundMethod = "MANUAL";
}
