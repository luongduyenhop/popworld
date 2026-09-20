package com.manguonmo.popworld.controller.admin;

import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.OrderItem;
import com.manguonmo.popworld.repository.OrderRepository;
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
    private final OrderRepository orderRepository;

    /**
     * Danh sách đơn hàng trong trang quản trị với bộ lọc trạng thái và tìm kiếm
     */
    @GetMapping
    public String listOrders(@RequestParam(value = "status", required = false, defaultValue = "ALL") String status,
                             @RequestParam(value = "keyword", required = false) String keyword,
                             Model model) {
        List<Order> orders;

        if (keyword != null && !keyword.trim().isEmpty()) {
            orders = orderRepository.searchOrders(keyword.trim());
        } else {
            orders = orderService.getAllOrders(status);
        }

        // Đếm số lượng đơn hàng theo từng trạng thái để hiển thị badge trên các Tab
        long countAll = orderRepository.count();
        long countToPay = orderRepository.countByStatus("TO_PAY");
        long countProcessing = orderRepository.countByStatus("PROCESSING");
        long countShipped = orderRepository.countByStatus("SHIPPED");
        long countCompleted = orderRepository.countByStatus("COMPLETED");
        long countCancelled = orderRepository.countByStatus("CANCELLED");

        model.addAttribute("orders", orders);
        model.addAttribute("currentStatus", status.toUpperCase());
        model.addAttribute("keyword", keyword);
        model.addAttribute("activeNav", "orders");

        model.addAttribute("countAll", countAll);
        model.addAttribute("countToPay", countToPay);
        model.addAttribute("countProcessing", countProcessing);
        model.addAttribute("countShipped", countShipped);
        model.addAttribute("countCompleted", countCompleted);
        model.addAttribute("countCancelled", countCancelled);

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
     * Chuyển trạng thái đơn hàng sang ĐANG GIAO HÀNG (PROCESSING -> SHIPPED)
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
     * Chuyển trạng thái đơn hàng sang HOÀN TẤT (SHIPPED -> COMPLETED)
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
}
