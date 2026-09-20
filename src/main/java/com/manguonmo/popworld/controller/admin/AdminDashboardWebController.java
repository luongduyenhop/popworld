package com.manguonmo.popworld.controller.admin;

import com.manguonmo.popworld.entity.CharacterIp;
import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.repository.CategoryRepository;
import com.manguonmo.popworld.repository.CharacterIpRepository;
import com.manguonmo.popworld.repository.OrderRepository;
import com.manguonmo.popworld.repository.ProductRepository;
import com.manguonmo.popworld.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardWebController {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CharacterIpRepository characterIpRepository;
    private final CategoryRepository categoryRepository;

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        // 1. Chỉ số KPI tài chính & vận hành
        BigDecimal totalRevenue = orderRepository.calculateTotalRevenue();
        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countByStatus("PROCESSING");
        long toPayOrders = orderRepository.countByStatus("TO_PAY");
        long shippedOrders = orderRepository.countByStatus("SHIPPED");
        long completedOrders = orderRepository.countByStatus("COMPLETED");
        long cancelledOrders = orderRepository.countByStatus("CANCELLED");

        // 2. Chỉ số kho hàng & khách hàng
        long totalProducts = productRepository.count();
        long activeProducts = productRepository.countByActiveTrue();
        long lowStockProducts = productRepository.countByStockQuantityLessThanEqual(10);
        long totalCustomers = userRepository.count();

        // 3. Đơn hàng mới nhất cần theo dõi
        List<Order> recentOrders = orderRepository.findTop8ByOrderByCreatedAtDesc();

        // 4. Danh sách các nhân vật IP nổi tiếng
        List<CharacterIp> characterIps = characterIpRepository.findAll();

        model.addAttribute("activeNav", "dashboard");
        model.addAttribute("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("toPayOrders", toPayOrders);
        model.addAttribute("shippedOrders", shippedOrders);
        model.addAttribute("completedOrders", completedOrders);
        model.addAttribute("cancelledOrders", cancelledOrders);

        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("activeProducts", activeProducts);
        model.addAttribute("lowStockProducts", lowStockProducts);
        model.addAttribute("totalCustomers", totalCustomers);

        model.addAttribute("recentOrders", recentOrders);
        model.addAttribute("characterIps", characterIps);

        return "admin/dashboard";
    }
}
