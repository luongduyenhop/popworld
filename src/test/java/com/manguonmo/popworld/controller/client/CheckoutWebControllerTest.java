package com.manguonmo.popworld.controller.client;

import com.manguonmo.popworld.entity.*;
import com.manguonmo.popworld.service.*;
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
    private UserService userService;

    @Mock
    private com.manguonmo.popworld.repository.CouponRepository couponRepository;

    @Mock
    private UserAddressService userAddressService;

    @Mock
    private Principal principal;


    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private CheckoutWebController checkoutWebController;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder().id(1L).email("test@popworld.com").fullName("Test User").enabled(true).build();
        lenient().when(principal.getName()).thenReturn("test@popworld.com");
        lenient().when(userService.getUserByEmail("test@popworld.com")).thenReturn(sampleUser);
    }

    @Test
    @DisplayName("showCheckoutPage: Chưa đăng nhập thì redirect về /login")
    void showCheckoutPage_Unauthenticated_RedirectsToLogin() {
        String viewName = checkoutWebController.showCheckoutPage(model, redirectAttributes, null);
        assertEquals("redirect:/login", viewName);
    }

    @Test
    @DisplayName("showCheckoutPage: Tài khoản bị vô hiệu hóa thì redirect về /login kèm lỗi")
    void showCheckoutPage_DisabledUser_RedirectsToLogin() {
        User disabledUser = User.builder().id(2L).email("disabled@popworld.com").enabled(false).build();
        when(principal.getName()).thenReturn("disabled@popworld.com");
        when(userService.getUserByEmail("disabled@popworld.com")).thenReturn(disabledUser);

        String viewName = checkoutWebController.showCheckoutPage(model, redirectAttributes, principal);

        assertEquals("redirect:/login", viewName);
        verify(redirectAttributes, times(1)).addFlashAttribute(eq("errorMessage"), anyString());
    }

    @Test
    @DisplayName("showCheckoutPage: Giỏ hàng trống thì redirect về /cart")
    void showCheckoutPage_EmptyCart_RedirectsToCart() {
        when(cartService.getCartItems(1L)).thenReturn(Collections.emptyList());

        String viewName = checkoutWebController.showCheckoutPage(model, redirectAttributes, principal);

        assertEquals("redirect:/cart", viewName);
        verify(redirectAttributes, times(1)).addFlashAttribute(eq("errorMessage"), anyString());
    }

    @Test
    @DisplayName("showCheckoutPage: Giỏ hàng không có món nào được chọn (isSelected=false) -> Redirect về /cart kèm errorMessage")
    void showCheckoutPage_NoSelectedItems_RedirectsToCart() {
        Product product = Product.builder().id(10L).singlePrice(new BigDecimal("200000")).build();
        CartItem item = CartItem.builder().id(1L).product(product).purchaseType("SINGLE_BOX").quantity(1).isSelected(false).build();

        when(cartService.getCartItems(1L)).thenReturn(List.of(item));

        String viewName = checkoutWebController.showCheckoutPage(model, redirectAttributes, principal);

        assertEquals("redirect:/cart", viewName);
        verify(redirectAttributes, times(1)).addFlashAttribute(eq("errorMessage"), contains("Vui lòng chọn ít nhất một sản phẩm"));
    }

    @Test
    @DisplayName("showCheckoutPage: Giỏ hàng có sản phẩm được chọn -> render trang checkout với chỉ các món được chọn")
    void showCheckoutPage_HasItems_RendersCheckout() {
        Product product = Product.builder().id(10L).singlePrice(new BigDecimal("200000")).build();
        CartItem selectedItem = CartItem.builder().id(1L).product(product).purchaseType("SINGLE_BOX").quantity(1).isSelected(true).build();
        CartItem unselectedItem = CartItem.builder().id(2L).product(product).purchaseType("SINGLE_BOX").quantity(1).isSelected(false).build();

        when(cartService.getCartItems(1L)).thenReturn(List.of(selectedItem, unselectedItem));
        when(cartService.calculateSelectedTotal(1L)).thenReturn(new BigDecimal("200000"));

        String viewName = checkoutWebController.showCheckoutPage(model, redirectAttributes, principal);

        assertEquals("checkout", viewName);
        verify(model, times(1)).addAttribute("user", sampleUser);
        verify(model, times(1)).addAttribute("cartItems", List.of(selectedItem));
    }

    @Test
    @DisplayName("placeOrder thành công với COD: Redirect sang trang success")
    void placeOrder_Success_COD_RedirectsToSuccess() {
        Order mockOrder = Order.builder().id(100L).orderCode("PW-12345").paymentMethod("COD").build();

        when(orderService.createOrder(eq(1L), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), eq("COD"), any()))
                .thenReturn(mockOrder);

        String viewName = checkoutWebController.placeOrder(
                "Nguyen Van A", "0987654321", "Hà Nội", "Cầu Giấy",
                "Dịch Vọng", "123 Cầu Giấy", "COD", null, redirectAttributes, principal
        );

        assertEquals("redirect:/checkout/success/PW-12345", viewName);
    }

    @Test
    @DisplayName("placeOrder thành công với SEPAY: Redirect sang trang thanh toán QR")
    void placeOrder_Success_SEPAY_RedirectsToPayment() {
        Order mockOrder = Order.builder().id(101L).orderCode("PW-99999").paymentMethod("SEPAY").build();

        when(orderService.createOrder(eq(1L), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), eq("SEPAY"), any()))
                .thenReturn(mockOrder);

        String viewName = checkoutWebController.placeOrder(
                "Nguyen Van A", "0987654321", "Hà Nội", "Cầu Giấy",
                "Dịch Vọng", "123 Cầu Giấy", "SEPAY", "POP10", redirectAttributes, principal
        );

        assertEquals("redirect:/checkout/payment/PW-99999", viewName);
    }

    @Test
    @DisplayName("placeOrder thất bại khi thông tin người nhận bị rỗng: Redirect về /checkout kèm errorMessage")
    void placeOrder_BlankRecipientInfo_RedirectsToCheckoutWithErrorMessage() {
        String viewName = checkoutWebController.placeOrder(
                "   ", "0987654321", "Hà Nội", "Cầu Giấy",
                "Dịch Vọng", "123 Cầu Giấy", "COD", null, redirectAttributes, principal
        );

        assertEquals("redirect:/checkout", viewName);
        verify(redirectAttributes, times(1)).addFlashAttribute(eq("errorMessage"), contains("Vui lòng điền đầy đủ"));
        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("showPaymentQrPage: Người dùng khác xem đơn không phải của mình -> Chuyển hướng 403")
    void showPaymentQrPage_OtherUserOrder_RedirectsTo403() {
        User otherOwner = User.builder().id(999L).build();
        Order order = Order.builder().id(10L).orderCode("PW-999").user(otherOwner).totalAmount(new BigDecimal("100000")).build();
        when(orderService.getOrderByCode("PW-999")).thenReturn(order);

        String viewName = checkoutWebController.showPaymentQrPage("PW-999", model, principal);

        assertEquals("redirect:/403", viewName);
    }

    @Test
    @DisplayName("showPaymentQrPage: Chưa đăng nhập -> Chuyển hướng /login")
    void showPaymentQrPage_Unauthenticated_RedirectsToLogin() {
        Order order = Order.builder().id(10L).orderCode("PW-999").build();
        when(orderService.getOrderByCode("PW-999")).thenReturn(order);

        String viewName = checkoutWebController.showPaymentQrPage("PW-999", model, null);

        assertEquals("redirect:/login", viewName);
    }

    @Test
    @DisplayName("showOrderSuccessPage: Người dùng khác xem đơn không phải của mình -> Chuyển hướng 403")
    void showOrderSuccessPage_OtherUserOrder_RedirectsTo403() {
        User otherOwner = User.builder().id(999L).build();
        Order order = Order.builder().id(10L).orderCode("PW-999").user(otherOwner).build();
        when(orderService.getOrderByCode("PW-999")).thenReturn(order);

        String viewName = checkoutWebController.showOrderSuccessPage("PW-999", model, principal);

        assertEquals("redirect:/403", viewName);
    }

    @Test
    @DisplayName("showOrderSuccessPage: Chưa đăng nhập -> Chuyển hướng /login")
    void showOrderSuccessPage_Unauthenticated_RedirectsToLogin() {
        Order order = Order.builder().id(10L).orderCode("PW-999").build();
        when(orderService.getOrderByCode("PW-999")).thenReturn(order);

        String viewName = checkoutWebController.showOrderSuccessPage("PW-999", model, null);

        assertEquals("redirect:/login", viewName);
    }

    @Test
    @DisplayName("cancelOrder thành công: Gọi orderService.cancelOrder và redirect về /orders kèm successMessage")
    void cancelOrder_Success_RedirectsToOrdersWithSuccessMessage() {
        String viewName = checkoutWebController.cancelOrder("PW-12345", "Đổi ý", redirectAttributes, principal);

        assertEquals("redirect:/orders", viewName);
        verify(orderService, times(1)).cancelOrder(1L, "PW-12345", "Đổi ý");
        verify(redirectAttributes, times(1)).addFlashAttribute(eq("successMessage"), contains("thành công"));
    }

    @Test
    @DisplayName("cancelOrder thất bại khi có lỗi: Redirect về /orders kèm errorMessage")
    void cancelOrder_Fail_RedirectsToOrdersWithErrorMessage() {
        doThrow(new RuntimeException("Đơn hàng không thể hủy"))
                .when(orderService).cancelOrder(1L, "PW-12345", "Lý do");

        String viewName = checkoutWebController.cancelOrder("PW-12345", "Lý do", redirectAttributes, principal);

        assertEquals("redirect:/orders", viewName);
        verify(redirectAttributes, times(1)).addFlashAttribute(eq("errorMessage"), contains("Đơn hàng không thể hủy"));
    }
}
