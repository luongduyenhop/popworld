package com.manguonmo.popworld.repository;

import com.manguonmo.popworld.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {

    List<WishlistItem> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT w FROM WishlistItem w JOIN FETCH w.product p LEFT JOIN FETCH p.category WHERE w.user.id = :userId ORDER BY w.createdAt DESC")
    List<WishlistItem> findByUserIdWithProduct(@Param("userId") Long userId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    Optional<WishlistItem> findByUserIdAndProductId(Long userId, Long productId);

    void deleteByUserIdAndProductId(Long userId, Long productId);

    long countByUserId(Long userId);

    @Query("SELECT w.product.id FROM WishlistItem w WHERE w.user.id = :userId")
    Set<Long> findProductIdsByUserId(@Param("userId") Long userId);
}
