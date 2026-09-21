package com.manguonmo.popworld.service.impl;

import com.manguonmo.popworld.dto.response.DashboardStatsResponse;
import com.manguonmo.popworld.entity.CharacterIp;
import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.repository.CharacterIpRepository;
import com.manguonmo.popworld.repository.OrderRepository;
import com.manguonmo.popworld.repository.ProductRepository;
import com.manguonmo.popworld.repository.UserRepository;
import com.manguonmo.popworld.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CharacterIpRepository characterIpRepository;

    @Override
    public DashboardStatsResponse getDashboardStats() {
        BigDecimal totalRevenue = orderRepository.calculateTotalRevenue();
        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countByStatus("PROCESSING");
        long toPayOrders = orderRepository.countByStatus("TO_PAY");
        long shippedOrders = orderRepository.countByStatus("SHIPPED");
        long completedOrders = orderRepository.countByStatus("COMPLETED");
        long cancelledOrders = orderRepository.countByStatus("CANCELLED");

        long totalProducts = productRepository.count();
        long activeProducts = productRepository.countByActiveTrue();
        long lowStockProducts = productRepository.countByStockQuantityLessThanEqual(10);
        long totalCustomers = userRepository.count();

        List<Order> recentOrders = orderRepository.findTop8ByOrderByCreatedAtDesc();
        List<CharacterIp> characterIps = characterIpRepository.findAll();

        return DashboardStatsResponse.builder()
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .toPayOrders(toPayOrders)
                .shippedOrders(shippedOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .lowStockProducts(lowStockProducts)
                .totalCustomers(totalCustomers)
                .recentOrders(recentOrders)
                .characterIps(characterIps)
                .build();
    }
}
