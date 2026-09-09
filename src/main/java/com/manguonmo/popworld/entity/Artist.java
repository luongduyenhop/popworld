package com.manguonmo.popworld.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "artists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Artist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tên nghệ sĩ (ví dụ: Kasing Lung, Kenny Wong, Ayan Deng)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    // Ảnh chân dung nghệ sĩ
    @Column(name = "avatar", length = 255)
    private String avatar;

    // Tiểu sử và phong cách nghệ thuật
    @Column(name = "biography", columnDefinition = "TEXT")
    private String biography;
}