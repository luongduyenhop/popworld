package com.manguonmo.popworld.dto.response;

import com.manguonmo.popworld.entity.CharacterIp;
import com.manguonmo.popworld.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private BigDecimal totalRevenue;
    private long totalOrders;
    private long pendingOrders;
    private long toPayOrders;
    private long shippedOrders;
    private long completedOrders;
    private long cancelledOrders;

    private long totalProducts;
    private long activeProducts;
    private long lowStockProducts;
    private long totalCustomers;

    private List<Order> recentOrders;
    private List<CharacterIp> characterIps;
}
