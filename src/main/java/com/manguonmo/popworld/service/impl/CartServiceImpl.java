package com.manguonmo.popworld.service.impl;

import com.manguonmo.popworld.service.CartService;
import com.manguonmo.popworld.entity.CartItem;
import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.repository.CartItemRepository;
import com.manguonmo.popworld.repository.ProductRepository;
import com.manguonmo.popworld.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartServiceImpl(CartItemRepository cartItemRepository,
                           ProductRepository productRepository,
                           UserRepository userRepository) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartItem> getCartItems(Long userId) {
        return cartItemRepository.findByUserId(userId);
    }

    @Override
    public CartItem addToCart(Long userId, Long productId, String purchaseType, int quantity) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng ID: " + userId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm ID: " + productId));

        String type = (purchaseType != null && purchaseType.equalsIgnoreCase("WHOLE_SET")) 
                ? "WHOLE_SET" : "SINGLE_BOX";

        Optional<CartItem> existingItemOpt = cartItemRepository
                .findByUserIdAndProductIdAndPurchaseType(userId, productId, type);

        if (existingItemOpt.isPresent()) {
            CartItem item = existingItemOpt.get();
            item.setQuantity(item.getQuantity() + quantity);
            return cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .user(user)
                    .product(product)
                    .purchaseType(type)
                    .quantity(Math.max(1, quantity))
                    .isSelected(true)
                    .build();
            return cartItemRepository.save(newItem);
        }
    }

    @Override
    public void updateQuantity(Long cartItemId, int quantity) {
        if (quantity <= 0) {
            cartItemRepository.deleteById(cartItemId);
        } else {
            cartItemRepository.findById(cartItemId).ifPresent(item -> {
                item.setQuantity(quantity);
                cartItemRepository.save(item);
            });
        }
    }

    @Override
    public void updateSelection(Long cartItemId, boolean isSelected) {
        cartItemRepository.findById(cartItemId).ifPresent(item -> {
            item.setIsSelected(isSelected);
            cartItemRepository.save(item);
        });
    }

    @Override
    public void removeFromCart(Long cartItemId) {
        cartItemRepository.deleteById(cartItemId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateSelectedTotal(Long userId) {
        List<CartItem> items = cartItemRepository.findByUserId(userId);
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem item : items) {
            if (Boolean.TRUE.equals(item.getIsSelected())) {
                BigDecimal unitPrice;
                if ("WHOLE_SET".equalsIgnoreCase(item.getPurchaseType()) && item.getProduct().getWholeSetPrice() != null) {
                    unitPrice = item.getProduct().getWholeSetPrice();
                } else {
                    unitPrice = item.getProduct().getSinglePrice();
                }
                BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
                total = total.add(itemTotal);
            }
        }
        return total;
    }

    @Override
    @Transactional(readOnly = true)
    public int getCartCount(Long userId) {
        List<CartItem> items = cartItemRepository.findByUserId(userId);
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    @Override
    @Transactional(readOnly = true)
    public User getDefaultUser() {
        return userRepository.findByEmail("user@popworld.com")
                .orElseGet(() -> userRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new IllegalStateException("Không có người dùng nào trong CSDL!")));
    }
}
