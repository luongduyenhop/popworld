package com.manguonmo.popworld.controller.admin;

import com.manguonmo.popworld.dto.response.OrderResponse;
import com.manguonmo.popworld.dto.response.OrderStatusCountResponse;
import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.OrderItem;
import com.manguonmo.popworld.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderWebController {

    private final OrderService orderService;

    /**
     * Danh sách đơn hàng trong trang quản trị với bộ lọc trạng thái và tìm kiếm
     */
    @GetMapping
    public String listOrders(@RequestParam(value = "status", required = false, defaultValue = "ALL") String status,
                             @RequestParam(value = "keyword", required = false) String keyword,
                             Model model) {
        List<OrderResponse> orders;

        if (keyword != null && !keyword.trim().isEmpty()) {
            orders = orderService.searchOrders(keyword.trim());
        } else {
            orders = orderService.getAllOrders(status);
        }

        // Đếm số lượng đơn hàng theo từng trạng thái bằng DTO từ OrderService
        OrderStatusCountResponse counts = orderService.getOrderStatusCounts();

        model.addAttribute("orders", orders);
        model.addAttribute("currentStatus", status.toUpperCase());
        model.addAttribute("keyword", keyword);
        model.addAttribute("activeNav", "orders");

        if (counts != null) {
            model.addAttribute("countAll", counts.getAll());
            model.addAttribute("countToPay", counts.getToPay());
            model.addAttribute("countProcessing", counts.getProcessing());
            model.addAttribute("countShipping", counts.getShipping());
            model.addAttribute("countDelivered", counts.getDelivered());
            model.addAttribute("countShipped", counts.getShipped());
            model.addAttribute("countCompleted", counts.getCompleted());
            model.addAttribute("countCancelled", counts.getCancelled());
            model.addAttribute("countExpired", counts.getExpired());
        }

        return "admin/orders";

    }

    /**
     * Xem chi tiết một đơn hàng cụ thể
     */
    @GetMapping("/{orderCode}")
    public String viewOrderDetail(@PathVariable String orderCode, Model model, RedirectAttributes redirectAttributes) {
        Order order = orderService.getOrderByCode(orderCode);
        if (order == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy đơn hàng: " + orderCode);
            return "redirect:/admin/orders";
        }

        List<OrderItem> orderItems = orderService.getOrderItems(order.getId());

        model.addAttribute("order", order);
        model.addAttribute("orderItems", orderItems);
        model.addAttribute("activeNav", "orders");

        return "admin/order-detail";
    }

    /**
     * Chuyển trạng thái đơn hàng sang ĐANG GIAO HÀNG (PROCESSING -> SHIPPING)
     */
    @PostMapping("/{orderCode}/ship")
    public String shipOrder(@PathVariable String orderCode,
                            @RequestParam(value = "redirect", required = false, defaultValue = "list") String redirect,
                            RedirectAttributes redirectAttributes) {
        try {
            orderService.shipOrder(orderCode);
            redirectAttributes.addFlashAttribute("successMessage", "Đã bàn giao đơn hàng " + orderCode + " cho đơn vị vận chuyển!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }

        if ("detail".equalsIgnoreCase(redirect)) {
            return "redirect:/admin/orders/" + orderCode;
        }
        return "redirect:/admin/orders";
    }

    /**
     * Chuyển trạng thái đơn hàng sang HOÀN TẤT (SHIPPING -> DELIVERED)
     */
    @PostMapping("/{orderCode}/complete")
    public String completeOrder(@PathVariable String orderCode,
                              @RequestParam(value = "redirect", required = false, defaultValue = "list") String redirect,
                              RedirectAttributes redirectAttributes) {
        try {
            orderService.completeOrder(orderCode);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xác nhận giao thành công đơn hàng " + orderCode + "!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }

        if ("detail".equalsIgnoreCase(redirect)) {
            return "redirect:/admin/orders/" + orderCode;
        }
        return "redirect:/admin/orders";
    }

    /**
     * Quản trị viên hủy đơn hàng (TO_PAY hoặc PROCESSING -> CANCELLED)
     */
    @PostMapping("/{orderCode}/cancel")
    public String cancelOrder(@PathVariable String orderCode,
                              @RequestParam(required = false, defaultValue = "") String reason,
                              @RequestParam(value = "redirect", required = false, defaultValue = "list") String redirect,
                              RedirectAttributes redirectAttributes) {
        try {
            orderService.adminCancelOrder(orderCode, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đơn hàng " + orderCode + " và hoàn kho tồn kho thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hủy đơn thất bại: " + e.getMessage());
        }

        if ("detail".equalsIgnoreCase(redirect)) {
            return "redirect:/admin/orders/" + orderCode;
        }
        return "redirect:/admin/orders";
    }
}
