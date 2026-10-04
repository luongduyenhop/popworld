package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.response.RevenueReportResponse;
import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.Refund;
import com.manguonmo.popworld.repository.OrderItemRepository;
import com.manguonmo.popworld.repository.OrderRepository;
import com.manguonmo.popworld.repository.RefundRepository;
import com.manguonmo.popworld.service.impl.ReportServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    @Test
    @DisplayName("Tính toán chính xác doanh thu gộp, hoàn tiền, doanh thu thuần và AOV")
    void getRevenueReport_calculatesFinancialMetrics() {
        LocalDateTime now = LocalDateTime.now();

        Order o1 = Order.builder()
                .id(1L)
                .totalAmount(new BigDecimal("1000000"))
                .status("DELIVERED")
                .paymentMethod("COD")
                .build();
        o1.setCreatedAt(now.minusDays(2));

        Order o2 = Order.builder()
                .id(2L)
                .totalAmount(new BigDecimal("500000"))
                .status("PROCESSING")
                .paymentMethod("BANKING")
                .build();
        o2.setCreatedAt(now.minusDays(1));

        Order oCancelled = Order.builder()
                .id(3L)
                .totalAmount(new BigDecimal("300000"))
                .status("CANCELLED")
                .paymentMethod("COD")
                .build();
        oCancelled.setCreatedAt(now);

        Refund r1 = Refund.builder()
                .id(1L)
                .amount(new BigDecimal("200000"))
                .status("COMPLETED")
                .processedAt(now.minusDays(1))
                .build();
        r1.setCreatedAt(now.minusDays(1));

        when(orderRepository.findByCreatedAtBetween(any(), any())).thenReturn(List.of(o1, o2, oCancelled));
        when(refundRepository.findByProcessedAtBetweenAndStatus(any(), any(), eq("COMPLETED"))).thenReturn(List.of(r1));
        when(orderItemRepository.findByOrderIdIn(any())).thenReturn(Collections.emptyList());

        RevenueReportResponse report = reportService.getRevenueReport("7DAYS", null, null);

        // Gross: 1,000,000 + 500,000 = 1,500,000 (oCancelled excluded)
        assertEquals(new BigDecimal("1500000"), report.getGrossRevenue());
        // Refunded: 200,000
        assertEquals(new BigDecimal("200000"), report.getTotalRefunded());
        // Net: 1,500,000 - 200,000 = 1,300,000
        assertEquals(new BigDecimal("1300000"), report.getNetRevenue());
        // Successful count: 2
        assertEquals(2, report.getSuccessfulOrderCount());
        // AOV: 1,500,000 / 2 = 750,000
        assertEquals(new BigDecimal("750000"), report.getAverageOrderValue());

        // Payment split:
        assertEquals(new BigDecimal("1000000"), report.getCodRevenue());
        assertEquals(1, report.getCodOrderCount());
        assertEquals(new BigDecimal("500000"), report.getBankingRevenue());
        assertEquals(1, report.getBankingOrderCount());
    }

    @Test
    @DisplayName("Hỗ trợ lọc khoảng ngày tùy chọn hợp lệ")
    void getRevenueReport_customRange_handlesDates() {
        LocalDate start = LocalDate.now().minusDays(10);
        LocalDate end = LocalDate.now().minusDays(2);

        when(orderRepository.findByCreatedAtBetween(any(), any())).thenReturn(Collections.emptyList());
        when(refundRepository.findByProcessedAtBetweenAndStatus(any(), any(), eq("COMPLETED"))).thenReturn(Collections.emptyList());

        RevenueReportResponse report = reportService.getRevenueReport("CUSTOM", start, end);

        assertEquals("CUSTOM", report.getTimeRange());
        assertEquals(start, report.getStartDate());
        assertEquals(end, report.getEndDate());
        assertEquals(BigDecimal.ZERO, report.getGrossRevenue());
        assertEquals(BigDecimal.ZERO, report.getNetRevenue());
        assertEquals(0, report.getSuccessfulOrderCount());
    }
}
