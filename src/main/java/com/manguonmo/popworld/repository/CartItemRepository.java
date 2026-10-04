package com.manguonmo.popworld.repository;

import com.manguonmo.popworld.entity.CartItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    @EntityGraph(attributePaths = {"product", "product.images"})
    List<CartItem> findByUserId(Long userId);

    @Query("SELECT COALESCE(SUM(c.quantity), 0) FROM CartItem c WHERE c.user.id = :userId")
    Integer countTotalQuantityByUserId(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"product", "product.images"})
    @Query("SELECT c FROM CartItem c WHERE c.user.id = :userId AND c.product.id = :productId AND c.purchaseType = :purchaseType")
    Optional<CartItem> findByUserIdAndProductIdAndPurchaseType(@Param("userId") Long userId,
                                                               @Param("productId") Long productId,
                                                               @Param("purchaseType") String purchaseType);
    @EntityGraph(attributePaths = {"product", "product.images"})
    @Query("SELECT c FROM CartItem c WHERE c.user.id = :userId AND c.isSelected = true")
    List<CartItem> findByUserIdAndIsSelectedTrue(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"product", "product.images"})
    @Query("SELECT c FROM CartItem c WHERE c.id = :id AND c.user.id = :userId")
    Optional<CartItem> findByIdAndUserId(@Param("id") Long id,
                                         @Param("userId") Long userId);

    void deleteByUserId(Long userId);
    void deleteByUserIdAndIsSelectedTrue(Long userId);
    void deleteByProductId(Long productId);
}