package com.manguonmo.popworld.controller.api;

import com.manguonmo.popworld.dto.response.ApiResponse;
import com.manguonmo.popworld.dto.response.CartItemResponse;
import com.manguonmo.popworld.entity.CartItem;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.mapper.CartMapper;
import com.manguonmo.popworld.service.CartService;
import com.manguonmo.popworld.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CartApiControllerTest {

    @Mock
    private CartService cartService;

    @Mock
    private CartMapper cartMapper;

    @Mock
    private UserService userService;

    @Mock
    private Principal principal;

    @InjectMocks
    private CartApiController cartApiController;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder().id(1L).email("user@popworld.com").enabled(true).build();
        lenient().when(principal.getName()).thenReturn("user@popworld.com");
        lenient().when(userService.getUserByEmail("user@popworld.com")).thenReturn(sampleUser);
    }

    @Test
    @DisplayName("getCartItems trả về danh sách giỏ hàng của user")
    public void getCartItems_shouldReturnList() {
        CartItem item = CartItem.builder().id(10L).build();
        CartItemResponse itemRes = CartItemResponse.builder().id(10L).build();

        when(cartService.getCartItems(1L)).thenReturn(List.of(item));
        when(cartMapper.toResponseList(List.of(item))).thenReturn(List.of(itemRes));

        ResponseEntity<ApiResponse<List<CartItemResponse>>> response = cartApiController.getCartItems(principal);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
        assertEquals(10L, response.getBody().getData().get(0).getId());
    }

    @Test
    @DisplayName("getCartSummary trả về tóm tắt giỏ hàng gồm số lượng, tổng tiền và items")
    public void getCartSummary_shouldReturnSummaryMap() {
        when(cartService.getCartItems(1L)).thenReturn(List.of());
        when(cartMapper.toResponseList(List.of())).thenReturn(List.of());
        when(cartService.calculateSelectedTotal(1L)).thenReturn(new BigDecimal("500000"));
        when(cartService.getCartCount(1L)).thenReturn(2);

        ResponseEntity<ApiResponse<Map<String, Object>>> response = cartApiController.getCartSummary(principal);

        assertNotNull(response);
        Map<String, Object> data = response.getBody().getData();
        assertEquals(2, data.get("cartCount"));
        assertEquals(new BigDecimal("500000"), data.get("totalAmount"));
        assertNotNull(data.get("items"));
    }

    @Test
    @DisplayName("addToCart thêm sản phẩm thành công và trả về DTO")
    public void addToCart_shouldAddItemSuccessfully() {
        CartItem item = CartItem.builder().id(5L).build();
        CartItemResponse itemRes = CartItemResponse.builder().id(5L).build();

        when(cartService.addToCart(1L, 100L, "SINGLE_BOX", 2)).thenReturn(item);
        when(cartMapper.toResponse(item)).thenReturn(itemRes);

        ResponseEntity<ApiResponse<CartItemResponse>> response = cartApiController.addToCart(100L, "SINGLE_BOX", 2, principal);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(5L, response.getBody().getData().getId());
    }

    @Test
    @DisplayName("updateQuantity cập nhật số lượng và truyền đúng userId")
    public void updateQuantity_shouldCallServiceWithUserId() {
        ResponseEntity<ApiResponse<Void>> response = cartApiController.updateQuantity(10L, 5, principal);

        verify(cartService, times(1)).updateQuantity(1L, 10L, 5);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    @DisplayName("updateSelection cập nhật chọn món và truyền đúng userId")
    public void updateSelection_shouldCallServiceWithUserId() {
        ResponseEntity<ApiResponse<Void>> response = cartApiController.updateSelection(10L, false, principal);

        verify(cartService, times(1)).updateSelection(1L, 10L, false);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    @DisplayName("removeItem xóa sản phẩm và gọi service đúng userId")
    public void removeItem_shouldCallServiceWithUserId() {
        ResponseEntity<ApiResponse<Void>> response = cartApiController.removeItem(99L, principal);

        verify(cartService, times(1)).removeFromCart(1L, 99L);
        assertEquals(200, response.getStatusCode().value());
    }
}
