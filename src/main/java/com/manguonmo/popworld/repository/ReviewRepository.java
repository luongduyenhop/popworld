package com.manguonmo.popworld.repository;

import com.manguonmo.popworld.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProductIdAndApprovedTrueOrderByCreatedAtDesc(Long productId);

    long countByProductIdAndApprovedTrue(Long productId);

    long countByApprovedTrue();

    long countByApprovedFalse();

    @Query("SELECT r FROM Review r LEFT JOIN FETCH r.user LEFT JOIN FETCH r.product WHERE r.id = :id")
    Optional<Review> findByIdWithUserAndProduct(@Param("id") Long id);

    @Query("SELECT r FROM Review r LEFT JOIN FETCH r.user LEFT JOIN FETCH r.product ORDER BY r.createdAt DESC")
    List<Review> findAllWithUserAndProduct();

    @Query("SELECT r FROM Review r LEFT JOIN FETCH r.user LEFT JOIN FETCH r.product WHERE r.approved = :approved ORDER BY r.createdAt DESC")
    List<Review> findByApprovedWithUserAndProduct(@Param("approved") Boolean approved);

    @Query("SELECT r FROM Review r LEFT JOIN FETCH r.user u LEFT JOIN FETCH r.product p " +
           "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :kw, '%')) " +
           "   OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :kw, '%')) " +
           "   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :kw, '%')) " +
           "   OR LOWER(r.comment) LIKE LOWER(CONCAT('%', :kw, '%')) " +
           "ORDER BY r.createdAt DESC")
    List<Review> searchReviewsWithUserAndProduct(@Param("kw") String keyword);

    @Query("SELECT r FROM Review r LEFT JOIN FETCH r.user u LEFT JOIN FETCH r.product p " +
           "WHERE r.approved = :approved " +
           "  AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :kw, '%')) " +
           "       OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :kw, '%')) " +
           "       OR LOWER(u.email) LIKE LOWER(CONCAT('%', :kw, '%')) " +
           "       OR LOWER(r.comment) LIKE LOWER(CONCAT('%', :kw, '%'))) " +
           "ORDER BY r.createdAt DESC")
    List<Review> searchReviewsByApprovedWithUserAndProduct(@Param("approved") Boolean approved, @Param("kw") String keyword);

    @Query("SELECT AVG(r.rating) FROM Review r")
    Double calculateAverageRating();

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    @Query("SELECT r FROM Review r LEFT JOIN FETCH r.product WHERE r.user.id = :userId AND r.product.id = :productId")
    Optional<Review> findByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    @Query("SELECT r FROM Review r LEFT JOIN FETCH r.product WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
    List<Review> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
}