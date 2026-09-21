package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.response.DashboardStatsResponse;
import com.manguonmo.popworld.entity.CharacterIp;
import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.repository.CharacterIpRepository;
import com.manguonmo.popworld.repository.OrderRepository;
import com.manguonmo.popworld.repository.ProductRepository;
import com.manguonmo.popworld.repository.UserRepository;
import com.manguonmo.popworld.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CharacterIpRepository characterIpRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @Test
    @DisplayName("getDashboardStats: Tổng hợp đầy đủ chỉ số KPI tài chính, đơn hàng, kho và khách hàng")
    void getDashboardStats_ShouldAggregateAllKpis() {
        when(orderRepository.calculateTotalRevenue()).thenReturn(new BigDecimal("15000000"));
        when(orderRepository.count()).thenReturn(120L);
        when(orderRepository.countByStatus("PROCESSING")).thenReturn(8L);
        when(orderRepository.countByStatus("TO_PAY")).thenReturn(5L);
        when(orderRepository.countByStatus("SHIPPED")).thenReturn(15L);
        when(orderRepository.countByStatus("COMPLETED")).thenReturn(90L);
        when(orderRepository.countByStatus("CANCELLED")).thenReturn(2L);

        when(productRepository.count()).thenReturn(60L);
        when(productRepository.countByActiveTrue()).thenReturn(50L);
        when(productRepository.countByStockQuantityLessThanEqual(10)).thenReturn(7L);

        when(userRepository.count()).thenReturn(200L);

        Order recentOrder = Order.builder().id(1L).orderCode("PW-RECENT").build();
        when(orderRepository.findTop8ByOrderByCreatedAtDesc()).thenReturn(List.of(recentOrder));

        CharacterIp ip = CharacterIp.builder().id(1L).name("Labubu").build();
        when(characterIpRepository.findAll()).thenReturn(List.of(ip));

        DashboardStatsResponse stats = dashboardService.getDashboardStats();

        assertNotNull(stats);
        assertEquals(new BigDecimal("15000000"), stats.getTotalRevenue());
        assertEquals(120L, stats.getTotalOrders());
        assertEquals(8L, stats.getPendingOrders());
        assertEquals(5L, stats.getToPayOrders());
        assertEquals(15L, stats.getShippedOrders());
        assertEquals(90L, stats.getCompletedOrders());
        assertEquals(2L, stats.getCancelledOrders());
        assertEquals(60L, stats.getTotalProducts());
        assertEquals(50L, stats.getActiveProducts());
        assertEquals(7L, stats.getLowStockProducts());
        assertEquals(200L, stats.getTotalCustomers());
        assertEquals(1, stats.getRecentOrders().size());
        assertEquals(1, stats.getCharacterIps().size());
    }
}
