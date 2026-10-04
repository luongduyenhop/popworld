package com.manguonmo.popworld.service;

import com.manguonmo.popworld.entity.Product;

import java.util.List;
import java.util.Set;

public interface WishlistService {

    /**
     * Bật/tắt trạng thái yêu thích: Nếu chưa có thì thêm, nếu đã có thì xóa.
     * @return true nếu sau thao tác sản phẩm ở trạng thái ĐÃ YÊU THÍCH, false nếu ĐÃ BỎ YÊU THÍCH
     */
    boolean toggleWishlist(Long userId, Long productId);

    /**
     * Lấy danh sách sản phẩm được người dùng yêu thích (sắp xếp theo thời gian mới nhất).
     */
    List<Product> getWishlistProducts(Long userId);

    /**
     * Lấy tập hợp Product ID đã yêu thích để tô màu nhanh trên danh mục sản phẩm.
     */
    Set<Long> getWishlistProductIds(Long userId);

    /**
     * Kiểm tra 1 sản phẩm cụ thể đã được người dùng yêu thích hay chưa.
     */
    boolean isWishlisted(Long userId, Long productId);

    /**
     * Đếm tổng số sản phẩm trong danh sách yêu thích của người dùng.
     */
    long getWishlistCount(Long userId);

    /**
     * Xóa 1 sản phẩm khỏi danh sách yêu thích.
     */
    void removeFromWishlist(Long userId, Long productId);
}
