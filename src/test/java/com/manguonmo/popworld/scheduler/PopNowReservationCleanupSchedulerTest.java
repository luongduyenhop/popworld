package com.manguonmo.popworld.scheduler;

import com.manguonmo.popworld.service.PopNowService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PopNowReservationCleanupSchedulerTest {

    @Mock
    private PopNowService popNowService;

    @InjectMocks
    private PopNowReservationCleanupScheduler scheduler;

    @Test
    @DisplayName("cleanupExpiredReservations: Gọi releaseExpiredReservations thành công")
    void cleanupExpiredReservations_Success() {
        when(popNowService.releaseExpiredReservations()).thenReturn(3);

        scheduler.cleanupExpiredReservations();

        verify(popNowService).releaseExpiredReservations();
    }

    @Test
    @DisplayName("cleanupExpiredReservations: Khi service ném ngoại lệ -> Không làm crash scheduler")
    void cleanupExpiredReservations_Exception_HandledGracefully() {
        when(popNowService.releaseExpiredReservations()).thenThrow(new RuntimeException("DB Timeout"));

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> scheduler.cleanupExpiredReservations());
        verify(popNowService).releaseExpiredReservations();
    }
}
