package com.manguonmo.popworld.security.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService();
    }

    @Test
    @DisplayName("tryAcquire: Cho phép các request trong giới hạn quota")
    void tryAcquire_shouldAllowWithinLimit() {
        String key = "TEST:192.168.1.1";
        int max = 3;
        Duration window = Duration.ofMinutes(1);

        assertTrue(rateLimiterService.tryAcquire(key, max, window));
        assertTrue(rateLimiterService.tryAcquire(key, max, window));
        assertTrue(rateLimiterService.tryAcquire(key, max, window));
    }

    @Test
    @DisplayName("tryAcquire: Từ chối khi vượt quá số lượng request tối đa")
    void tryAcquire_shouldBlockWhenExceedingLimit() {
        String key = "TEST:192.168.1.2";
        int max = 2;
        Duration window = Duration.ofMinutes(1);

        assertTrue(rateLimiterService.tryAcquire(key, max, window));
        assertTrue(rateLimiterService.tryAcquire(key, max, window));
        assertFalse(rateLimiterService.tryAcquire(key, max, window));
        assertFalse(rateLimiterService.tryAcquire(key, max, window));
    }

    @Test
    @DisplayName("tryAcquire: Các key khác nhau (IP hoặc danh mục) không ảnh hưởng lẫn nhau")
    void tryAcquire_shouldIsolateDifferentKeys() {
        String key1 = "COUPON:10.0.0.1";
        String key2 = "COUPON:10.0.0.2";
        int max = 1;
        Duration window = Duration.ofMinutes(1);

        assertTrue(rateLimiterService.tryAcquire(key1, max, window));
        assertFalse(rateLimiterService.tryAcquire(key1, max, window));

        // Key2 vẫn còn nguyên quota
        assertTrue(rateLimiterService.tryAcquire(key2, max, window));
        assertFalse(rateLimiterService.tryAcquire(key2, max, window));
    }

    @Test
    @DisplayName("reset: Xóa quota của key và cho phép gửi request trở lại")
    void reset_shouldClearKeyLimit() {
        String key = "TEST:reset";
        int max = 1;
        Duration window = Duration.ofMinutes(1);

        assertTrue(rateLimiterService.tryAcquire(key, max, window));
        assertFalse(rateLimiterService.tryAcquire(key, max, window));

        rateLimiterService.reset(key);

        assertTrue(rateLimiterService.tryAcquire(key, max, window));
    }

    @Test
    @DisplayName("getRemainingRequests: Tính toán chính xác số lượng request còn lại")
    void getRemainingRequests_shouldCalculateCorrectly() {
        String key = "TEST:remaining";
        int max = 5;
        Duration window = Duration.ofMinutes(1);

        assertEquals(5, rateLimiterService.getRemainingRequests(key, max, window));

        rateLimiterService.tryAcquire(key, max, window);
        rateLimiterService.tryAcquire(key, max, window);

        assertEquals(3, rateLimiterService.getRemainingRequests(key, max, window));
    }
}
