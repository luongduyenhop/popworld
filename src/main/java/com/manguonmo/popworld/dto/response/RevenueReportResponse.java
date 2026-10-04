package com.manguonmo.popworld.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class RevenueReportResponse {
    private String timeRange;
    private LocalDate startDate;
    private LocalDate endDate;

    // Core KPIs
    private BigDecimal grossRevenue;
    private BigDecimal totalRefunded;
    private BigDecimal netRevenue;
    private long successfulOrderCount;
    private long totalOrderCount;
    private BigDecimal averageOrderValue;
    private double refundRatePercent;

    // Payment methods split
    private BigDecimal codRevenue;
    private long codOrderCount;
    private double codRevenuePercent;
    private BigDecimal bankingRevenue;
    private long bankingOrderCount;
    private double bankingRevenuePercent;

    // Top Selling Products
    private List<TopProductRevenueDto> topProducts;

    // Daily breakdown
    private List<DailyRevenueDto> dailyBreakdown;
}
