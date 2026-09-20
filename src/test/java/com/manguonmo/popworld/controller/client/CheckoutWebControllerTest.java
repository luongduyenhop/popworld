package com.manguonmo.popworld.controller.client;

import com.manguonmo.popworld.entity.*;
import com.manguonmo.popworld.service.CartService;
import com.manguonmo.popworld.service.CategoryService;
import com.manguonmo.popworld.service.CharacterIpService;
import com.manguonmo.popworld.service.OrderService;
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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckoutWebControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private CartService cartService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private CharacterIpService characterIpService;

    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private CheckoutWebController checkoutWebController;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder().id(1L).fullName("Test User").build();
    }

    @Test
    @DisplayName("showCheckoutPage: Giỏ hàng trống thì redirect về /cart")
    void showCheckoutPage_EmptyCart_RedirectsToCart() {
        when(cartService.getDefaultUser()).thenReturn(sampleUser);
        when(cartService.getCartItems(1L)).thenReturn(Collections.emptyList());

        String viewName = checkoutWebController.showCheckoutPage(model, redirectAttributes);

        assertEquals("redirect:/cart", viewName);
        verify(redirectAttributes, times(1)).addFlashAttribute(eq("errorMessage"), anyString());
    }

    @Test
    @DisplayName("showCheckoutPage: Giỏ hàng có sản phẩm thì render trang checkout")
    void showCheckoutPage_HasItems_RendersCheckout() {
        Product product = Product.builder().id(10L).singlePrice(new BigDecimal("200000")).build();
        CartItem item = CartItem.builder().id(1L).product(product).purchaseType("SINGLE_BOX").quantity(1).build();

        when(cartService.getDefaultUser()).thenReturn(sampleUser);
        when(cartService.getCartItems(1L)).thenReturn(List.of(item));
        when(cartService.calculateSelectedTotal(1L)).thenReturn(new BigDecimal("200000"));

        String viewName = checkoutWebController.showCheckoutPage(model, redirectAttributes);

        assertEquals("checkout", viewName);
        verify(model, times(1)).addAttribute("user", sampleUser);
        verify(model, times(1)).addAttribute("cartItems", List.of(item));
    }

    @Test
    @DisplayName("placeOrder thành công với COD: Redirect sang trang success")
    void placeOrder_Success_COD_RedirectsToSuccess() {
        Order mockOrder = Order.builder().id(100L).orderCode("PW-12345").paymentMethod("COD").build();

        when(cartService.getDefaultUser()).thenReturn(sampleUser);
        when(orderService.createOrder(eq(1L), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), eq("COD"), any()))
                .thenReturn(mockOrder);

        String viewName = checkoutWebController.placeOrder(
                "Nguyen Van A", "0987654321", "Hà Nội", "Cầu Giấy",
                "Dịch Vọng", "123 Cầu Giấy", "COD", null, redirectAttributes
        );

        assertEquals("redirect:/checkout/success/PW-12345", viewName);
    }

    @Test
    @DisplayName("placeOrder thành công với SEPAY: Redirect sang trang thanh toán QR")
    void placeOrder_Success_SEPAY_RedirectsToPayment() {
        Order mockOrder = Order.builder().id(101L).orderCode("PW-99999").paymentMethod("SEPAY").build();

        when(cartService.getDefaultUser()).thenReturn(sampleUser);
        when(orderService.createOrder(eq(1L), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), eq("SEPAY"), any()))
                .thenReturn(mockOrder);

        String viewName = checkoutWebController.placeOrder(
                "Nguyen Van A", "0987654321", "Hà Nội", "Cầu Giấy",
                "Dịch Vọng", "123 Cầu Giấy", "SEPAY", "POP10", redirectAttributes
        );

        assertEquals("redirect:/checkout/payment/PW-99999", viewName);
    }

    @Test
    @DisplayName("cancelOrder thành công: Gọi orderService.cancelOrder và redirect về /orders kèm successMessage")
    void cancelOrder_Success_RedirectsToOrdersWithSuccessMessage() {
        when(cartService.getDefaultUser()).thenReturn(sampleUser);

        String viewName = checkoutWebController.cancelOrder("PW-12345", "Đổi ý", redirectAttributes);

        assertEquals("redirect:/orders", viewName);
        verify(orderService, times(1)).cancelOrder(1L, "PW-12345", "Đổi ý");
        verify(redirectAttributes, times(1)).addFlashAttribute(eq("successMessage"), contains("thành công"));
    }

    @Test
    @DisplayName("cancelOrder thất bại khi có lỗi: Redirect về /orders kèm errorMessage")
    void cancelOrder_Fail_RedirectsToOrdersWithErrorMessage() {
        when(cartService.getDefaultUser()).thenReturn(sampleUser);
        doThrow(new RuntimeException("Đơn hàng không thể hủy"))
                .when(orderService).cancelOrder(1L, "PW-12345", "Lý do");

        String viewName = checkoutWebController.cancelOrder("PW-12345", "Lý do", redirectAttributes);

        assertEquals("redirect:/orders", viewName);
        verify(redirectAttributes, times(1)).addFlashAttribute(eq("errorMessage"), contains("Đơn hàng không thể hủy"));
    }

    @Test
    @DisplayName("showMyOrders: Render trang my-orders với danh sách đơn hàng của user")
    void showMyOrders_RendersMyOrdersView() {
        when(cartService.getDefaultUser()).thenReturn(sampleUser);
        Order order = Order.builder().id(10L).orderCode("PW-001").build();
        when(orderService.getOrdersByUser(1L)).thenReturn(List.of(order));

        String viewName = checkoutWebController.showMyOrders(model);

        assertEquals("my-orders", viewName);
        verify(model, times(1)).addAttribute("orders", List.of(order));
        verify(model, times(1)).addAttribute("user", sampleUser);
    }
}
