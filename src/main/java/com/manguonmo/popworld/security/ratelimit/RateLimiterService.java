package com.manguonmo.popworld.security.ratelimit;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service quản lý giới hạn tần suất (Rate Limiting) in-memory cho MVP.
 * Sử dụng cơ chế Fixed Window Counter thread-safe, không đòi hỏi infrastructure Redis bên ngoài.
 */
@Service
public class RateLimiterService {

    private static final int MAX_ENTRIES = 10_000;
    private final ConcurrentHashMap<String, WindowCounter> buckets = new ConcurrentHashMap<>();

    /**
     * Thử lấy quota cho key xác định.
     *
     * @param key         Định danh giới hạn (ví dụ: COUPON:192.168.1.1)
     * @param maxRequests Số lượng request tối đa trong cửa sổ thời gian
     * @param window      Độ dài cửa sổ thời gian
     * @return true nếu request được phép thực thi, false nếu vượt quá quota
     */
    public boolean tryAcquire(String key, int maxRequests, Duration window) {
        long now = System.currentTimeMillis();
        long windowMillis = window.toMillis();

        if (buckets.size() > MAX_ENTRIES) {
            cleanup(now);
        }

        WindowCounter counter = buckets.compute(key, (k, existing) -> {
            if (existing == null || (now - existing.windowStart >= windowMillis)) {
                return new WindowCounter(now, 1);
            } else {
                existing.count.incrementAndGet();
                return existing;
            }
        });

        return counter.count.get() <= maxRequests;
    }

    /**
     * Lấy số lượng request còn lại cho key trong cửa sổ hiện tại.
     */
    public int getRemainingRequests(String key, int maxRequests, Duration window) {
        WindowCounter counter = buckets.get(key);
        if (counter == null || (System.currentTimeMillis() - counter.windowStart >= window.toMillis())) {
            return maxRequests;
        }
        return Math.max(0, maxRequests - counter.count.get());
    }

    /**
     * Xóa quota cho 1 key (hỗ trợ kiểm thử hoặc mở khóa thủ công)
     */
    public void reset(String key) {
        buckets.remove(key);
    }

    /**
     * Xóa toàn bộ bucket cache
     */
    public void clear() {
        buckets.clear();
    }

    /**
     * Xóa các mục đã hết hạn quá 2 phút để bảo vệ dung lượng bộ nhớ
     */
    private void cleanup(long now) {
        buckets.entrySet().removeIf(entry -> now - entry.getValue().windowStart > 120_000);
    }

    private static class WindowCounter {
        final long windowStart;
        final AtomicInteger count;

        WindowCounter(long windowStart, int initialCount) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(initialCount);
        }
    }
}
