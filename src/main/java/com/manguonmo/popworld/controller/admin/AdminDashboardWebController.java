package com.manguonmo.popworld.controller.admin;

import com.manguonmo.popworld.dto.response.DashboardStatsResponse;
import com.manguonmo.popworld.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardWebController {

    private final DashboardService dashboardService;

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        DashboardStatsResponse stats = dashboardService.getDashboardStats();

        model.addAttribute("activeNav", "dashboard");
        model.addAttribute("totalRevenue", stats.getTotalRevenue());
        model.addAttribute("totalOrders", stats.getTotalOrders());
        model.addAttribute("pendingOrders", stats.getPendingOrders());
        model.addAttribute("toPayOrders", stats.getToPayOrders());
        model.addAttribute("shippedOrders", stats.getShippedOrders());
        model.addAttribute("completedOrders", stats.getCompletedOrders());
        model.addAttribute("cancelledOrders", stats.getCancelledOrders());

        model.addAttribute("totalProducts", stats.getTotalProducts());
        model.addAttribute("activeProducts", stats.getActiveProducts());
        model.addAttribute("lowStockProducts", stats.getLowStockProducts());
        model.addAttribute("totalCustomers", stats.getTotalCustomers());

        model.addAttribute("recentOrders", stats.getRecentOrders());
        model.addAttribute("characterIps", stats.getCharacterIps());

        return "admin/dashboard";
    }
}
