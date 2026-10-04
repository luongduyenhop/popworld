package com.manguonmo.popworld.service;

import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.entity.WishlistItem;
import com.manguonmo.popworld.repository.ProductRepository;
import com.manguonmo.popworld.repository.UserRepository;
import com.manguonmo.popworld.repository.WishlistRepository;
import com.manguonmo.popworld.service.impl.WishlistServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private WishlistServiceImpl wishlistService;

    private User sampleUser;
    private Product sampleProduct;
    private WishlistItem sampleItem;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder().id(1L).email("user@test.com").fullName("Test User").build();
        sampleProduct = Product.builder().id(10L).name("Hirono Little Mischief").singlePrice(BigDecimal.valueOf(280000)).build();
        sampleItem = WishlistItem.builder().id(100L).user(sampleUser).product(sampleProduct).build();
    }

    @Test
    @DisplayName("toggleWishlist: Khi chưa có trong danh sách -> Thêm mới và trả về true")
    void toggleWishlist_WhenNotExists_ShouldAddAndReturnTrue() {
        when(wishlistRepository.findByUserIdAndProductId(1L, 10L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(productRepository.findById(10L)).thenReturn(Optional.of(sampleProduct));
        when(wishlistRepository.save(any(WishlistItem.class))).thenReturn(sampleItem);

        boolean result = wishlistService.toggleWishlist(1L, 10L);

        assertTrue(result);
        verify(wishlistRepository).save(any(WishlistItem.class));
        verify(wishlistRepository, never()).delete(any());
    }

    @Test
    @DisplayName("toggleWishlist: Khi đã tồn tại trong danh sách -> Xóa bỏ và trả về false")
    void toggleWishlist_WhenAlreadyExists_ShouldDeleteAndReturnFalse() {
        when(wishlistRepository.findByUserIdAndProductId(1L, 10L)).thenReturn(Optional.of(sampleItem));

        boolean result = wishlistService.toggleWishlist(1L, 10L);

        assertFalse(result);
        verify(wishlistRepository).delete(sampleItem);
        verify(wishlistRepository, never()).save(any());
    }

    @Test
    @DisplayName("getWishlistProducts: Lấy danh sách sản phẩm yêu thích thành công")
    void getWishlistProducts_Success() {
        when(wishlistRepository.findByUserIdWithProduct(1L)).thenReturn(List.of(sampleItem));

        List<Product> products = wishlistService.getWishlistProducts(1L);

        assertNotNull(products);
        assertEquals(1, products.size());
        assertEquals("Hirono Little Mischief", products.get(0).getName());
    }

    @Test
    @DisplayName("getWishlistProductIds: Lấy tập hợp ID sản phẩm yêu thích")
    void getWishlistProductIds_Success() {
        when(wishlistRepository.findProductIdsByUserId(1L)).thenReturn(Set.of(10L, 20L));

        Set<Long> ids = wishlistService.getWishlistProductIds(1L);

        assertEquals(2, ids.size());
        assertTrue(ids.contains(10L));
    }

    @Test
    @DisplayName("isWishlisted: Kiểm tra trạng thái yêu thích")
    void isWishlisted_Success() {
        when(wishlistRepository.existsByUserIdAndProductId(1L, 10L)).thenReturn(true);
        when(wishlistRepository.existsByUserIdAndProductId(1L, 99L)).thenReturn(false);

        assertTrue(wishlistService.isWishlisted(1L, 10L));
        assertFalse(wishlistService.isWishlisted(1L, 99L));
    }

    @Test
    @DisplayName("getWishlistCount: Đếm số lượng sản phẩm yêu thích")
    void getWishlistCount_Success() {
        when(wishlistRepository.countByUserId(1L)).thenReturn(5L);

        assertEquals(5L, wishlistService.getWishlistCount(1L));
        assertEquals(0L, wishlistService.getWishlistCount(null));
    }

    @Test
    @DisplayName("removeFromWishlist: Xóa sản phẩm khỏi danh sách yêu thích")
    void removeFromWishlist_Success() {
        wishlistService.removeFromWishlist(1L, 10L);

        verify(wishlistRepository).deleteByUserIdAndProductId(1L, 10L);
    }
}
