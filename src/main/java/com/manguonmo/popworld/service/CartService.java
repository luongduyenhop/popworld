package com.manguonmo.popworld.service;

import com.manguonmo.popworld.entity.CartItem;
import com.manguonmo.popworld.entity.User;

import java.math.BigDecimal;
import java.util.List;

public interface CartService {
    List<CartItem> getCartItems(Long userId);
    CartItem addToCart(Long userId, Long productId, String purchaseType, int quantity);
    void updateQuantity(Long userId, Long cartItemId, int quantity);
    void updateSelection(Long userId, Long cartItemId, boolean isSelected);
    void removeFromCart(Long userId, Long cartItemId);
    BigDecimal calculateSelectedTotal(Long userId);
    int getCartCount(Long userId);
}
