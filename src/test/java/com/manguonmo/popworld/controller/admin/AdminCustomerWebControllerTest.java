package com.manguonmo.popworld.controller.admin;

import com.manguonmo.popworld.dto.response.CustomerStatsResponse;
import com.manguonmo.popworld.dto.response.PointTransactionResponse;
import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.entity.UserAddress;
import com.manguonmo.popworld.service.OrderService;
import com.manguonmo.popworld.service.UserAddressService;
import com.manguonmo.popworld.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminCustomerWebControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UserAddressService userAddressService;

    @Mock
    private OrderService orderService;

    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirectAttributes;

    @Mock
    private Principal principal;

    @InjectMocks
    private AdminCustomerWebController controller;

    @Test
    @DisplayName("listCustomers: Trả về trang admin/customers và đẩy đủ model attributes")
    void listCustomers_Success() {
        User u1 = User.builder().id(1L).fullName("Customer 1").build();
        CustomerStatsResponse stats = CustomerStatsResponse.builder()
                .totalCustomers(10L)
                .vipCount(3L)
                .memberCount(7L)
                .build();

        when(userService.getAllCustomers()).thenReturn(List.of(u1));
        when(userService.getCustomerStats()).thenReturn(stats);

        String view = controller.listCustomers(model);

        assertEquals("admin/customers", view);
        verify(model).addAttribute("customers", List.of(u1));
        verify(model).addAttribute("totalCustomers", 10L);
        verify(model).addAttribute("vipCount", 3L);
        verify(model).addAttribute("memberCount", 7L);
        verify(model).addAttribute("activeNav", "customers");
    }

    @Test
    @DisplayName("viewCustomerDetail: Hiển thị chi tiết khách hàng thành công")
    void viewCustomerDetail_Success() {
        User customer = User.builder().id(5L).fullName("Test User").build();
        List<UserAddress> addresses = List.of(UserAddress.builder().id(1L).recipientName("Test User").build());
        List<Order> orders = Collections.emptyList();
        List<PointTransactionResponse> pointHistory = Collections.emptyList();

        when(userService.getUserById(5L)).thenReturn(customer);
        when(userAddressService.getAddressesByUserId(5L)).thenReturn(addresses);
        when(orderService.getOrdersByUser(5L)).thenReturn(orders);
        when(userService.getPointHistory(5L)).thenReturn(pointHistory);

        String view = controller.viewCustomerDetail(5L, model, redirectAttributes);

        assertEquals("admin/customer-detail", view);
        verify(model).addAttribute("customer", customer);
        verify(model).addAttribute("addresses", addresses);
        verify(model).addAttribute("orders", orders);
        verify(model).addAttribute("pointHistory", pointHistory);
        verify(model).addAttribute("activeNav", "customers");
    }

    @Test
    @DisplayName("viewCustomerDetail: Người dùng không tồn tại thì redirect về /admin/customers")
    void viewCustomerDetail_NotFound_RedirectsWithFlashError() {
        when(userService.getUserById(999L)).thenThrow(new RuntimeException("Not found"));

        String view = controller.viewCustomerDetail(999L, model, redirectAttributes);

        assertEquals("redirect:/admin/customers", view);
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), any());
    }

    @Test
    @DisplayName("toggleCustomerStatus: Admin không thể tự khóa tài khoản của chính mình")
    void toggleCustomerStatus_SelfLock_Blocked() {
        User selfAdmin = User.builder().id(1L).email("admin@popworld.com").build();
        when(userService.getUserById(1L)).thenReturn(selfAdmin);
        when(principal.getName()).thenReturn("admin@popworld.com");

        String view = controller.toggleCustomerStatus(1L, "detail", principal, redirectAttributes);

        assertEquals("redirect:/admin/customers/1", view);
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), any());
        verify(userService, never()).toggleUserStatus(anyLong());
    }

    @Test
    @DisplayName("toggleCustomerStatus: Khóa/mở khóa thành công và redirect về detail")
    void toggleCustomerStatus_Success_RedirectDetail() {
        User target = User.builder().id(2L).email("user@example.com").build();
        User updated = User.builder().id(2L).enabled(false).build();

        when(userService.getUserById(2L)).thenReturn(target);
        when(principal.getName()).thenReturn("admin@popworld.com");
        when(userService.toggleUserStatus(2L)).thenReturn(updated);

        String view = controller.toggleCustomerStatus(2L, "detail", principal, redirectAttributes);

        assertEquals("redirect:/admin/customers/2", view);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), any());
    }

    @Test
    @DisplayName("toggleCustomerStatus: Khóa/mở khóa thành công và redirect về list")
    void toggleCustomerStatus_Success_RedirectList() {
        User target = User.builder().id(2L).email("user@example.com").build();
        User updated = User.builder().id(2L).enabled(true).build();

        when(userService.getUserById(2L)).thenReturn(target);
        when(principal.getName()).thenReturn("admin@popworld.com");
        when(userService.toggleUserStatus(2L)).thenReturn(updated);

        String view = controller.toggleCustomerStatus(2L, "list", principal, redirectAttributes);

        assertEquals("redirect:/admin/customers", view);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), any());
    }

    @Test
    @DisplayName("adjustPoints: Điều chỉnh điểm thưởng CSKH thành công")
    void adjustPoints_Success() {
        String view = controller.adjustPoints(5L, 100, 20, 1, redirectAttributes);

        assertEquals("redirect:/admin/customers/5", view);
        verify(userService).adjustUserPoints(5L, 100, 20, 1);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), any());
    }

    @Test
    @DisplayName("resetCustomerPassword: Đặt lại mật khẩu thành công và redirect về trang detail")
    void resetCustomerPassword_Success_RedirectDetail() {
        when(principal.getName()).thenReturn("admin@popworld.com");

        String view = controller.resetCustomerPassword(5L, "NewPwd123", "NewPwd123", "detail", principal, redirectAttributes);

        assertEquals("redirect:/admin/customers/5", view);
        verify(userService).adminResetPassword(5L, "NewPwd123", "NewPwd123", "admin@popworld.com");
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), any());
    }

    @Test
    @DisplayName("resetCustomerPassword: Đặt lại mật khẩu thành công và redirect về trang list")
    void resetCustomerPassword_Success_RedirectList() {
        when(principal.getName()).thenReturn("admin@popworld.com");

        String view = controller.resetCustomerPassword(5L, "NewPwd123", "NewPwd123", "list", principal, redirectAttributes);

        assertEquals("redirect:/admin/customers", view);
        verify(userService).adminResetPassword(5L, "NewPwd123", "NewPwd123", "admin@popworld.com");
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), any());
    }

    @Test
    @DisplayName("resetCustomerPassword: Gặp lỗi ném ngoại lệ được bắt và đẩy flash errorMessage")
    void resetCustomerPassword_Failure_CatchesAndSetsFlashError() {
        when(principal.getName()).thenReturn("admin@popworld.com");
        doThrow(new RuntimeException("Mật khẩu không khớp"))
                .when(userService).adminResetPassword(5L, "Pass1", "Pass2", "admin@popworld.com");

        String view = controller.resetCustomerPassword(5L, "Pass1", "Pass2", "detail", principal, redirectAttributes);

        assertEquals("redirect:/admin/customers/5", view);
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), eq("Không thể đặt lại mật khẩu: Mật khẩu không khớp"));
    }
}
