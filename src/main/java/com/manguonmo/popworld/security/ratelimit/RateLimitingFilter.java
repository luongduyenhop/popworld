package com.manguonmo.popworld.security.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Filter bảo vệ hệ thống trước hành vi lạm dụng (Abuse Protection & Rate Limiting).
 * Đặt trước các bộ lọc bảo mật để chặn đứng request spam sớm nhất có thể.
 */
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;

    public RateLimitingFilter(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        RateLimitRule rule = resolveRule(method, path);
        if (rule != null) {
            String clientIp = getClientIp(request);
            String rateLimitKey = rule.category + ":" + clientIp;

            boolean allowed = rateLimiterService.tryAcquire(rateLimitKey, rule.maxRequests, rule.window);
            if (!allowed) {
                handleLimitExceeded(response, path, rule);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private RateLimitRule resolveRule(String method, String path) {
        // 1. Coupon Validate: Chống brute-force coupon codes (10 req/phút)
        if ("POST".equalsIgnoreCase(method) && "/api/coupons/validate".equals(path)) {
            return new RateLimitRule("COUPON", 10, Duration.ofMinutes(1),
                    "Bạn đã vượt quá giới hạn thử mã giảm giá (tối đa 10 lần/phút). Vui lòng thử lại sau ít phút!");
        }

        // 2. User Registration: Chống bot spam tạo tài khoản rác (5 req/phút)
        if ("POST".equalsIgnoreCase(method) && "/register".equals(path)) {
            return new RateLimitRule("REGISTER", 5, Duration.ofMinutes(1),
                    "Bạn đã gửi yêu cầu đăng ký quá nhiều lần (tối đa 5 lần/phút). Vui lòng thử lại sau ít phút!");
        }

        // 3. SePay Webhook: Chống webhook flooding / DoS (60 req/phút)
        if ("POST".equalsIgnoreCase(method) && path.startsWith("/api/payment/sepay/webhook")) {
            return new RateLimitRule("SEPAY_WEBHOOK", 60, Duration.ofMinutes(1),
                    "Tần suất gửi tín hiệu Webhook vượt quá ngưỡng cho phép (tối đa 60 lần/phút).");
        }

        // 4. Cart API mutations: Chống spam giỏ hàng (30 req/phút)
        if (path.startsWith("/api/cart") && ("POST".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method))) {
            return new RateLimitRule("CART_API", 30, Duration.ofMinutes(1),
                    "Thao tác giỏ hàng quá nhanh (tối đa 30 lần/phút). Vui lòng thử lại sau ít phút!");
        }

        // 5. Order API: Chống spam tra cứu và polling bất thường (60 req/phút)
        if (path.startsWith("/api/orders")) {
            return new RateLimitRule("ORDER_API", 60, Duration.ofMinutes(1),
                    "Yêu cầu thông tin đơn hàng vượt quá tần suất cho phép. Vui lòng thử lại sau!");
        }

        return null;
    }

    private void handleLimitExceeded(HttpServletResponse response, String path, RateLimitRule rule) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // HTTP 429
        response.setHeader("Retry-After", "60");

        if (path.startsWith("/api/")) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            String jsonResponse = String.format("{\"success\":false,\"message\":\"%s\",\"data\":null}", rule.errorMessage);
            response.getWriter().write(jsonResponse);
        } else {
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write(rule.errorMessage);
        }
    }

    public static String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    public static class RateLimitRule {
        final String category;
        final int maxRequests;
        final Duration window;
        final String errorMessage;

        public RateLimitRule(String category, int maxRequests, Duration window, String errorMessage) {
            this.category = category;
            this.maxRequests = maxRequests;
            this.window = window;
            this.errorMessage = errorMessage;
        }

        public String getCategory() {
            return category;
        }

        public int getMaxRequests() {
            return maxRequests;
        }

        public Duration getWindow() {
            return window;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
