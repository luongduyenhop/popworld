package com.manguonmo.popworld.repository;

import com.manguonmo.popworld.entity.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {
    Optional<News> findBySlugAndPublishedTrue(String slug);
    List<News> findByPublishedTrueOrderByCreatedAtDesc();
}