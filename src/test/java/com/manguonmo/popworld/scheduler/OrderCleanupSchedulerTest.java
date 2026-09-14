package com.manguonmo.popworld.scheduler;

import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.OrderItem;
import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.repository.OrderItemRepository;
import com.manguonmo.popworld.repository.OrderRepository;
import com.manguonmo.popworld.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit Test kiểm thử cơ chế tự động hủy đơn và hoàn trả tồn kho của OrderCleanupScheduler
 */
@ExtendWith(MockitoExtension.class)
class OrderCleanupSchedulerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderCleanupScheduler scheduler;

    @Test
    @DisplayName("Dọn dẹp: Hủy đơn quá 15 phút và hoàn trả tồn kho đúng quy cách SINGLE_BOX vs WHOLE_SET")
    void cleanupExpiredOrders_Success_RestoresStockCorrectly() {
        // --- 1. ARRANGE ---
        Order expiredOrder = Order.builder()
                .id(10L)
                .orderCode("PW-1726000000010")
                .status("TO_PAY")
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .build();

        Product singleProduct = Product.builder().id(101L).name("Hirono Blind Box").build();
        Product setProduct = Product.builder().id(102L).name("Skullpanda Whole Set").build();

        OrderItem itemSingle = OrderItem.builder()
                .id(1L)
                .product(singleProduct)
                .purchaseType("SINGLE_BOX")
                .quantity(2) // 2 hộp lẻ
                .build();

        OrderItem itemSet = OrderItem.builder()
                .id(2L)
                .product(setProduct)
                .purchaseType("WHOLE_SET")
                .quantity(1) // 1 bộ nguyên thùng = 12 hộp lẻ
                .build();

        when(orderRepository.findByExpiresAtBeforeAndStatus(any(LocalDateTime.class), eq("TO_PAY")))
                .thenReturn(List.of(expiredOrder));

        when(orderItemRepository.findByOrderId(10L))
                .thenReturn(List.of(itemSingle, itemSet));

        // --- 2. ACT ---
        scheduler.cleanupExpiredOrders();

        // --- 3. ASSERT ---
        // Đơn hàng phải chuyển sang CANCELLED
        assertEquals("CANCELLED", expiredOrder.getStatus());

        // Kiểm tra hoàn kho cho SINGLE_BOX: 2 hộp -> gọi addStock(101L, 2)
        verify(productRepository, times(1)).addStock(101L, 2);

        // Kiểm tra hoàn kho cho WHOLE_SET: 1 bộ -> gọi addStock(102L, 12)
        verify(productRepository, times(1)).addStock(102L, 12);

        // Đã lưu lại danh sách đơn bị hủy
        verify(orderRepository, times(1)).saveAll(List.of(expiredOrder));
    }

    @Test
    @DisplayName("Dọn dẹp: Khi không có đơn quá hạn thì không làm gì cả")
    void cleanupExpiredOrders_NoExpiredOrders_DoesNothing() {
        when(orderRepository.findByExpiresAtBeforeAndStatus(any(LocalDateTime.class), eq("TO_PAY")))
                .thenReturn(Collections.emptyList());

        scheduler.cleanupExpiredOrders();

        verify(orderItemRepository, never()).findByOrderId(anyLong());
        verify(productRepository, never()).addStock(anyLong(), anyInt());
        verify(orderRepository, never()).saveAll(anyList());
    }
}
