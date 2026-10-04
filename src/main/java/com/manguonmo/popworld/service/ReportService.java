package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.response.RevenueReportResponse;

import java.time.LocalDate;

public interface ReportService {

    RevenueReportResponse getRevenueReport(String timeRange, LocalDate customStart, LocalDate customEnd);
}
