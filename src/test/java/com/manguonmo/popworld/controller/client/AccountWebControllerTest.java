package com.manguonmo.popworld.controller.client;

import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.OrderItem;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.repository.CouponRepository;
import com.manguonmo.popworld.repository.UserAddressRepository;
import com.manguonmo.popworld.service.CategoryService;
import com.manguonmo.popworld.service.CharacterIpService;
import com.manguonmo.popworld.service.OrderService;
import com.manguonmo.popworld.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountWebControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private OrderService orderService;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserAddressRepository userAddressRepository;

    @Mock
    private com.manguonmo.popworld.service.UserAddressService userAddressService;

    @Mock
    private CategoryService categoryService;


    @Mock
    private CharacterIpService characterIpService;

    @Mock
    private Model model;

    @Mock
    private Principal principal;

    @InjectMocks
    private AccountWebController accountWebController;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .email("test@popworld.com")
                .fullName("Test User")
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("showAccountHub: Chuyển hướng đến /login khi chưa đăng nhập")
    void showAccountHub_WhenPrincipalNull_ShouldRedirectToLogin() {
        String view = accountWebController.showAccountHub(model, null);
        assertEquals("redirect:/login", view);
    }

    @Test
    @DisplayName("showAccountHub: Render my-orders khi đã đăng nhập thành công")
    void showAccountHub_WhenAuthenticated_ShouldRenderMyOrders() {
        when(principal.getName()).thenReturn("test@popworld.com");
        when(userService.getUserByEmail("test@popworld.com")).thenReturn(sampleUser);
        when(orderService.getOrdersByUser(1L)).thenReturn(Collections.emptyList());
        when(couponRepository.countByActiveTrue()).thenReturn(3L);
        when(userAddressRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

        String view = accountWebController.showAccountHub(model, principal);

        assertEquals("my-orders", view);
        verify(model).addAttribute("user", sampleUser);
        verify(model).addAttribute("couponsCount", 3L);
        verify(model).addAttribute("addresses", Collections.emptyList());
    }

    @Test
    @DisplayName("profileRedirect: Chuyển hướng 302 về /account")
    void profileRedirect_ShouldRedirectToAccount() {
        String view = accountWebController.profileRedirect();
        assertEquals("redirect:/account", view);
    }

    @Test
    @DisplayName("showOrderDetailPage: Render order-success khi đơn hàng tồn tại")
    void showOrderDetailPage_WhenOrderExists_ShouldRenderOrderSuccess() {
        Order order = Order.builder()
                .id(100L)
                .orderCode("PW-123456")
                .user(sampleUser)
                .build();

        when(orderService.getOrderByCode("PW-123456")).thenReturn(order);
        when(principal.getName()).thenReturn("test@popworld.com");
        when(userService.getUserByEmail("test@popworld.com")).thenReturn(sampleUser);
        when(orderService.getOrderItems(100L)).thenReturn(Collections.emptyList());

        String view = accountWebController.showOrderDetailPage("PW-123456", model, principal);

        assertEquals("order-success", view);
        verify(model).addAttribute("order", order);
    }

    @Test
    @DisplayName("showOrderDetailPage: Chuyển hướng về /login khi chưa đăng nhập")
    void showOrderDetailPage_WhenPrincipalNull_ShouldRedirectToLogin() {
        Order order = Order.builder().id(100L).orderCode("PW-123456").build();
        when(orderService.getOrderByCode("PW-123456")).thenReturn(order);

        String view = accountWebController.showOrderDetailPage("PW-123456", model, null);

        assertEquals("redirect:/login", view);
    }

    @Test
    @DisplayName("showOrderDetailPage: Chuyển hướng 403 khi xem đơn của người khác")
    void showOrderDetailPage_WhenOtherUser_ShouldRedirectTo403() {
        User otherOwner = User.builder().id(999L).role("ROLE_USER").build();
        Order order = Order.builder().id(100L).orderCode("PW-123456").user(otherOwner).build();

        when(orderService.getOrderByCode("PW-123456")).thenReturn(order);
        when(principal.getName()).thenReturn("test@popworld.com");
        when(userService.getUserByEmail("test@popworld.com")).thenReturn(sampleUser);

        String view = accountWebController.showOrderDetailPage("PW-123456", model, principal);

        assertEquals("redirect:/403", view);
    }

    @Test
    @DisplayName("showOrderDetailPage: Admin được phép xem đơn của người dùng khác")
    void showOrderDetailPage_WhenAdminUser_ShouldRenderOrderSuccess() {
        User adminUser = User.builder().id(99L).email("admin@popworld.com").role("ROLE_ADMIN").build();
        User otherOwner = User.builder().id(999L).role("ROLE_USER").build();
        Order order = Order.builder().id(100L).orderCode("PW-123456").user(otherOwner).build();
        Principal adminPrincipal = () -> "admin@popworld.com";

        when(orderService.getOrderByCode("PW-123456")).thenReturn(order);
        when(userService.getUserByEmail("admin@popworld.com")).thenReturn(adminUser);
        when(orderService.getOrderItems(100L)).thenReturn(Collections.emptyList());

        String view = accountWebController.showOrderDetailPage("PW-123456", model, adminPrincipal);

        assertEquals("order-success", view);
        verify(model).addAttribute("order", order);
    }

    @Test
    @DisplayName("updateProfile: Thành công redirect về /account kèm flash message")
    void updateProfile_Success_RedirectsToAccount() {
        when(principal.getName()).thenReturn("test@popworld.com");
        when(userService.getUserByEmail("test@popworld.com")).thenReturn(sampleUser);

        org.springframework.validation.BindingResult br = mock(org.springframework.validation.BindingResult.class);
        when(br.hasErrors()).thenReturn(false);
        org.springframework.web.servlet.mvc.support.RedirectAttributes ra = mock(org.springframework.web.servlet.mvc.support.RedirectAttributes.class);

        com.manguonmo.popworld.dto.request.ProfileUpdateRequest req = new com.manguonmo.popworld.dto.request.ProfileUpdateRequest("Nguyen Van A", "0912345678");

        String view = accountWebController.updateProfile(req, br, ra, principal);

        assertEquals("redirect:/account", view);
        verify(userService).updateProfile(1L, req);
        verify(ra).addFlashAttribute(eq("successMessage"), anyString());
    }

    @Test
    @DisplayName("changePassword: Thành công redirect về /account kèm flash message")
    void changePassword_Success_RedirectsToAccount() {
        when(principal.getName()).thenReturn("test@popworld.com");
        when(userService.getUserByEmail("test@popworld.com")).thenReturn(sampleUser);

        org.springframework.validation.BindingResult br = mock(org.springframework.validation.BindingResult.class);
        when(br.hasErrors()).thenReturn(false);
        org.springframework.web.servlet.mvc.support.RedirectAttributes ra = mock(org.springframework.web.servlet.mvc.support.RedirectAttributes.class);

        com.manguonmo.popworld.dto.request.ChangePasswordRequest req = new com.manguonmo.popworld.dto.request.ChangePasswordRequest("old", "new123", "new123");

        String view = accountWebController.changePassword(req, br, ra, principal);

        assertEquals("redirect:/account", view);
        verify(userService).changePassword(1L, req);
        verify(ra).addFlashAttribute(eq("successMessage"), anyString());
    }

    @Test
    @DisplayName("addAddress: Thành công redirect về /account kèm flash message")
    void addAddress_Success_RedirectsToAccount() {
        when(principal.getName()).thenReturn("test@popworld.com");
        when(userService.getUserByEmail("test@popworld.com")).thenReturn(sampleUser);

        org.springframework.validation.BindingResult br = mock(org.springframework.validation.BindingResult.class);
        when(br.hasErrors()).thenReturn(false);
        org.springframework.web.servlet.mvc.support.RedirectAttributes ra = mock(org.springframework.web.servlet.mvc.support.RedirectAttributes.class);

        com.manguonmo.popworld.dto.request.AddressRequest req = com.manguonmo.popworld.dto.request.AddressRequest.builder()
                .recipientName("A").recipientPhone("0912").provinceCity("HN").district("CG").detailedAddress("123").build();

        String view = accountWebController.addAddress(req, br, ra, principal);

        assertEquals("redirect:/account", view);
        verify(userAddressService).createAddress(1L, req);
        verify(ra).addFlashAttribute(eq("successMessage"), anyString());
    }

    @Test
    @DisplayName("deleteAddress: Thành công redirect về /account")
    void deleteAddress_Success_RedirectsToAccount() {
        when(principal.getName()).thenReturn("test@popworld.com");
        when(userService.getUserByEmail("test@popworld.com")).thenReturn(sampleUser);
        org.springframework.web.servlet.mvc.support.RedirectAttributes ra = mock(org.springframework.web.servlet.mvc.support.RedirectAttributes.class);

        String view = accountWebController.deleteAddress(10L, ra, principal);

        assertEquals("redirect:/account", view);
        verify(userAddressService).deleteAddress(1L, 10L);
    }
}

