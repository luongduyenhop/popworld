package com.manguonmo.popworld.controller.api;

import com.manguonmo.popworld.dto.request.WishlistToggleRequest;
import com.manguonmo.popworld.dto.response.ApiResponse;
import com.manguonmo.popworld.dto.response.WishlistToggleResponse;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.service.UserService;
import com.manguonmo.popworld.service.WishlistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishlistApiControllerTest {

    @Mock
    private WishlistService wishlistService;

    @Mock
    private UserService userService;

    @Mock
    private Principal principal;

    @InjectMocks
    private WishlistApiController controller;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder().id(1L).email("user@test.com").enabled(true).build();
    }

    @Test
    @DisplayName("toggleWishlist: Chưa đăng nhập -> Ném BadRequestException")
    void toggleWishlist_Unauthenticated_ThrowsBadRequest() {
        WishlistToggleRequest request = WishlistToggleRequest.builder().productId(10L).build();

        assertThrows(BadRequestException.class, () -> controller.toggleWishlist(request, null));
    }

    @Test
    @DisplayName("toggleWishlist: Đăng nhập hợp lệ -> Toggle thành công và trả về WishlistToggleResponse")
    void toggleWishlist_Success() {
        when(principal.getName()).thenReturn("user@test.com");
        when(userService.getUserByEmail("user@test.com")).thenReturn(sampleUser);
        when(wishlistService.toggleWishlist(1L, 10L)).thenReturn(true);
        when(wishlistService.getWishlistCount(1L)).thenReturn(1L);

        WishlistToggleRequest request = WishlistToggleRequest.builder().productId(10L).build();
        ResponseEntity<ApiResponse<WishlistToggleResponse>> response = controller.toggleWishlist(request, principal);

        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        WishlistToggleResponse data = response.getBody().getData();
        assertTrue(data.isWishlisted());
        assertEquals(1L, data.getWishlistCount());
        assertEquals(10L, data.getProductId());
    }

    @Test
    @DisplayName("getWishlistCount: Trả về số lượng sản phẩm yêu thích của user")
    void getWishlistCount_Success() {
        when(principal.getName()).thenReturn("user@test.com");
        when(userService.getUserByEmail("user@test.com")).thenReturn(sampleUser);
        when(wishlistService.getWishlistCount(1L)).thenReturn(3L);

        ResponseEntity<ApiResponse<Long>> response = controller.getWishlistCount(principal);

        assertNotNull(response.getBody());
        assertEquals(3L, response.getBody().getData());
    }

    @Test
    @DisplayName("isWishlisted: Kiểm tra trạng thái yêu thích của sản phẩm")
    void isWishlisted_Success() {
        when(principal.getName()).thenReturn("user@test.com");
        when(userService.getUserByEmail("user@test.com")).thenReturn(sampleUser);
        when(wishlistService.isWishlisted(1L, 10L)).thenReturn(true);

        ResponseEntity<ApiResponse<Boolean>> response = controller.isWishlisted(10L, principal);

        assertNotNull(response.getBody());
        assertTrue(response.getBody().getData());
    }
}
