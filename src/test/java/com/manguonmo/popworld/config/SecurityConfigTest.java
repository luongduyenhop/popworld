package com.manguonmo.popworld.config;

import com.manguonmo.popworld.controller.admin.AdminDashboardWebController;
import com.manguonmo.popworld.controller.api.CartApiController;
import com.manguonmo.popworld.controller.client.CartWebController;
import com.manguonmo.popworld.controller.client.CheckoutWebController;
import com.manguonmo.popworld.controller.webhook.SePayWebhookController;
import com.manguonmo.popworld.dto.response.DashboardStatsResponse;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.mapper.CartMapper;
import com.manguonmo.popworld.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AdminDashboardWebController.class, CartWebController.class, CheckoutWebController.class, CartApiController.class, SePayWebhookController.class})
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

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

    @MockitoBean
    private UserAddressService userAddressService;


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

    @Test
    @DisplayName("Gửi POST form /checkout/place-order không có CSRF token -> Bị từ chối HTTP 403 Forbidden")
    void whenPost_withoutCsrf_shouldBeForbidden() throws Exception {
        mockMvc.perform(post("/checkout/place-order")
                        .with(user("user@test.com").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Gửi POST form /checkout/place-order có CSRF token hợp lệ -> Vượt qua bộ lọc CSRF")
    void whenPost_withCsrf_shouldPassCsrfFilter() throws Exception {
        User user = User.builder()
                .id(1L)
                .email("user@test.com")
                .enabled(true)
                .build();
        when(userService.getUserByEmail("user@test.com")).thenReturn(user);

        mockMvc.perform(post("/checkout/place-order")
                        .with(csrf())
                        .with(user("user@test.com").roles("USER"))
                        .param("recipientName", "Nguyen Van A")
                        .param("recipientPhone", "0987654321")
                        .param("provinceCity", "Hà Nội")
                        .param("district", "Cầu Giấy")
                        .param("detailedAddress", "123 Cầu Giấy")
                        .param("paymentMethod", "COD"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Webhook SePay POST /api/payment/sepay/webhook được miễn trừ CSRF -> Không bị 403 Forbidden")
    void whenPostSePayWebhook_withoutCsrf_shouldNotBeBlockedByCsrf() throws Exception {
        when(paymentService.processSePayWebhook(any(), any())).thenReturn(true);

        mockMvc.perform(post("/api/payment/sepay/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gateway\":\"Vietcombank\",\"accumulated\":100000}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Gửi quá 60 request liên tiếp đến SePay Webhook -> Bị RateLimitingFilter chặn với mã 429 Too Many Requests")
    void whenExceedingWebhookRateLimit_shouldReturn429() throws Exception {
        when(paymentService.processSePayWebhook(any(), any())).thenReturn(true);

        for (int i = 0; i < 60; i++) {
            mockMvc.perform(post("/api/payment/sepay/webhook")
                            .header("X-Forwarded-For", "198.51.100.55")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"gateway\":\"Vietcombank\",\"accumulated\":100000}"))
                    .andExpect(status().isOk());
        }

        // Request thứ 61 từ cùng IP
        mockMvc.perform(post("/api/payment/sepay/webhook")
                        .header("X-Forwarded-For", "198.51.100.55")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gateway\":\"Vietcombank\",\"accumulated\":100000}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"));
    }

    @Test
    @DisplayName("POST /logout có CSRF -> Đăng xuất thành công, chuyển hướng về /login?logout=true")
    void whenLogout_shouldRedirectToLoginAndClearSession() throws Exception {
        mockMvc.perform(post("/logout")
                        .with(csrf())
                        .with(user("user@test.com").roles("USER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"));
    }
}
