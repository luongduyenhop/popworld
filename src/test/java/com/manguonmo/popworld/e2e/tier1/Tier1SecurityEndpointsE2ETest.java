package com.manguonmo.popworld.e2e.tier1;

import com.manguonmo.popworld.dto.request.BoxReservationRequest;
import com.manguonmo.popworld.e2e.base.BaseE2ETest;
import com.manguonmo.popworld.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Tier 1 - Security Endpoints, RBAC, IDOR, CSRF, and Rate Limiting E2E Tests")
public class Tier1SecurityEndpointsE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("T1-SEC-01: Chưa đăng nhập truy cập /admin/products -> Redirect 302 về /login")
    void whenUnauthenticated_accessAdmin_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/admin/products"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("T1-SEC-02: Role CLIENT/USER truy cập /admin/products -> Bị chặn 403 Forbidden")
    void whenUserRole_accessAdmin_shouldReturnForbidden() throws Exception {
        User clientUser = createTestUser("client_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");

        mockMvc.perform(get("/admin/products")
                        .with(user(clientUser.getEmail()).roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("T1-SEC-03: Role ADMIN truy cập /admin/products -> Cho phép 200 OK")
    void whenAdminRole_accessAdmin_shouldReturnOk() throws Exception {
        User adminUser = createTestUser("admin_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_ADMIN");

        mockMvc.perform(get("/admin/products")
                        .with(user(adminUser.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("T1-SEC-04: Chưa đăng nhập truy cập /checkout hoặc /cart -> Redirect 302 về /login")
    void whenUnauthenticated_accessProtectedClientRoutes_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/checkout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/cart"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("T1-SEC-05: IDOR Protection trên Đơn hàng - User B không được xem đơn của User A")
    void whenUserB_viewsOrderOfUserA_shouldBeRejected() throws Exception {
        User userA = createTestUser("usera_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");
        User userB = createTestUser("userb_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");
        Product product = createTestProduct("IDOR Product", 10, BigDecimal.valueOf(100000), null);

        // User A creates order
        cartService.addToCart(userA.getId(), product.getId(), "SINGLE_BOX", 1);
        Order orderA = orderService.createOrder(userA.getId(), "User A", "0987654321", "Ha Noi", "Cau Giay", "Dich Vong", "123 Xuan Thuy", "COD", null);

        // User B attempts to access User A's order by orderCode
        mockMvc.perform(get("/api/orders/" + orderA.getOrderCode())
                        .with(user(userB.getEmail()).roles("USER")))
                .andExpect(status().is(either(is(403)).or(is(400))))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("quyền")));

        // User B attempts to poll status of User A's order
        mockMvc.perform(get("/api/orders/" + orderA.getOrderCode() + "/status")
                        .with(user(userB.getEmail()).roles("USER")))
                .andExpect(status().is(either(is(403)).or(is(400))))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("quyền")));
    }

    @Test
    @DisplayName("T1-SEC-06: IDOR Protection trên Giỏ hàng - User B không được sửa hoặc xóa CartItem của User A")
    void whenUserB_modifiesCartItemOfUserA_shouldBeRejected() throws Exception {
        User userA = createTestUser("usera_cart_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");
        User userB = createTestUser("userb_cart_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");
        Product product = createTestProduct("Cart IDOR Product", 10, BigDecimal.valueOf(100000), null);

        CartItem itemA = cartService.addToCart(userA.getId(), product.getId(), "SINGLE_BOX", 1);

        // User B attempts to update quantity of User A's cart item
        mockMvc.perform(patch("/api/cart/items/" + itemA.getId() + "/quantity")
                        .param("quantity", "5")
                        .with(user(userB.getEmail()).roles("USER"))
                        .with(csrf()))
                .andExpect(status().is(either(is(403)).or(is(400))))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("quyền")));

        // User B attempts to delete User A's cart item
        mockMvc.perform(delete("/api/cart/items/" + itemA.getId())
                        .with(user(userB.getEmail()).roles("USER"))
                        .with(csrf()))
                .andExpect(status().is(either(is(403)).or(is(400))))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("quyền")));
    }

    @Test
    @DisplayName("T1-SEC-07: IDOR Protection trên Blind Box POP NOW - User B không được thao tác trên phiếu giữ hộp của User A")
    void whenUserB_operatesOnReservationOfUserA_shouldBeRejected() throws Exception {
        User userA = createTestUser("usera_pn_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");
        User userB = createTestUser("userb_pn_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");
        Product product = createTestProduct("PopNow IDOR Product", 10, BigDecimal.valueOf(150000), null);

        BoxReservationRequest request = new BoxReservationRequest();
        request.setProductId(product.getId());
        request.setBoxIndex(1);

        var reservationA = popNowService.reserveBox(userA.getId(), request);

        // User B attempts to cancel User A's reservation
        mockMvc.perform(post("/api/popnow/cancel")
                        .param("reservationCode", reservationA.getReservationCode())
                        .with(user(userB.getEmail()).roles("USER"))
                        .with(csrf()))
                .andExpect(status().is(either(is(403)).or(is(400))))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("quyền")));

        // User B attempts to unbox User A's reservation
        mockMvc.perform(post("/api/popnow/unbox")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationCode\":\"" + reservationA.getReservationCode() + "\"}")
                        .with(user(userB.getEmail()).roles("USER"))
                        .with(csrf()))
                .andExpect(status().is(either(is(403)).or(is(400))))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("quyền")));
    }

    @Test
    @DisplayName("T1-SEC-08: CSRF Protection - Thao tác thay đổi trạng thái không có CSRF token bị chặn 403")
    void whenPostWithoutCsrf_shouldReturnForbidden() throws Exception {
        User user = createTestUser("user_csrf_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");

        mockMvc.perform(post("/checkout/place-order")
                        .param("recipientName", "Nguyen Van A")
                        .param("recipientPhone", "0987654321")
                        .param("provinceCity", "Ha Noi")
                        .param("detailedAddress", "123 Xuan Thuy")
                        .with(user(user.getEmail()).roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("T1-SEC-09: Rate Limiting - Gửi quá 10 request validate coupon/phút bị chặn HTTP 429 Too Many Requests")
    void whenRapidCouponValidate_shouldTriggerRateLimit() throws Exception {
        String testIp = "192.168.100." + (new java.util.Random().nextInt(200) + 10);

        for (int i = 1; i <= 10; i++) {
            mockMvc.perform(post("/api/coupons/validate")
                            .header("X-Forwarded-For", testIp)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"couponCode\":\"NONEXISTENT\",\"subtotal\":100000}")
                            .with(csrf()))
                    .andExpect(status().is(either(is(400)).or(is(404))));
        }

        // Request thứ 11 từ cùng client IP phải bị chặn HTTP 429
        mockMvc.perform(post("/api/coupons/validate")
                        .header("X-Forwarded-For", testIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"couponCode\":\"NONEXISTENT\",\"subtotal\":100000}")
                        .with(csrf()))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("giới hạn")));
    }

    @Test
    @DisplayName("T1-SEC-10: Rate Limiting - Gửi quá 5 request /register/phút bị chặn HTTP 429 Too Many Requests")
    void whenRapidRegisterRequests_shouldTriggerRateLimit() throws Exception {
        String testIp = "192.168.200." + (new java.util.Random().nextInt(200) + 10);

        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/register")
                            .header("X-Forwarded-For", testIp)
                            .param("fullName", "Spam User")
                            .param("email", "spam_" + i + "@example.com")
                            .param("password", "short")
                            .param("confirmPassword", "different")
                            .with(csrf()))
                    .andExpect(status().isOk());
        }

        // Request thứ 6 từ cùng IP bị chặn HTTP 429
        mockMvc.perform(post("/register")
                        .header("X-Forwarded-For", testIp)
                        .param("fullName", "Spam User 6")
                        .param("email", "spam_6@example.com")
                        .param("password", "short")
                        .param("confirmPassword", "different")
                        .with(csrf()))
                .andExpect(status().isTooManyRequests());
    }
}
