package com.manguonmo.popworld.entity;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "character_ips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class CharacterIp extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true,length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(name = "banner_url",length = 255)
    private String bannerUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id",nullable = false)
    private Artist artist;
}
