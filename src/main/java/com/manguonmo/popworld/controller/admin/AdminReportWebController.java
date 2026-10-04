package com.manguonmo.popworld.controller.admin;

import com.manguonmo.popworld.dto.response.RevenueReportResponse;
import com.manguonmo.popworld.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminReportWebController {

    private final ReportService reportService;

    @GetMapping
    public String viewRevenueReports(
            @RequestParam(required = false, defaultValue = "30DAYS") String timeRange,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

        RevenueReportResponse report = reportService.getRevenueReport(timeRange, startDate, endDate);

        model.addAttribute("report", report);
        model.addAttribute("activeItem", "reports");

        return "admin/reports";
    }
}
