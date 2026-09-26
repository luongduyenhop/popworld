package com.manguonmo.popworld.scheduler;

import com.manguonmo.popworld.service.PopNowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PopNowReservationCleanupScheduler {

    private final PopNowService popNowService;

    /**
     * Tự động quét và giải phóng các phiếu giữ hộp POP NOW đã hết hạn (TTL 5 phút theo benchmark POP MART) mỗi 60 giây.
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
    public void cleanupExpiredReservations() {
        try {
            int releasedCount = popNowService.releaseExpiredReservations();
            if (releasedCount > 0) {
                log.info("PopNowReservationCleanupScheduler: Đã tự động thu hồi {} phiếu giữ hộp quá hạn.", releasedCount);
            }
        } catch (Exception e) {
            log.error("Lỗi trong quá trình quét dọn phiếu giữ hộp POP NOW hết hạn: {}", e.getMessage(), e);
        }
    }
}
