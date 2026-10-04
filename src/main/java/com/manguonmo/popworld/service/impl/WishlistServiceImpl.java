package com.manguonmo.popworld.service.impl;

import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.entity.WishlistItem;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.repository.ProductRepository;
import com.manguonmo.popworld.repository.UserRepository;
import com.manguonmo.popworld.repository.WishlistRepository;
import com.manguonmo.popworld.service.WishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public boolean toggleWishlist(Long userId, Long productId) {
        if (userId == null || productId == null) {
            throw new IllegalArgumentException("UserId và ProductId không được để trống!");
        }

        Optional<WishlistItem> existingOpt = wishlistRepository.findByUserIdAndProductId(userId, productId);
        if (existingOpt.isPresent()) {
            wishlistRepository.delete(existingOpt.get());
            log.info("Wishlist: User ID={} đã bỏ yêu thích Product ID={}", userId, productId);
            return false;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng ID: " + userId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm ID: " + productId));

        WishlistItem newItem = WishlistItem.builder()
                .user(user)
                .product(product)
                .build();
        wishlistRepository.save(newItem);
        log.info("Wishlist: User ID={} đã thêm Product ID={} ({}) vào danh sách yêu thích", userId, productId, product.getName());
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getWishlistProducts(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<WishlistItem> items = wishlistRepository.findByUserIdWithProduct(userId);
        return items.stream()
                .map(WishlistItem::getProduct)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> getWishlistProductIds(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        return wishlistRepository.findProductIdsByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isWishlisted(Long userId, Long productId) {
        if (userId == null || productId == null) {
            return false;
        }
        return wishlistRepository.existsByUserIdAndProductId(userId, productId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getWishlistCount(Long userId) {
        if (userId == null) {
            return 0;
        }
        return wishlistRepository.countByUserId(userId);
    }

    @Override
    @Transactional
    public void removeFromWishlist(Long userId, Long productId) {
        if (userId == null || productId == null) {
            return;
        }
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
        log.info("Wishlist: Đã xóa Product ID={} khỏi danh sách yêu thích của User ID={}", productId, userId);
    }
}
