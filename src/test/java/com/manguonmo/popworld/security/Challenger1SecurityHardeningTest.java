package com.manguonmo.popworld.security;

import com.manguonmo.popworld.config.SecurityConfig;
import com.manguonmo.popworld.controller.api.CartApiController;
import com.manguonmo.popworld.controller.api.OrderApiController;
import com.manguonmo.popworld.controller.api.PopNowApiController;
import com.manguonmo.popworld.controller.client.PopNowWebController;
import com.manguonmo.popworld.entity.BoxReservation;
import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.ReservationStatus;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.exception.GlobalExceptionHandler;
import com.manguonmo.popworld.mapper.CartMapper;
import com.manguonmo.popworld.mapper.OrderMapper;
import com.manguonmo.popworld.repository.BoxReservationRepository;
import com.manguonmo.popworld.repository.CouponRepository;
import com.manguonmo.popworld.repository.OrderRepository;
import com.manguonmo.popworld.repository.OwnedItemRepository;
import com.manguonmo.popworld.security.ratelimit.RateLimiterService;
import com.manguonmo.popworld.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Empirical Security Challenge Test Suite by Challenger 1 (Milestone 1).
 * Directly tests and stresses:
 * 1. CSRF enforcement on POST /api/popnow/simulate-payment and POST /popnow/checkout/{code}
 * 2. IDOR prevention yielding HTTP 403 Forbidden across cart, orders, and unbox reservations.
 */
