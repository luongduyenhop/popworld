package com.manguonmo.popworld.security.ratelimit;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitingFilterTest {

    private RateLimiterService rateLimiterService;
    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService();
        rateLimitingFilter = new RateLimitingFilter(rateLimiterService);
    }

    @Test
    @DisplayName("Coupon Validate: Cho phép tối đa 10 request và chặn request thứ 11 với mã 429 Too Many Requests")
    void couponValidate_shouldAllowUpTo10Requests_andBlock11th() throws ServletException, IOException {
        String clientIp = "192.168.1.100";

        for (int i = 1; i <= 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/coupons/validate");
            req.setRemoteAddr(clientIp);
            MockHttpServletResponse res = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            rateLimitingFilter.doFilter(req, res, chain);
            assertEquals(200, res.getStatus());
        }

        // Request thứ 11 trong vòng 1 phút
        MockHttpServletRequest blockedReq = new MockHttpServletRequest("POST", "/api/coupons/validate");
        blockedReq.setRemoteAddr(clientIp);
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();
        MockFilterChain blockedChain = new MockFilterChain();

        rateLimitingFilter.doFilter(blockedReq, blockedRes, blockedChain);

        assertEquals(429, blockedRes.getStatus());
        assertEquals("60", blockedRes.getHeader("Retry-After"));
        assertTrue(blockedRes.getContentAsString().contains("Bạn đã vượt quá giới hạn thử mã giảm giá"));
        assertTrue(blockedRes.getContentAsString().contains("\"success\":false"));
    }

    @Test
    @DisplayName("Register: Cho phép tối đa 5 lần đăng ký/phút và chặn request thứ 6 với mã 429")
    void register_shouldAllowUpTo5Requests_andBlock6th() throws ServletException, IOException {
        String clientIp = "192.168.1.200";

        for (int i = 1; i <= 5; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/register");
            req.setRemoteAddr(clientIp);
            MockHttpServletResponse res = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            rateLimitingFilter.doFilter(req, res, chain);
            assertEquals(200, res.getStatus());
        }

        // Request thứ 6 bị chặn
        MockHttpServletRequest blockedReq = new MockHttpServletRequest("POST", "/register");
        blockedReq.setRemoteAddr(clientIp);
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();
        MockFilterChain blockedChain = new MockFilterChain();

        rateLimitingFilter.doFilter(blockedReq, blockedRes, blockedChain);

        assertEquals(429, blockedRes.getStatus());
        assertTrue(blockedRes.getContentAsString().contains("Bạn đã gửi yêu cầu đăng ký quá nhiều lần"));
    }

    @Test
    @DisplayName("SePay Webhook: Được phép gửi tối đa 60 webhook/phút và chặn khi vượt quá")
    void sePayWebhook_shouldEnforceRateLimit() throws ServletException, IOException {
        String clientIp = "123.45.67.89";

        for (int i = 1; i <= 60; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/payment/sepay/webhook");
            req.setRemoteAddr(clientIp);
            MockHttpServletResponse res = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            rateLimitingFilter.doFilter(req, res, chain);
            assertEquals(200, res.getStatus());
        }

        // Request thứ 61 bị chặn
        MockHttpServletRequest blockedReq = new MockHttpServletRequest("POST", "/api/payment/sepay/webhook");
        blockedReq.setRemoteAddr(clientIp);
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();
        MockFilterChain blockedChain = new MockFilterChain();

        rateLimitingFilter.doFilter(blockedReq, blockedRes, blockedChain);

        assertEquals(429, blockedRes.getStatus());
        assertTrue(blockedRes.getContentAsString().contains("Tần suất gửi tín hiệu Webhook vượt quá"));
    }

    @Test
    @DisplayName("Public/Catalog routes: Các route duyệt web thông thường không bị áp dụng giới hạn gắt gao")
    void normalRoutes_shouldNotBeBlocked() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/products/labubu-fall-in-wild");
        req.setRemoteAddr("192.168.1.1");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        rateLimitingFilter.doFilter(req, res, chain);
        assertEquals(200, res.getStatus());
    }

    @Test
    @DisplayName("getClientIp: Trích xuất chính xác IP gốc từ header X-Forwarded-For")
    void getClientIp_shouldExtractFromXForwardedFor() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Forwarded-For", "203.0.113.195, 70.41.3.18, 150.172.238.178");
        req.setRemoteAddr("10.0.0.1");

        String ip = RateLimitingFilter.getClientIp(req);
        assertEquals("203.0.113.195", ip);
    }
}
