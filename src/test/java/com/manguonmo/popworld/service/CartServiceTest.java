package com.manguonmo.popworld.service;

import com.manguonmo.popworld.entity.CartItem;
import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.OutOfStockException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.repository.CartItemRepository;
import com.manguonmo.popworld.repository.ProductRepository;
import com.manguonmo.popworld.repository.UserRepository;
import com.manguonmo.popworld.service.impl.CartServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    @Test
    @DisplayName("Thêm sản phẩm mới vào giỏ hàng thành công")
    void test_AddToCart_NewItem() {
        User user = User.builder().id(1L).email("user@popworld.com").build();
        Product product = Product.builder().id(10L).name("Labubu").singlePrice(new BigDecimal("350000")).stockQuantity(100).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByUserIdAndProductIdAndPurchaseType(1L, 10L, "SINGLE_BOX")).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartItem saved = cartService.addToCart(1L, 10L, "SINGLE_BOX", 2);

        assertNotNull(saved);
        assertEquals(2, saved.getQuantity());
        assertEquals("SINGLE_BOX", saved.getPurchaseType());
        verify(cartItemRepository, times(1)).save(any(CartItem.class));
    }

    @Test
    @DisplayName("Thêm sản phẩm với số lượng <= 0 -> Ném BadRequestException")
    void test_AddToCart_QuantityZeroOrNegative_ThrowsBadRequest() {
        assertThrows(BadRequestException.class, () -> cartService.addToCart(1L, 10L, "SINGLE_BOX", 0));
        assertThrows(BadRequestException.class, () -> cartService.addToCart(1L, 10L, "SINGLE_BOX", -2));
        verifyNoInteractions(cartItemRepository);
    }

    @Test
    @DisplayName("Thêm sản phẩm vượt quá tồn kho -> Ném OutOfStockException")
    void test_AddToCart_OutOfStock_ThrowsOutOfStockException() {
        User user = User.builder().id(1L).build();
        Product product = Product.builder().id(10L).name("Labubu").stockQuantity(5).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByUserIdAndProductIdAndPurchaseType(1L, 10L, "SINGLE_BOX")).thenReturn(Optional.empty());

        assertThrows(OutOfStockException.class, () -> cartService.addToCart(1L, 10L, "SINGLE_BOX", 10));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Thêm WHOLE_SET (12 hộp) vượt quá tồn kho -> Ném OutOfStockException")
    void test_AddToCart_WholeSet_OutOfStock_ThrowsOutOfStockException() {
        User user = User.builder().id(1L).build();
        Product product = Product.builder().id(10L).name("Labubu Set").stockQuantity(20).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByUserIdAndProductIdAndPurchaseType(1L, 10L, "WHOLE_SET")).thenReturn(Optional.empty());

        // 2 sets * 12 boxes = 24 boxes > 20 available
        assertThrows(OutOfStockException.class, () -> cartService.addToCart(1L, 10L, "WHOLE_SET", 2));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Thêm sản phẩm đã bị vô hiệu hóa (active = false) -> Ném BadRequestException")
    void test_AddToCart_InactiveProduct_ThrowsBadRequestException() {
        User user = User.builder().id(1L).build();
        Product inactiveProduct = Product.builder().id(10L).name("Disabled Toy").active(false).stockQuantity(50).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(inactiveProduct));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> cartService.addToCart(1L, 10L, "SINGLE_BOX", 1));
        assertTrue(ex.getMessage().contains("tạm dừng mở bán"));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tính tổng tiền các món được chọn trong giỏ")
    void test_CalculateSelectedTotal() {
        Product p1 = Product.builder().id(1L).singlePrice(new BigDecimal("100000")).build();
        Product p2 = Product.builder().id(2L).wholeSetPrice(new BigDecimal("600000")).singlePrice(new BigDecimal("120000")).build();

        CartItem item1 = CartItem.builder().product(p1).quantity(2).purchaseType("SINGLE_BOX").isSelected(true).build(); // 200,000
        CartItem item2 = CartItem.builder().product(p2).quantity(1).purchaseType("WHOLE_SET").isSelected(true).build();  // 600,000
        CartItem item3 = CartItem.builder().product(p1).quantity(5).purchaseType("SINGLE_BOX").isSelected(false).build(); // Unselected

        when(cartItemRepository.findByUserId(1L)).thenReturn(List.of(item1, item2, item3));

        BigDecimal total = cartService.calculateSelectedTotal(1L);

        assertEquals(new BigDecimal("800000"), total);
    }

    @Test
    @DisplayName("Không có món nào được chọn trong giỏ -> calculateSelectedTotal trả về 0")
    void test_CalculateSelectedTotal_NoneSelected_ReturnsZero() {
        Product p1 = Product.builder().id(1L).singlePrice(new BigDecimal("100000")).build();
        CartItem item = CartItem.builder().product(p1).quantity(2).isSelected(false).build();

        when(cartItemRepository.findByUserId(1L)).thenReturn(List.of(item));

        BigDecimal total = cartService.calculateSelectedTotal(1L);

        assertEquals(BigDecimal.ZERO, total);
    }

    @Test
    @DisplayName("IDOR Check: updateQuantity - Chủ sở hữu hợp lệ (Owner) được phép cập nhật số lượng")
    void updateQuantity_WhenOwner_ShouldUpdateQuantity() {
        User owner = User.builder().id(1L).build();
        CartItem item = CartItem.builder().id(10L).user(owner).quantity(2).build();

        when(cartItemRepository.findById(10L)).thenReturn(Optional.of(item));

        cartService.updateQuantity(1L, 10L, 5);

        assertEquals(5, item.getQuantity());
        verify(cartItemRepository, times(1)).save(item);
    }

    @Test
    @DisplayName("IDOR Prevention: updateQuantity - User B cố sửa giỏ của User A -> Bị chặn và state KHÔNG thay đổi")
    void updateQuantity_WhenOtherUser_ShouldThrowBadRequestAndNotModifyState() {
        User userA = User.builder().id(1L).build();
        CartItem item = CartItem.builder().id(10L).user(userA).quantity(2).build();

        when(cartItemRepository.findById(10L)).thenReturn(Optional.of(item));

        // User B (id = 2) cố tình cập nhật CartItem #10 của User A
        assertThrows(BadRequestException.class, () -> cartService.updateQuantity(2L, 10L, 99));

        // State bất biến: số lượng vẫn giữ nguyên là 2, không gọi save hay delete
        assertEquals(2, item.getQuantity());
        verify(cartItemRepository, never()).save(any());
        verify(cartItemRepository, never()).delete(any());
    }

    @Test
    @DisplayName("updateQuantity: Số lượng <= 0 thì tự động xóa món khỏi giỏ của chủ sở hữu")
    void updateQuantity_WhenQuantityZeroOrNegative_ShouldDelete() {
        User owner = User.builder().id(1L).build();
        CartItem item = CartItem.builder().id(10L).user(owner).quantity(2).build();

        when(cartItemRepository.findById(10L)).thenReturn(Optional.of(item));

        cartService.updateQuantity(1L, 10L, 0);

        verify(cartItemRepository, times(1)).delete(item);
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("IDOR Check: updateSelection - Chủ sở hữu được phép chọn/bỏ chọn món")
    void updateSelection_WhenOwner_ShouldUpdateSelection() {
        User owner = User.builder().id(1L).build();
        CartItem item = CartItem.builder().id(10L).user(owner).isSelected(true).build();

        when(cartItemRepository.findById(10L)).thenReturn(Optional.of(item));

        cartService.updateSelection(1L, 10L, false);

        assertFalse(item.getIsSelected());
        verify(cartItemRepository, times(1)).save(item);
    }

    @Test
    @DisplayName("IDOR Prevention: updateSelection - User B cố đổi trạng thái chọn của User A -> Bị từ chối")
    void updateSelection_WhenOtherUser_ShouldThrowBadRequestAndNotModifyState() {
        User userA = User.builder().id(1L).build();
        CartItem item = CartItem.builder().id(10L).user(userA).isSelected(true).build();

        when(cartItemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThrows(BadRequestException.class, () -> cartService.updateSelection(2L, 10L, false));

        assertTrue(item.getIsSelected());
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("IDOR Check: removeFromCart - Chủ sở hữu được phép xóa món khỏi giỏ")
    void removeFromCart_WhenOwner_ShouldDelete() {
        User owner = User.builder().id(1L).build();
        CartItem item = CartItem.builder().id(10L).user(owner).build();

        when(cartItemRepository.findById(10L)).thenReturn(Optional.of(item));

        cartService.removeFromCart(1L, 10L);

        verify(cartItemRepository, times(1)).delete(item);
    }

    @Test
    @DisplayName("IDOR Prevention: removeFromCart - User B cố xóa món của User A -> Bị chặn, món hàng KHÔNG bị xóa")
    void removeFromCart_WhenOtherUser_ShouldThrowBadRequestAndNotDelete() {
        User userA = User.builder().id(1L).build();
        CartItem item = CartItem.builder().id(10L).user(userA).build();

        when(cartItemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThrows(BadRequestException.class, () -> cartService.removeFromCart(2L, 10L));

        verify(cartItemRepository, never()).delete(any());
        verify(cartItemRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("CartItem không tồn tại -> Ném ResourceNotFoundException")
    void whenItemNotFound_ShouldThrowResourceNotFound() {
        when(cartItemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cartService.updateQuantity(1L, 999L, 5));
        assertThrows(ResourceNotFoundException.class, () -> cartService.updateSelection(1L, 999L, false));
        assertThrows(ResourceNotFoundException.class, () -> cartService.removeFromCart(1L, 999L));
    }
}
