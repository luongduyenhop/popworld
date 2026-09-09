package com.manguonmo.popworld.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;


    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;

  
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}