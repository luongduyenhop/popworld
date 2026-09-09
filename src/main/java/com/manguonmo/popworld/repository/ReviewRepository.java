package com.manguonmo.popworld.repository;

import com.manguonmo.popworld.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductIdAndApprovedTrueOrderByCreatedAtDesc(Long productId);
    long countByProductIdAndApprovedTrue(Long productId);
}