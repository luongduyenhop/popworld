package com.manguonmo.popworld.config;

import com.manguonmo.popworld.controller.admin.AdminDashboardWebController;
import com.manguonmo.popworld.controller.api.CartApiController;
import com.manguonmo.popworld.controller.client.CartWebController;
import com.manguonmo.popworld.controller.client.CheckoutWebController;
import com.manguonmo.popworld.dto.response.DashboardStatsResponse;
import com.manguonmo.popworld.mapper.CartMapper;
import com.manguonmo.popworld.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AdminDashboardWebController.class, CartWebController.class, CheckoutWebController.class, CartApiController.class})
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private CharacterIpService characterIpService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private CartMapper cartMapper;

    @MockitoBean
    private com.manguonmo.popworld.repository.CouponRepository couponRepository;


    @Test
    @DisplayName("Chưa đăng nhập truy cập /admin/** -> Redirect về /login")
    void whenUnauthenticated_accessAdmin_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("ROLE_USER truy cập /admin/** -> Bị từ chối và forward về /403 kèm mã 403 Forbidden")
    void whenRoleUser_accessAdmin_shouldForwardTo403() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("user@test.com").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/403"));
    }

    @Test
    @DisplayName("ROLE_ADMIN truy cập /admin/** -> Được phép truy cập thành công")
    void whenRoleAdmin_accessAdmin_shouldBeAllowed() throws Exception {
        when(dashboardService.getDashboardStats()).thenReturn(DashboardStatsResponse.builder()
                .characterIps(java.util.List.of())
                .recentOrders(java.util.List.of())
                .build());

        mockMvc.perform(get("/admin/dashboard")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Chưa đăng nhập truy cập /cart/** -> Redirect về /login")
    void whenUnauthenticated_accessCart_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/cart"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("Chưa đăng nhập truy cập /api/cart/** -> Redirect về /login")
    void whenUnauthenticated_accessApiCart_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("Chưa đăng nhập truy cập /checkout/** -> Redirect về /login")
    void whenUnauthenticated_accessCheckout_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/checkout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("Chưa đăng nhập truy cập /orders -> Redirect về /login")
    void whenUnauthenticated_accessOrders_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}
