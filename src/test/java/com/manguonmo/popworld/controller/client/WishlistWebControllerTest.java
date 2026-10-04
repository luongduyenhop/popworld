package com.manguonmo.popworld.controller.client;

import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.service.UserService;
import com.manguonmo.popworld.service.WishlistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishlistWebControllerTest {

    @Mock
    private WishlistService wishlistService;

    @Mock
    private UserService userService;

    @Mock
    private Principal principal;

    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private WishlistWebController wishlistWebController;

    private User sampleUser;
    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setEmail("test@popworld.com");
        sampleUser.setEnabled(true);

        sampleProduct = new Product();
        sampleProduct.setId(10L);
        sampleProduct.setName("Skullpanda City of Night");
        sampleProduct.setSinglePrice(new BigDecimal("350000"));
    }

    @Test
    @DisplayName("showWishlistPage - Khi chưa đăng nhập -> Chuyển hướng login kèm continue url")
    void showWishlistPage_unauthenticated() {
        String view = wishlistWebController.showWishlistPage(model, null);
        assertEquals("redirect:/login?continue=/wishlist", view);
        verifyNoInteractions(wishlistService);
    }

    @Test
    @DisplayName("showWishlistPage - Khi đã đăng nhập -> Nạp danh sách yêu thích và trả về view wishlist")
    void showWishlistPage_authenticated() {
        when(principal.getName()).thenReturn("test@popworld.com");
        when(userService.getUserByEmail("test@popworld.com")).thenReturn(sampleUser);
        when(wishlistService.getWishlistProducts(1L)).thenReturn(List.of(sampleProduct));

        String view = wishlistWebController.showWishlistPage(model, principal);

        assertEquals("wishlist", view);
        verify(model).addAttribute(eq("products"), anyList());
        verify(model).addAttribute(eq("wishlistCount"), eq(1));
        verify(model).addAttribute(eq("pageTitle"), anyString());
    }

    @Test
    @DisplayName("removeFromWishlist - Khi chưa đăng nhập -> Chuyển hướng login")
    void removeFromWishlist_unauthenticated() {
        String view = wishlistWebController.removeFromWishlist(10L, redirectAttributes, null);
        assertEquals("redirect:/login", view);
        verifyNoInteractions(wishlistService);
    }

    @Test
    @DisplayName("removeFromWishlist - Khi đã đăng nhập -> Gọi service xóa và chuyển hướng lại /wishlist")
    void removeFromWishlist_authenticated() {
        when(principal.getName()).thenReturn("test@popworld.com");
        when(userService.getUserByEmail("test@popworld.com")).thenReturn(sampleUser);

        String view = wishlistWebController.removeFromWishlist(10L, redirectAttributes, principal);

        assertEquals("redirect:/wishlist", view);
        verify(wishlistService).removeFromWishlist(1L, 10L);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
    }
}
