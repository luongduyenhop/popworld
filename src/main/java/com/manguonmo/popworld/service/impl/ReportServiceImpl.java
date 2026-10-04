package com.manguonmo.popworld.service.impl;

import com.manguonmo.popworld.dto.response.DailyRevenueDto;
import com.manguonmo.popworld.dto.response.RevenueReportResponse;
import com.manguonmo.popworld.dto.response.TopProductRevenueDto;
import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.OrderItem;
import com.manguonmo.popworld.entity.Refund;
import com.manguonmo.popworld.repository.OrderItemRepository;
import com.manguonmo.popworld.repository.OrderRepository;
import com.manguonmo.popworld.repository.RefundRepository;
import com.manguonmo.popworld.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final OrderRepository orderRepository;
    private final RefundRepository refundRepository;
    private final OrderItemRepository orderItemRepository;

    private static final Set<String> SUCCESSFUL_ORDER_STATUSES = Set.of(
            "DELIVERED", "SHIPPING", "PROCESSING", "PACKED", "COMPLETED", "SHIPPED", "PAID"
    );

    @Override
    @Transactional(readOnly = true)
    public RevenueReportResponse getRevenueReport(String timeRange, LocalDate customStart, LocalDate customEnd) {
        String range = (timeRange != null && !timeRange.trim().isEmpty()) ? timeRange.trim().toUpperCase() : "30DAYS";

        LocalDate today = LocalDate.now();
        LocalDate startDate;
        LocalDate endDate = today;

        switch (range) {
            case "TODAY":
                startDate = today;
                break;
            case "7DAYS":
                startDate = today.minusDays(6);
                break;
            case "THIS_MONTH":
                startDate = today.withDayOfMonth(1);
                break;
            case "THIS_YEAR":
                startDate = today.withDayOfYear(1);
                break;
            case "CUSTOM":
                startDate = (customStart != null) ? customStart : today.minusDays(30);
                if (customEnd != null) {
                    endDate = customEnd;
                }
                if (startDate.isAfter(endDate)) {
                    LocalDate temp = startDate;
                    startDate = endDate;
                    endDate = temp;
                }
                break;
            case "30DAYS":
            default:
                range = "30DAYS";
                startDate = today.minusDays(29);
                break;
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        // Fetch Orders in range
        List<Order> ordersInRange = orderRepository.findByCreatedAtBetween(startDateTime, endDateTime);
        List<Order> successfulOrders = ordersInRange.stream()
                .filter(o -> SUCCESSFUL_ORDER_STATUSES.contains(o.getStatus()))
                .collect(Collectors.toList());

        // Fetch Refunds in range
        List<Refund> refundsInRange = refundRepository.findByProcessedAtBetweenAndStatus(startDateTime, endDateTime, "COMPLETED");

        // Compute Financial Totals
        BigDecimal grossRevenue = BigDecimal.ZERO;
        BigDecimal codRevenue = BigDecimal.ZERO;
        long codCount = 0;
        BigDecimal bankingRevenue = BigDecimal.ZERO;
        long bankingCount = 0;

        for (Order o : successfulOrders) {
            BigDecimal orderTotal = o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO;
            grossRevenue = grossRevenue.add(orderTotal);

            if ("COD".equalsIgnoreCase(o.getPaymentMethod())) {
                codRevenue = codRevenue.add(orderTotal);
                codCount++;
            } else {
                bankingRevenue = bankingRevenue.add(orderTotal);
                bankingCount++;
            }
        }

        BigDecimal totalRefunded = BigDecimal.ZERO;
        for (Refund r : refundsInRange) {
            if (r.getAmount() != null) {
                totalRefunded = totalRefunded.add(r.getAmount());
            }
        }

        BigDecimal netRevenue = grossRevenue.subtract(totalRefunded);
        if (netRevenue.compareTo(BigDecimal.ZERO) < 0) {
            netRevenue = BigDecimal.ZERO;
        }

        long successfulOrderCount = successfulOrders.size();
        long totalOrderCount = ordersInRange.size();

        BigDecimal aov = BigDecimal.ZERO;
        if (successfulOrderCount > 0) {
            aov = grossRevenue.divide(BigDecimal.valueOf(successfulOrderCount), 0, RoundingMode.HALF_UP);
        }

        double refundRate = 0.0;
        if (grossRevenue.compareTo(BigDecimal.ZERO) > 0) {
            refundRate = totalRefunded.multiply(BigDecimal.valueOf(100))
                    .divide(grossRevenue, 2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        // Daily breakdown map
        Map<LocalDate, DailyRevenueDto> dailyMap = new HashMap<>();

        // Initialize entries for range if reasonable (e.g. up to 60 days)
        long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        if (daysDiff <= 62) {
            LocalDate curr = startDate;
            while (!curr.isAfter(endDate)) {
                dailyMap.put(curr, DailyRevenueDto.builder()
                        .date(curr)
                        .orderCount(0)
                        .grossRevenue(BigDecimal.ZERO)
                        .refundedAmount(BigDecimal.ZERO)
                        .netRevenue(BigDecimal.ZERO)
                        .build());
                curr = curr.plusDays(1);
            }
        }

        for (Order o : successfulOrders) {
            LocalDate d = o.getCreatedAt().toLocalDate();
            DailyRevenueDto dto = dailyMap.computeIfAbsent(d, k -> DailyRevenueDto.builder()
                    .date(k)
                    .orderCount(0)
                    .grossRevenue(BigDecimal.ZERO)
                    .refundedAmount(BigDecimal.ZERO)
                    .netRevenue(BigDecimal.ZERO)
                    .build());
            dto.setOrderCount(dto.getOrderCount() + 1);
            BigDecimal amt = o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO;
            dto.setGrossRevenue(dto.getGrossRevenue().add(amt));
            dto.setNetRevenue(dto.getGrossRevenue().subtract(dto.getRefundedAmount()));
        }

        for (Refund r : refundsInRange) {
            LocalDate d = (r.getProcessedAt() != null) ? r.getProcessedAt().toLocalDate() : r.getCreatedAt().toLocalDate();
            DailyRevenueDto dto = dailyMap.computeIfAbsent(d, k -> DailyRevenueDto.builder()
                    .date(k)
                    .orderCount(0)
                    .grossRevenue(BigDecimal.ZERO)
                    .refundedAmount(BigDecimal.ZERO)
                    .netRevenue(BigDecimal.ZERO)
                    .build());
            BigDecimal amt = r.getAmount() != null ? r.getAmount() : BigDecimal.ZERO;
            dto.setRefundedAmount(dto.getRefundedAmount().add(amt));
            dto.setNetRevenue(dto.getGrossRevenue().subtract(dto.getRefundedAmount()));
        }

        List<DailyRevenueDto> dailyList = new ArrayList<>(dailyMap.values());
        dailyList.sort(Comparator.comparing(DailyRevenueDto::getDate).reversed());

        // Top Selling Products
        List<TopProductRevenueDto> topProducts = computeTopProducts(successfulOrders);

        double codPercent = 0.0;
        double bankingPercent = 0.0;
        if (grossRevenue.compareTo(BigDecimal.ZERO) > 0) {
            codPercent = codRevenue.multiply(BigDecimal.valueOf(100))
                    .divide(grossRevenue, 1, RoundingMode.HALF_UP)
                    .doubleValue();
            bankingPercent = bankingRevenue.multiply(BigDecimal.valueOf(100))
                    .divide(grossRevenue, 1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        return RevenueReportResponse.builder()
                .timeRange(range)
                .startDate(startDate)
                .endDate(endDate)
                .grossRevenue(grossRevenue)
                .totalRefunded(totalRefunded)
                .netRevenue(netRevenue)
                .successfulOrderCount(successfulOrderCount)
                .totalOrderCount(totalOrderCount)
                .averageOrderValue(aov)
                .refundRatePercent(refundRate)
                .codRevenue(codRevenue)
                .codOrderCount(codCount)
                .codRevenuePercent(codPercent)
                .bankingRevenue(bankingRevenue)
                .bankingOrderCount(bankingCount)
                .bankingRevenuePercent(bankingPercent)
                .topProducts(topProducts)
                .dailyBreakdown(dailyList)
                .build();
    }

    private List<TopProductRevenueDto> computeTopProducts(List<Order> orders) {
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
        List<OrderItem> items = orderItemRepository.findByOrderIdIn(orderIds);

        Map<Long, TopProductAccumulator> accMap = new HashMap<>();
        for (OrderItem item : items) {
            if (item.getProduct() == null) continue;
            Long pid = item.getProduct().getId();
            TopProductAccumulator acc = accMap.computeIfAbsent(pid, k -> new TopProductAccumulator(
                    item.getProduct().getId(),
                    item.getProduct().getName(),
                    item.getProduct().getMainImageUrl()
            ));
            int qty = item.getQuantity() != null ? item.getQuantity() : 1;
            BigDecimal price = item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO;
            acc.unitsSold += qty;
            acc.revenue = acc.revenue.add(price);
        }

        return accMap.values().stream()
                .sorted((a, b) -> b.revenue.compareTo(a.revenue))
                .limit(5)
                .map(acc -> TopProductRevenueDto.builder()
                        .productId(acc.productId)
                        .productName(acc.productName)
                        .imageUrl(acc.imageUrl)
                        .unitsSold(acc.unitsSold)
                        .revenue(acc.revenue)
                        .build())
                .collect(Collectors.toList());
    }

    private static class TopProductAccumulator {
        Long productId;
        String productName;
        String imageUrl;
        long unitsSold = 0;
        BigDecimal revenue = BigDecimal.ZERO;

        TopProductAccumulator(Long productId, String productName, String imageUrl) {
            this.productId = productId;
            this.productName = productName;
            this.imageUrl = imageUrl;
        }
    }
}
