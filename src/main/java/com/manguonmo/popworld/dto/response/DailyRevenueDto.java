package com.manguonmo.popworld.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class DailyRevenueDto {
    private LocalDate date;
    private long orderCount;
    private BigDecimal grossRevenue;
    private BigDecimal refundedAmount;
    private BigDecimal netRevenue;
}
