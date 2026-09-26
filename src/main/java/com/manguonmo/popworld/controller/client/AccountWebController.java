package com.manguonmo.popworld.controller.client;

import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.OrderItem;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.repository.CouponRepository;
import com.manguonmo.popworld.service.CategoryService;
import com.manguonmo.popworld.service.CharacterIpService;
import com.manguonmo.popworld.service.OrderService;
import com.manguonmo.popworld.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller quản lý Trung tâm Tài khoản Khách hàng (Account Hub)
 * Canonical URL: /account
 * Alias redirect: /profile -> /account
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AccountWebController {

    private final UserService userService;
    private final OrderService orderService;
    private final CouponRepository couponRepository;
    private final CategoryService categoryService;
    private final CharacterIpService characterIpService;

    private void addCommonAttributes(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("characterIps", characterIpService.getAllCharacterIps());
    }

    /**
     * Canonical Account Hub
     * Hỗ trợ alias /orders tạm thời để duy trì tương thích cho đến khi tách riêng trang đơn hàng
     */
    @GetMapping({"/account", "/orders"})
    public String showAccountHub(Model model, Principal principal) {
        addCommonAttributes(model);

        if (principal == null) {
            return "redirect:/login";
        }

        User user = userService.getUserByEmail(principal.getName());
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            return "redirect:/login";
        }

        List<Order> orders = orderService.getOrdersByUser(user.getId());

        // Lấy danh sách sản phẩm cho từng đơn hàng để hiển thị ảnh thumbnail và thông tin chi tiết
        Map<Long, List<OrderItem>> orderItemsMap = orders.stream()
                .collect(Collectors.toMap(Order::getId, order -> orderService.getOrderItems(order.getId()), (a, b) -> a));

        model.addAttribute("user", user);
        model.addAttribute("orders", orders);
        model.addAttribute("orderItemsMap", orderItemsMap);
        model.addAttribute("couponsCount", couponRepository.countByActiveTrue());

        return "my-orders";
    }

    /**
     * Alias redirect từ /profile về /account canonical
     */
    @GetMapping("/profile")
    public String profileRedirect() {
        return "redirect:/account";
    }
}
