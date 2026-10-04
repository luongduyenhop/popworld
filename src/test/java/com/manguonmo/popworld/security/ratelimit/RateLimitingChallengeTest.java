package com.manguonmo.popworld.security.ratelimit;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Rate Limiting Adversarial & Stress Challenge Tests")
class RateLimitingChallengeTest {

    private RateLimiterService rateLimiterService;
    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService();
        rateLimitingFilter = new RateLimitingFilter(rateLimiterService);
    }

    @Test
    @DisplayName("Challenge Login: Đúng 10 request được phép, request thứ 11 bị 429, IP khác hoàn toàn độc lập")
    void login_exactBoundary_10Allowed_11thBlocked_andIpIsolation() throws ServletException, IOException {
        String attackerIp = "10.0.0.1";
        String victimIp = "10.0.0.2";

        // 10 requests from attacker -> all allowed
        for (int i = 1; i <= 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/login");
            req.setRemoteAddr(attackerIp);
            MockHttpServletResponse res = new MockHttpServletResponse();
            rateLimitingFilter.doFilter(req, res, new MockFilterChain());
            assertEquals(200, res.getStatus(), "Request " + i + " must succeed");
        }

        // 11th request from attacker -> 429 Too Many Requests
        MockHttpServletRequest blockedReq = new MockHttpServletRequest("POST", "/login");
        blockedReq.setRemoteAddr(attackerIp);
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();
        rateLimitingFilter.doFilter(blockedReq, blockedRes, new MockFilterChain());
        assertEquals(429, blockedRes.getStatus(), "11th request must be rate-limited");
        assertEquals("60", blockedRes.getHeader("Retry-After"));

        // Victim IP must NOT be blocked
        MockHttpServletRequest victimReq = new MockHttpServletRequest("POST", "/login");
        victimReq.setRemoteAddr(victimIp);
        MockHttpServletResponse victimRes = new MockHttpServletResponse();
        rateLimitingFilter.doFilter(victimReq, victimRes, new MockFilterChain());
        assertEquals(200, victimRes.getStatus(), "Separate IP must not be affected by attacker");
    }

    @Test
    @DisplayName("Challenge Login: GET /login không bị giới hạn tần suất bởi POST rule")
    void login_getMethod_notRateLimited() throws ServletException, IOException {
        String clientIp = "10.0.0.3";
        for (int i = 1; i <= 30; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/login");
            req.setRemoteAddr(clientIp);
            MockHttpServletResponse res = new MockHttpServletResponse();
            rateLimitingFilter.doFilter(req, res, new MockFilterChain());
            assertEquals(200, res.getStatus());
        }
    }

    @Test
    @DisplayName("Challenge Checkout Place Order: Đúng 15 request được phép, request thứ 16 bị 429, IP khác độc lập")
    void checkoutPlaceOrder_exactBoundary_15Allowed_16thBlocked_andIpIsolation() throws ServletException, IOException {
        String ip1 = "192.168.10.1";
        String ip2 = "192.168.10.2";

        for (int i = 1; i <= 15; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/checkout/place-order");
            req.setRemoteAddr(ip1);
            MockHttpServletResponse res = new MockHttpServletResponse();
            rateLimitingFilter.doFilter(req, res, new MockFilterChain());
            assertEquals(200, res.getStatus());
        }

        MockHttpServletRequest blockedReq = new MockHttpServletRequest("POST", "/checkout/place-order");
        blockedReq.setRemoteAddr(ip1);
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();
        rateLimitingFilter.doFilter(blockedReq, blockedRes, new MockFilterChain());
        assertEquals(429, blockedRes.getStatus());
        assertEquals("60", blockedRes.getHeader("Retry-After"));
        assertTrue(blockedRes.getContentAsString().contains("đặt hàng quá thường xuyên"));

        // IP2 is independent
        MockHttpServletRequest ip2Req = new MockHttpServletRequest("POST", "/checkout/place-order");
        ip2Req.setRemoteAddr(ip2);
        MockHttpServletResponse ip2Res = new MockHttpServletResponse();
        rateLimitingFilter.doFilter(ip2Req, ip2Res, new MockFilterChain());
        assertEquals(200, ip2Res.getStatus());
    }

    @Test
    @DisplayName("Challenge Order Tracking: Đúng 15 request được phép, request thứ 16 bị 429, IP khác độc lập")
    void orderTracking_exactBoundary_15Allowed_16thBlocked_andIpIsolation() throws ServletException, IOException {
        String ip1 = "172.16.0.1";
        String ip2 = "172.16.0.2";

        for (int i = 1; i <= 15; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/order-tracking");
            req.setRemoteAddr(ip1);
            MockHttpServletResponse res = new MockHttpServletResponse();
            rateLimitingFilter.doFilter(req, res, new MockFilterChain());
            assertEquals(200, res.getStatus());
        }

        MockHttpServletRequest blockedReq = new MockHttpServletRequest("POST", "/order-tracking");
        blockedReq.setRemoteAddr(ip1);
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();
        rateLimitingFilter.doFilter(blockedReq, blockedRes, new MockFilterChain());
        assertEquals(429, blockedRes.getStatus());
        assertEquals("60", blockedRes.getHeader("Retry-After"));
        assertTrue(blockedRes.getContentAsString().contains("tra cứu đơn hàng quá nhiều lần"));

        // IP2 is independent
        MockHttpServletRequest ip2Req = new MockHttpServletRequest("POST", "/order-tracking");
        ip2Req.setRemoteAddr(ip2);
        MockHttpServletResponse ip2Res = new MockHttpServletResponse();
        rateLimitingFilter.doFilter(ip2Req, ip2Res, new MockFilterChain());
        assertEquals(200, ip2Res.getStatus());
    }

    @Test
    @DisplayName("Challenge POP NOW Reserve: Đúng 20 request được phép, request thứ 21 trả JSON 429 format chuẩn")
    void popNowReserve_exactBoundary_20Allowed_21stBlocked_returnsJson429() throws ServletException, IOException {
        String ip1 = "10.20.30.40";

        for (int i = 1; i <= 20; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/popnow/reserve");
            req.setRemoteAddr(ip1);
            MockHttpServletResponse res = new MockHttpServletResponse();
            rateLimitingFilter.doFilter(req, res, new MockFilterChain());
            assertEquals(200, res.getStatus());
        }

        MockHttpServletRequest blockedReq = new MockHttpServletRequest("POST", "/api/popnow/reserve");
        blockedReq.setRemoteAddr(ip1);
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();
        rateLimitingFilter.doFilter(blockedReq, blockedRes, new MockFilterChain());

        assertEquals(429, blockedRes.getStatus());
        assertEquals("60", blockedRes.getHeader("Retry-After"));
        assertTrue(blockedRes.getContentType().startsWith("application/json"));
        assertTrue(blockedRes.getContentAsString().contains("\"success\":false"));
        assertTrue(blockedRes.getContentAsString().contains("giữ hộp quá nhiều lần"));
    }

    @Test
    @DisplayName("Challenge Concurrency: 100 luồng đồng thời tranh chấp quota 15 permit -> Chính xác 15 thành công, 85 bị từ chối")
    void rateLimiterService_concurrentStressTest_exactConcurrencyInvariants() throws InterruptedException {
        String key = "CONCURRENCY_TEST:127.0.0.1";
        int totalThreads = 100;
        int maxPermits = 15;
        Duration window = Duration.ofMinutes(1);

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch readyLatch = new CountDownLatch(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        for (int i = 0; i < totalThreads; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    boolean acquired = rateLimiterService.tryAcquire(key, maxPermits, window);
                    if (acquired) {
                        successCount.incrementAndGet();
                    } else {
                        rejectedCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertEquals(maxPermits, successCount.get(), "Chính xác 15 request phải thành công trong môi trường đa luồng");
        assertEquals(totalThreads - maxPermits, rejectedCount.get(), "Chính xác 85 request phải bị từ chối");
    }
}
