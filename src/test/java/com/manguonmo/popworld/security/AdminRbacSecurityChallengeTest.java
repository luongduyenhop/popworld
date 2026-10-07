package com.manguonmo.popworld.security;

import com.manguonmo.popworld.config.SecurityConfig;
import com.manguonmo.popworld.controller.admin.*;
import com.manguonmo.popworld.dto.response.InventorySummaryResponse;
import com.manguonmo.popworld.dto.response.RevenueReportResponse;
import com.manguonmo.popworld.dto.response.ReviewStatsResponse;
import com.manguonmo.popworld.repository.SeriesRepository;
import com.manguonmo.popworld.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {
        AdminDashboardWebController.class,
        AdminCouponWebController.class,
        AdminCustomerWebController.class,
        AdminDatabaseWebController.class,
        AdminInventoryWebController.class,
        AdminOrderWebController.class,
        AdminPopNowWebController.class,
        AdminProductWebController.class,
        AdminReportWebController.class,
        AdminReviewWebController.class,
        AdminSupportWebController.class
})
@Import(SecurityConfig.class)
@DisplayName("Admin RBAC & Access Control Challenge Across All 11 Controllers")
class AdminRbacSecurityChallengeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private CouponService couponService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserAddressService userAddressService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private RefundService refundService;

    @MockitoBean
    private DatabaseSeedService databaseSeedService;

    @MockitoBean
    private InventoryService inventoryService;

    @MockitoBean
    private PopNowAdminService popNowAdminService;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private SeriesRepository seriesRepository;

    @MockitoBean
    private com.manguonmo.popworld.repository.CharacterIpRepository characterIpRepository;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private ContactService contactService;

    @MockitoBean
    private CharacterIpService characterIpService;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private WishlistService wishlistService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        org.mockito.Mockito.when(dashboardService.getDashboardStats())
                .thenReturn(com.manguonmo.popworld.dto.response.DashboardStatsResponse.builder()
                        .characterIps(java.util.List.of())
                        .recentOrders(java.util.List.of())
                        .build());
        org.mockito.Mockito.when(userService.getCustomerStats())
                .thenReturn(com.manguonmo.popworld.dto.response.CustomerStatsResponse.builder().build());
        org.mockito.Mockito.when(productService.getProductStats())
                .thenReturn(com.manguonmo.popworld.dto.response.ProductStatsResponse.builder().build());
        org.mockito.Mockito.when(inventoryService.getInventorySummary(org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(InventorySummaryResponse.builder()
                        .totalInventoryValue(java.math.BigDecimal.ZERO)
                        .build());
        org.mockito.Mockito.when(reportService.getRevenueReport(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(RevenueReportResponse.builder()
                        .grossRevenue(java.math.BigDecimal.ZERO)
                        .totalRefunded(java.math.BigDecimal.ZERO)
                        .netRevenue(java.math.BigDecimal.ZERO)
                        .averageOrderValue(java.math.BigDecimal.ZERO)
                        .codRevenue(java.math.BigDecimal.ZERO)
                        .bankingRevenue(java.math.BigDecimal.ZERO)
                        .topProducts(java.util.List.of())
                        .dailyBreakdown(java.util.List.of())
                        .build());
        org.mockito.Mockito.when(reviewService.getReviewStats())
                .thenReturn(ReviewStatsResponse.builder().build());
    }

    @ParameterizedTest(name = "Unauthenticated: GET {0} -> Redirect to /login")
    @ValueSource(strings = {
            "/admin",
            "/admin/dashboard",
            "/admin/coupons",
            "/admin/customers",
            "/admin/inventory",
            "/admin/orders",
            "/admin/popnow",
            "/admin/products",
            "/admin/reports",
            "/admin/reviews",
            "/admin/support"
    })
    void unauthenticated_getEndpoints_shouldRedirectToLogin(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("Unauthenticated: POST /admin/database/reseed -> Redirect to /login")
    void unauthenticated_postDatabaseReseed_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(post("/admin/database/reseed").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @ParameterizedTest(name = "Non-admin (ROLE_USER): GET {0} -> 403 Forbidden")
    @ValueSource(strings = {
            "/admin",
            "/admin/dashboard",
            "/admin/coupons",
            "/admin/customers",
            "/admin/inventory",
            "/admin/orders",
            "/admin/popnow",
            "/admin/products",
            "/admin/reports",
            "/admin/reviews",
            "/admin/support"
    })
    void roleUser_getEndpoints_shouldBeForbidden(String path) throws Exception {
        mockMvc.perform(get(path)
                        .with(user("user@example.com").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/403"));
    }

    @ParameterizedTest(name = "Non-admin (ROLE_CLIENT): GET {0} -> 403 Forbidden")
    @ValueSource(strings = {
            "/admin",
            "/admin/dashboard",
            "/admin/coupons",
            "/admin/customers",
            "/admin/inventory",
            "/admin/orders",
            "/admin/popnow",
            "/admin/products",
            "/admin/reports",
            "/admin/reviews",
            "/admin/support"
    })
    void roleClient_getEndpoints_shouldBeForbidden(String path) throws Exception {
        mockMvc.perform(get(path)
                        .with(user("client@example.com").roles("CLIENT")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/403"));
    }

    @Test
    @DisplayName("Non-admin (ROLE_USER): POST /admin/database/reseed -> 403 Forbidden")
    void roleUser_postDatabaseReseed_shouldBeForbidden() throws Exception {
        mockMvc.perform(post("/admin/database/reseed")
                        .with(csrf())
                        .with(user("user@example.com").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/403"));
    }

    @ParameterizedTest(name = "Admin (ROLE_ADMIN): GET {0} -> Allowed through security")
    @ValueSource(strings = {
            "/admin",
            "/admin/coupons",
            "/admin/customers",
            "/admin/inventory",
            "/admin/orders",
            "/admin/popnow",
            "/admin/products",
            "/admin/reports",
            "/admin/reviews",
            "/admin/support"
    })
    void roleAdmin_getEndpoints_shouldNotBeForbiddenOrRedirectedToLogin(String path) throws Exception {
        mockMvc.perform(get(path)
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }
}
