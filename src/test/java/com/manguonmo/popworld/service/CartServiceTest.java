package com.manguonmo.popworld.service;

import com.manguonmo.popworld.service.impl.CartServiceImpl;
import com.manguonmo.popworld.entity.CartItem;
import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.repository.CartItemRepository;
import com.manguonmo.popworld.repository.ProductRepository;
import com.manguonmo.popworld.repository.UserRepository;
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
        Product product = Product.builder().id(10L).name("Labubu").singlePrice(new BigDecimal("350000")).build();

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
}
