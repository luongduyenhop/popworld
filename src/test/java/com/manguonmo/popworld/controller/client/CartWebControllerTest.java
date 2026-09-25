package com.manguonmo.popworld.controller.client;

import com.manguonmo.popworld.entity.CartItem;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.service.CartService;
import com.manguonmo.popworld.service.CategoryService;
import com.manguonmo.popworld.service.CharacterIpService;
import com.manguonmo.popworld.service.UserService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartWebControllerTest {

    @Mock
    private CartService cartService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private CharacterIpService characterIpService;

    @Mock
    private UserService userService;

    @Mock
    private Principal principal;

    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private CartWebController cartWebController;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder().id(1L).email("test@popworld.com").fullName("Test User").build();
    }

    @Test
    @DisplayName("viewCart: Hiển thị giỏ hàng và các thuộc tính liên quan")
    void viewCart_shouldRenderCartPage() {
        when(principal.getName()).thenReturn("test@popworld.com");
        when(userService.getUserByEmail("test@popworld.com")).thenReturn(sampleUser);
        CartItem item = CartItem.builder().id(10L).build();
        when(cartService.getCartItems(1L)).thenReturn(List.of(item));
        when(cartService.calculateSelectedTotal(1L)).thenReturn(new BigDecimal("300000"));
        when(cartService.getCartCount(1L)).thenReturn(1);

        String viewName = cartWebController.viewCart(model, principal);

        assertEquals("cart", viewName);
        verify(model, times(1)).addAttribute("cartItems", List.of(item));
        verify(model, times(1)).addAttribute("totalAmount", new BigDecimal("300000"));
        verify(model, times(1)).addAttribute("cartCount", 1);
        verify(model, times(1)).addAttribute("user", sampleUser);
    }

    @Test
    @DisplayName("addToCart: Thêm sản phẩm thành công và redirect về /cart kèm flash message")
    void addToCart_Success_RedirectsToCart() {
        when(principal.getName()).thenReturn("test@popworld.com");
        when(userService.getUserByEmail("test@popworld.com")).thenReturn(sampleUser);

        String viewName = cartWebController.addToCart(100L, "SINGLE_BOX", 2, redirectAttributes, principal);

        assertEquals("redirect:/cart", viewName);
        verify(cartService, times(1)).addToCart(1L, 100L, "SINGLE_BOX", 2);
        verify(redirectAttributes, times(1)).addFlashAttribute(eq("successMessage"), contains("thành công"));
    }

    @Test
    @DisplayName("addToCart: Khi có ngoại lệ thì redirect về /cart kèm errorMessage")
    void addToCart_Fail_RedirectsToCartWithErrorMessage() {
        when(principal.getName()).thenReturn("test@popworld.com");
        when(userService.getUserByEmail("test@popworld.com")).thenReturn(sampleUser);
        doThrow(new RuntimeException("Hết hàng tồn kho"))
                .when(cartService).addToCart(1L, 100L, "SINGLE_BOX", 2);

        String viewName = cartWebController.addToCart(100L, "SINGLE_BOX", 2, redirectAttributes, principal);

        assertEquals("redirect:/cart", viewName);
        verify(redirectAttributes, times(1)).addFlashAttribute(eq("errorMessage"), contains("Hết hàng tồn kho"));
    }

    @Test
    @DisplayName("updateQuantity: Cập nhật số lượng và redirect về /cart")
    void updateQuantity_shouldCallServiceAndRedirect() {
        when(principal.getName()).thenReturn("test@popworld.com");
        when(userService.getUserByEmail("test@popworld.com")).thenReturn(sampleUser);

        String viewName = cartWebController.updateQuantity(5L, 3, principal, redirectAttributes);

        assertEquals("redirect:/cart", viewName);
        verify(cartService, times(1)).updateQuantity(1L, 5L, 3);
    }

    @Test
    @DisplayName("toggleSelection: Đổi trạng thái chọn món và redirect về /cart")
    void toggleSelection_shouldCallServiceAndRedirect() {
        when(principal.getName()).thenReturn("test@popworld.com");
        when(userService.getUserByEmail("test@popworld.com")).thenReturn(sampleUser);

        String viewName = cartWebController.toggleSelection(5L, true, principal, redirectAttributes);

        assertEquals("redirect:/cart", viewName);
        verify(cartService, times(1)).updateSelection(1L, 5L, true);
    }

    @Test
    @DisplayName("deleteCartItem: Xóa sản phẩm khỏi giỏ và redirect về /cart")
    void deleteCartItem_shouldCallServiceAndRedirect() {
        when(principal.getName()).thenReturn("test@popworld.com");
        when(userService.getUserByEmail("test@popworld.com")).thenReturn(sampleUser);

        String viewName = cartWebController.deleteCartItem(15L, principal, redirectAttributes);

        assertEquals("redirect:/cart", viewName);
        verify(cartService, times(1)).removeFromCart(1L, 15L);
        verify(redirectAttributes, times(1)).addFlashAttribute(eq("successMessage"), contains("Đã xóa"));
    }

    @Test
    @DisplayName("viewCart: Khi không có sản phẩm nào được chọn (0 món) thì totalAmount là 0")
    void viewCart_whenNoItemsSelected_totalAmountIsZero() {
        when(principal.getName()).thenReturn("test@popworld.com");
        when(userService.getUserByEmail("test@popworld.com")).thenReturn(sampleUser);
        CartItem item1 = CartItem.builder().id(1L).isSelected(false).build();
        CartItem item2 = CartItem.builder().id(2L).isSelected(false).build();
        CartItem item3 = CartItem.builder().id(3L).isSelected(false).build();
        List<CartItem> cartList = List.of(item1, item2, item3);

        when(cartService.getCartItems(1L)).thenReturn(cartList);
        when(cartService.calculateSelectedTotal(1L)).thenReturn(BigDecimal.ZERO);
        when(cartService.getCartCount(1L)).thenReturn(3);

        String viewName = cartWebController.viewCart(model, principal);

        assertEquals("cart", viewName);
        verify(model, times(1)).addAttribute("cartItems", cartList);
        verify(model, times(1)).addAttribute("totalAmount", BigDecimal.ZERO);
        verify(model, times(1)).addAttribute("cartCount", 3);
    }

    @Test
    @DisplayName("viewCart: Khi có 1 trong 3 sản phẩm được chọn thì totalAmount đúng bằng giá sản phẩm đó")
    void viewCart_whenOneItemSelected_totalAmountMatchesSelected() {
        when(principal.getName()).thenReturn("test@popworld.com");
        when(userService.getUserByEmail("test@popworld.com")).thenReturn(sampleUser);
        CartItem item1 = CartItem.builder().id(1L).isSelected(true).build();
        CartItem item2 = CartItem.builder().id(2L).isSelected(false).build();
        CartItem item3 = CartItem.builder().id(3L).isSelected(false).build();
        List<CartItem> cartList = List.of(item1, item2, item3);
        BigDecimal expectedTotal = new BigDecimal("200000");

        when(cartService.getCartItems(1L)).thenReturn(cartList);
        when(cartService.calculateSelectedTotal(1L)).thenReturn(expectedTotal);
        when(cartService.getCartCount(1L)).thenReturn(3);

        String viewName = cartWebController.viewCart(model, principal);

        assertEquals("cart", viewName);
        verify(model, times(1)).addAttribute("cartItems", cartList);
        verify(model, times(1)).addAttribute("totalAmount", expectedTotal);
        verify(model, times(1)).addAttribute("cartCount", 3);
    }
}

