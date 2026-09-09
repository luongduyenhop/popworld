package com.manguonmo.popworld.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "series")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Series extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "name", nullable = false, length = 150)
    private String name;


    @Column(name = "description", columnDefinition = "TEXT")
    private String description;


    @Column(name = "banner_url", length = 255)
    private String bannerUrl;


    @Column(name = "release_date")
    private LocalDate releaseDate;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_ip_id", nullable = false)
    private CharacterIp characterIp;
}