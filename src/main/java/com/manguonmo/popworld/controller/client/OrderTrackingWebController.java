package com.manguonmo.popworld.controller.client;

import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.OrderItem;
import com.manguonmo.popworld.entity.OrderTimeline;
import com.manguonmo.popworld.service.CategoryService;
import com.manguonmo.popworld.service.CharacterIpService;
import com.manguonmo.popworld.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class OrderTrackingWebController {

    private final OrderService orderService;
    private final CategoryService categoryService;
    private final CharacterIpService characterIpService;

    private void addCommonAttributes(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("characterIps", characterIpService.getAllCharacterIps());
    }

    @GetMapping("/order-tracking")
    public String showTrackingPage(@RequestParam(required = false) String orderCode,
                                   @RequestParam(required = false) String phone,
                                   Model model) {
        addCommonAttributes(model);

        if (orderCode != null && !orderCode.trim().isEmpty() && phone != null && !phone.trim().isEmpty()) {
            return processLookup(orderCode.trim(), phone.trim(), model);
        }

        model.addAttribute("orderCode", orderCode);
        model.addAttribute("phone", phone);
        return "order-tracking";
    }

    @PostMapping("/order-tracking")
    public String lookupOrder(@RequestParam("orderCode") String orderCode,
                              @RequestParam("phone") String phone,
                              Model model) {
        addCommonAttributes(model);
        return processLookup(orderCode != null ? orderCode.trim() : "", phone != null ? phone.trim() : "", model);
    }

    private String processLookup(String orderCode, String phone, Model model) {
        model.addAttribute("orderCode", orderCode);
        model.addAttribute("phone", phone);

        if (orderCode.isEmpty() || phone.isEmpty()) {
            model.addAttribute("errorMessage", "Vui lòng nhập đầy đủ Mã đơn hàng và Số điện thoại nhận hàng!");
            return "order-tracking";
        }

        try {
            Order order = orderService.getOrderByCode(orderCode);
            if (order == null) {
                model.addAttribute("errorMessage", "Không tìm thấy đơn hàng với mã \"" + orderCode + "\". Vui lòng kiểm tra lại!");
                return "order-tracking";
            }

            // Kiểm tra số điện thoại (cho phép kiểm tra với shippingPhone hoặc user phone)
            String orderPhone = order.getRecipientPhone() != null ? order.getRecipientPhone().trim() : "";
            String userPhone = order.getUser() != null && order.getUser().getPhone() != null ? order.getUser().getPhone().trim() : "";

            boolean phoneMatches = orderPhone.equals(phone) || userPhone.equals(phone);
            if (!phoneMatches) {
                model.addAttribute("errorMessage", "Số điện thoại \"" + phone + "\" không khớp với số nhận hàng của đơn này!");
                return "order-tracking";
            }

            List<OrderItem> items = orderService.getOrderItems(order.getId());
            List<com.manguonmo.popworld.dto.response.OrderTimelineResponse> timelines = orderService.getOrderTimelines(order.getOrderCode());

            model.addAttribute("order", order);
            model.addAttribute("items", items);
            model.addAttribute("timelines", timelines);
            model.addAttribute("searchSuccess", true);

        } catch (Exception e) {
            log.error("Lỗi khi tra cứu đơn hàng {}: {}", orderCode, e.getMessage(), e);
            model.addAttribute("errorMessage", "Đã xảy ra lỗi khi tra cứu đơn hàng: " + e.getMessage());
        }

        return "order-tracking";
    }
}
