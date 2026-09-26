package com.manguonmo.popworld.repository;

import com.manguonmo.popworld.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    @org.springframework.data.jpa.repository.Query("SELECT c FROM CartItem c JOIN FETCH c.product WHERE c.user.id = :userId")
    List<CartItem> findByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM CartItem c JOIN FETCH c.product WHERE c.user.id = :userId AND c.product.id = :productId AND c.purchaseType = :purchaseType")
    Optional<CartItem> findByUserIdAndProductIdAndPurchaseType(@org.springframework.data.repository.query.Param("userId") Long userId,
                                                               @org.springframework.data.repository.query.Param("productId") Long productId,
                                                               @org.springframework.data.repository.query.Param("purchaseType") String purchaseType);
    @org.springframework.data.jpa.repository.Query("SELECT c FROM CartItem c JOIN FETCH c.product WHERE c.user.id = :userId AND c.isSelected = true")
    List<CartItem> findByUserIdAndIsSelectedTrue(@org.springframework.data.repository.query.Param("userId") Long userId);

    void deleteByUserId(Long userId);
    void deleteByUserIdAndIsSelectedTrue(Long userId);
    void deleteByProductId(Long productId);
}