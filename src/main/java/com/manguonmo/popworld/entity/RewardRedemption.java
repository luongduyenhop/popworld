package com.manguonmo.popworld.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reward_redemptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardRedemption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "reward_title", nullable = false, length = 200)
    private String rewardTitle;

    @Column(name = "points_cost", nullable = false)
    private Integer pointsCost;

    @Column(name = "redemption_code", nullable = false, unique = true, length = 50)
    private String redemptionCode;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 30)
    private String status = "CLAIMED"; // CLAIMED, SHIPPED, COMPLETED

    @Column(name = "notes", length = 500)
    private String notes;
}
