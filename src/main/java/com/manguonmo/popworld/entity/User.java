package com.manguonmo.popworld.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "phone", length = 20)
    private String phone;

    @Builder.Default
    @Column(name = "reward_points")
    private Integer rewardPoints = 0;

    @Builder.Default
    @Column(name = "lucky_points")
    private Integer luckyPoints = 50;

    @Builder.Default
    @Column(name = "hint_cards")
    private Integer hintCards = 1;

    @Column(name = "last_check_in_date")
    private java.time.LocalDate lastCheckInDate;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "gender", length = 20)
    private String gender; // MALE, FEMALE, OTHER

    @Column(name = "birthday")
    private java.time.LocalDate birthday;

    @Column(name = "member_code", unique = true, length = 30)
    private String memberCode;

    @Builder.Default
    @Column(name = "membership_tier", length = 30)
    private String membershipTier = "MEMBER";

    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @Builder.Default
    @Column(name = "enabled")
    private Boolean enabled = true;
}