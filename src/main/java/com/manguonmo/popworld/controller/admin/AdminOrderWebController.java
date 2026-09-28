package com.manguonmo.popworld.controller.admin;

import com.manguonmo.popworld.dto.response.OrderResponse;
import com.manguonmo.popworld.dto.response.OrderStatusCountResponse;
import com.manguonmo.popworld.dto.response.OrderTimelineResponse;
import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.OrderItem;
import com.manguonmo.popworld.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

import com.manguonmo.popworld.dto.response.RefundResponse;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.service.RefundService;
import java.math.BigDecimal;
import java.security.Principal;

@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderWebController {

    private final OrderService orderService;
    private final RefundService refundService;

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
            model.addAttribute("countPacked", counts.getPacked());
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
     * Xem chi tiết một đơn hàng cụ thể kèm dòng thời gian (Order Timeline)
     */
    @GetMapping("/{orderCode}")
    public String viewOrderDetail(@PathVariable String orderCode, Model model, RedirectAttributes redirectAttributes) {
        Order order = orderService.getOrderByCode(orderCode);
        if (order == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy đơn hàng: " + orderCode);
            return "redirect:/admin/orders";
        }

        List<OrderItem> orderItems = orderService.getOrderItems(order.getId());
        List<RefundResponse> refunds = refundService.getRefundsByOrderCode(orderCode);
        BigDecimal totalRefunded = refundService.getTotalRefundedAmount(orderCode);
        BigDecimal remainingRefundable = refundService.getRemainingRefundableAmount(orderCode);
        boolean isRefundEligible = refundService.isEligibleForRefund(orderCode);
        List<OrderTimelineResponse> timelines = orderService.getOrderTimelines(orderCode);

        model.addAttribute("order", order);
        model.addAttribute("orderItems", orderItems);
        model.addAttribute("refunds", refunds);
        model.addAttribute("totalRefunded", totalRefunded);
        model.addAttribute("remainingRefundable", remainingRefundable);
        model.addAttribute("isRefundEligible", isRefundEligible);
        model.addAttribute("timelines", timelines);
        model.addAttribute("activeNav", "orders");

        return "admin/order-detail";
    }

    /**
     * Chuyển trạng thái đơn hàng sang ĐÃ ĐÓNG GÓI (PROCESSING -> PACKED)
     */
    @PostMapping("/{orderCode}/pack")
    public String packOrder(@PathVariable String orderCode,
                            @RequestParam(value = "note", required = false) String note,
                            @RequestParam(value = "redirect", required = false, defaultValue = "list") String redirect,
                            Principal principal,
                            RedirectAttributes redirectAttributes) {
        try {
            String adminUsername = principal != null ? principal.getName() : "Quản trị viên";
            orderService.packOrder(orderCode, adminUsername, note);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xác nhận đóng gói và niêm phong đơn hàng " + orderCode + "!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi đóng gói: " + e.getMessage());
        }

        if ("detail".equalsIgnoreCase(redirect)) {
            return "redirect:/admin/orders/" + orderCode;
        }
        return "redirect:/admin/orders";
    }

    public String shipOrder(String orderCode, String redirect, RedirectAttributes redirectAttributes) {
        return shipOrder(orderCode, null, null, null, redirect, null, redirectAttributes);
    }

    /**
     * Chuyển trạng thái đơn hàng sang ĐANG GIAO HÀNG (PACKED / PROCESSING -> SHIPPING) kèm thông tin vận đơn
     */
    @PostMapping("/{orderCode}/ship")
    public String shipOrder(@PathVariable String orderCode,
                            @RequestParam(value = "carrier", required = false, defaultValue = "Giao Hàng Nhanh (GHN)") String carrier,
                            @RequestParam(value = "trackingNumber", required = false) String trackingNumber,
                            @RequestParam(value = "note", required = false) String note,
                            @RequestParam(value = "redirect", required = false, defaultValue = "list") String redirect,
                            Principal principal,
                            RedirectAttributes redirectAttributes) {
        try {
            if (principal != null || (trackingNumber != null && !trackingNumber.isBlank()) || (carrier != null && !carrier.isBlank() && !"Giao Hàng Nhanh (GHN)".equals(carrier))) {
                String adminUsername = principal != null ? principal.getName() : "Quản trị viên";
                orderService.shipOrder(orderCode, carrier, trackingNumber, adminUsername, note);
            } else {
                orderService.shipOrder(orderCode);
            }
            redirectAttributes.addFlashAttribute("successMessage", "Đã bàn giao đơn hàng " + orderCode + " cho đơn vị vận chuyển!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }

        if ("detail".equalsIgnoreCase(redirect)) {
            return "redirect:/admin/orders/" + orderCode;
        }
        return "redirect:/admin/orders";
    }

    public String completeOrder(String orderCode, String redirect, RedirectAttributes redirectAttributes) {
        return completeOrder(orderCode, null, redirect, null, redirectAttributes);
    }

    /**
     * Chuyển trạng thái đơn hàng sang HOÀN TẤT (SHIPPING -> DELIVERED)
     */
    @PostMapping("/{orderCode}/complete")
    public String completeOrder(@PathVariable String orderCode,
                                @RequestParam(value = "note", required = false) String note,
                                @RequestParam(value = "redirect", required = false, defaultValue = "list") String redirect,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            if (principal != null || (note != null && !note.isBlank())) {
                String adminUsername = principal != null ? principal.getName() : "Quản trị viên";
                orderService.completeOrder(orderCode, adminUsername, note);
            } else {
                orderService.completeOrder(orderCode);
            }
            redirectAttributes.addFlashAttribute("successMessage", "Đã xác nhận giao thành công đơn hàng " + orderCode + "!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }

        if ("detail".equalsIgnoreCase(redirect)) {
            return "redirect:/admin/orders/" + orderCode;
        }
        return "redirect:/admin/orders";
    }

    public String cancelOrder(String orderCode, String reason, String redirect, RedirectAttributes redirectAttributes) {
        return cancelOrder(orderCode, reason, redirect, null, redirectAttributes);
    }

    /**
     * Quản trị viên hủy đơn hàng (TO_PAY, PROCESSING hoặc PACKED -> CANCELLED)
     */
    @PostMapping("/{orderCode}/cancel")
    public String cancelOrder(@PathVariable String orderCode,
                              @RequestParam(required = false, defaultValue = "") String reason,
                              @RequestParam(value = "redirect", required = false, defaultValue = "list") String redirect,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        try {
            if (principal != null) {
                orderService.adminCancelOrder(orderCode, reason, principal.getName());
            } else {
                orderService.adminCancelOrder(orderCode, reason);
            }
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đơn hàng " + orderCode + " và hoàn kho tồn kho thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hủy đơn thất bại: " + e.getMessage());
        }

        if ("detail".equalsIgnoreCase(redirect)) {
            return "redirect:/admin/orders/" + orderCode;
        }
        return "redirect:/admin/orders";
    }

    /**
     * Quản trị viên thực hiện hoàn tiền thủ công (Manual Refund MVP)
     */
    @PostMapping("/{orderCode}/refund")
    public String refundOrder(@PathVariable String orderCode,
                              @RequestParam("amount") BigDecimal amount,
                              @RequestParam(value = "reason", required = false) String reason,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng đăng nhập với tài khoản Quản trị viên để thực hiện hoàn tiền.");
            return "redirect:/admin/orders/" + orderCode;
        }

        try {
            String adminUsername = principal.getName();
            refundService.processManualRefund(orderCode, amount, reason, adminUsername);
            redirectAttributes.addFlashAttribute("successMessage", "Đã ghi nhận hoàn tiền thành công cho đơn hàng " + orderCode + "!");
        } catch (BadRequestException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xử lý hoàn tiền: " + e.getMessage());
        }

        return "redirect:/admin/orders/" + orderCode;
    }
}