@WebMvcTest(controllers = {
        PopNowApiController.class,
        PopNowWebController.class,
        OrderApiController.class,
        CartApiController.class
})
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class Challenger1SecurityHardeningTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PopNowService popNowService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private OrderMapper orderMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private CartMapper cartMapper;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private CharacterIpService characterIpService;

    @MockitoBean
    private UserAddressService userAddressService;

    @MockitoBean
    private PopNowThemeService popNowThemeService;

    @MockitoBean
    private BoxReservationRepository boxReservationRepository;

    @MockitoBean
    private OwnedItemRepository ownedItemRepository;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private CouponRepository couponRepository;

    @MockitoBean
    private WishlistService wishlistService;

    @MockitoBean
    private RateLimiterService rateLimiterService;

    private User attackerUser;
    private User victimUser;

    @BeforeEach
    void setupUsers() {
        attackerUser = User.builder()
                .id(999L)
                .email("attacker@popworld.com")
                .role("ROLE_USER")
                .enabled(true)
                .build();

        victimUser = User.builder()
                .id(111L)
                .email("victim@popworld.com")
                .role("ROLE_USER")
                .enabled(true)
                .build();

        when(userService.getUserByEmail("attacker@popworld.com")).thenReturn(attackerUser);
        when(userService.getUserByEmail("victim@popworld.com")).thenReturn(victimUser);
        org.mockito.Mockito.lenient().when(rateLimiterService.tryAcquire(any(), anyInt(), any())).thenReturn(true);
    }

    // =========================================================================
    // SECTION 1: CSRF ENFORCEMENT CHALLENGES
    // =========================================================================

    @Test
    @DisplayName("CSRF Challenge: POST /api/popnow/simulate-payment KHÔNG có CSRF token -> Phải bị HTTP 403 Forbidden")
    void simulatePayment_withoutCsrf_mustBeBlockedWith403() throws Exception {
        mockMvc.perform(post("/api/popnow/simulate-payment")
                        .with(user("attacker@popworld.com").roles("USER"))
                        .param("orderCode", "PW-123456"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CSRF Challenge: POST /api/popnow/simulate-payment CÓ CSRF token hợp lệ -> Vượt qua CSRF filter")
    void simulatePayment_withCsrf_passesCsrfFilter() throws Exception {
        Order order = Order.builder()
                .orderCode("PW-123456")
                .user(attackerUser)
                .status("TO_PAY")
                .build();
        when(orderService.getOrderByCode("PW-123456")).thenReturn(order);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        mockMvc.perform(post("/api/popnow/simulate-payment")
                        .with(csrf())
                        .with(user("attacker@popworld.com").roles("USER"))
                        .param("orderCode", "PW-123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("CSRF Challenge: POST /popnow/checkout/{reservationCode} KHÔNG có CSRF token -> Bị HTTP 403 Forbidden")
    void popNowCheckout_withoutCsrf_mustBeBlockedWith403() throws Exception {
        mockMvc.perform(post("/popnow/checkout/PN-RES-999")
                        .with(user("attacker@popworld.com").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CSRF Challenge: POST /popnow/checkout/{reservationCode} CÓ CSRF token -> Vượt qua CSRF filter")
    void popNowCheckout_withCsrf_passesCsrfFilter() throws Exception {
        Order order = Order.builder()
                .orderCode("PW-POP-999")
                .paymentMethod("SEPAY")
                .build();
        when(orderService.createOrderForReservation(eq(999L), eq("PN-RES-999"), anyString())).thenReturn(order);

        mockMvc.perform(post("/popnow/checkout/PN-RES-999")
                        .with(csrf())
                        .with(user("attacker@popworld.com").roles("USER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/checkout/payment/PW-POP-999"));
    }

    // =========================================================================
    // SECTION 2: IDOR PREVENTION CHALLENGES (Must return HTTP 403 Forbidden)
    // =========================================================================

    @Test
    @DisplayName("IDOR Challenge (Cart Item Quantity): Attacker cố sửa số lượng món của Victim -> HTTP 403 Forbidden")
    void cartItemQuantity_whenAttackerAccessesVictimItem_mustReturn403() throws Exception {
        doThrow(new AccessDeniedException("Bạn không có quyền thao tác trên món hàng này!"))
                .when(cartService).updateQuantity(eq(999L), eq(55L), eq(10));

        mockMvc.perform(patch("/api/cart/items/55/quantity")
                        .with(csrf())
                        .with(user("attacker@popworld.com").roles("USER"))
                        .param("quantity", "10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền thao tác trên món hàng này!"));
    }

    @Test
    @DisplayName("IDOR Challenge (Cart Item Remove): Attacker cố xóa món hàng của Victim -> HTTP 403 Forbidden")
    void cartItemRemove_whenAttackerDeletesVictimItem_mustReturn403() throws Exception {
        doThrow(new AccessDeniedException("Bạn không có quyền thao tác trên món hàng này!"))
                .when(cartService).removeFromCart(eq(999L), eq(55L));

        mockMvc.perform(delete("/api/cart/items/55")
                        .with(csrf())
                        .with(user("attacker@popworld.com").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền thao tác trên món hàng này!"));
    }

    @Test
    @DisplayName("IDOR Challenge (Order Detail): Attacker cố xem thông tin đơn hàng của Victim -> HTTP 403 Forbidden")
    void orderDetail_whenAttackerAccessesVictimOrder_mustReturn403() throws Exception {
        Order victimOrder = Order.builder()
                .id(100L)
                .orderCode("PW-VICTIM-01")
                .user(victimUser)
                .build();
        when(orderService.getOrderByCode("PW-VICTIM-01")).thenReturn(victimOrder);

        mockMvc.perform(get("/api/orders/PW-VICTIM-01")
                        .with(user("attacker@popworld.com").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền xem thông tin đơn hàng này."));
    }

    @Test
    @DisplayName("IDOR Challenge (Order Status Polling): Attacker cố poll trạng thái đơn của Victim -> HTTP 403 Forbidden")
    void orderStatus_whenAttackerPollsVictimOrder_mustReturn403() throws Exception {
        Order victimOrder = Order.builder()
                .id(100L)
                .orderCode("PW-VICTIM-02")
                .user(victimUser)
                .status("PROCESSING")
                .build();
        when(orderService.getOrderByCode("PW-VICTIM-02")).thenReturn(victimOrder);

        mockMvc.perform(get("/api/orders/PW-VICTIM-02/status")
                        .with(user("attacker@popworld.com").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền xem thông tin đơn hàng này."));
    }

    @Test
    @DisplayName("IDOR Challenge (Unbox Reservation API): Attacker gọi unbox phiếu của Victim -> HTTP 403 Forbidden")
    void unboxReservation_whenAttackerUnboxesVictimReservation_mustReturn403() throws Exception {
        when(popNowService.unbox(eq(999L), eq("PN-VICTIM-RES")))
                .thenThrow(new AccessDeniedException("Bạn không có quyền mở hộp với mã phiếu này!"));

        mockMvc.perform(post("/api/popnow/unbox")
                        .with(csrf())
                        .with(user("attacker@popworld.com").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationCode\":\"PN-VICTIM-RES\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền mở hộp với mã phiếu này!"));
    }

    @Test
    @DisplayName("IDOR Challenge (Reveal Web Page): Attacker truy cập trang reveal phiếu của Victim -> Redirect /403")
    void revealPage_whenAttackerAccessesVictimReservation_redirectsTo403() throws Exception {
        BoxReservation victimReservation = BoxReservation.builder()
                .reservationCode("PN-VICTIM-RES")
                .user(victimUser)
                .status(ReservationStatus.PURCHASED)
                .build();
        when(boxReservationRepository.findByReservationCode("PN-VICTIM-RES"))
                .thenReturn(Optional.of(victimReservation));

        mockMvc.perform(get("/popnow/reveal/PN-VICTIM-RES")
                        .with(user("attacker@popworld.com").roles("USER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/403"));
    }
}
