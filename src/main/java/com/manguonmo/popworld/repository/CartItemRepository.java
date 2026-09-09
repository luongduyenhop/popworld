package com.manguonmo.popworld.repository;

import com.manguonmo.popworld.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUserId(Long userId);
    Optional<CartItem> findByUserIdAndProductIdAndPurchaseType(Long userId, Long productId, String purchaseType);
    void deleteByUserId(Long userId);
    void deleteByUserIdAndIsSelectedTrue(Long userId);
}