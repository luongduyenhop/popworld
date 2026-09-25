package com.manguonmo.popworld.scheduler;

import com.manguonmo.popworld.entity.Coupon;
import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.OrderItem;
import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.repository.OrderItemRepository;
import com.manguonmo.popworld.repository.OrderRepository;
import com.manguonmo.popworld.repository.ProductRepository;
import com.manguonmo.popworld.service.CouponService;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit Test kiểm thử cơ chế tự động chuyển trạng thái EXPIRED và hoàn trả tồn kho của OrderCleanupScheduler
 */
@ExtendWith(MockitoExtension.class)
class OrderCleanupSchedulerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CouponService couponService;

    @InjectMocks
    private OrderCleanupScheduler scheduler;

    @Test
    @DisplayName("Dọn dẹp: Đơn quá 15 phút chuyển sang EXPIRED và hoàn trả tồn kho đúng quy cách SINGLE_BOX vs WHOLE_SET")
    void cleanupExpiredOrders_Success_RestoresStockAndSetsExpiredStatus() {
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
        // Đơn hàng phải chuyển sang EXPIRED (không phải CANCELLED)
        assertEquals("EXPIRED", expiredOrder.getStatus());
        assertTrue(expiredOrder.getNote().contains("hết hạn do quá hạn 15 phút"));

        // Kiểm tra hoàn kho cho SINGLE_BOX: 2 hộp -> gọi addStock(101L, 2)
        verify(productRepository, times(1)).addStock(101L, 2);

        // Kiểm tra hoàn kho cho WHOLE_SET: 1 bộ -> gọi addStock(102L, 12)
        verify(productRepository, times(1)).addStock(102L, 12);

        // Đã lưu lại danh sách đơn bị hết hạn
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

    @Test
    @DisplayName("Dọn dẹp: Đơn quá hạn có Coupon thì phải gọi releaseCoupon để hoàn lại lượt sử dụng")
    void cleanupExpiredOrders_WithCoupon_ReleasesCouponSuccessfully() {
        // --- 1. ARRANGE ---
        User user = User.builder().id(99L).build();
        Coupon coupon = Coupon.builder().id(50L).code("POP10").build();

        Order expiredOrder = Order.builder()
                .id(20L)
                .orderCode("PW-1726000000020")
                .status("TO_PAY")
                .user(user)
                .coupon(coupon)
                .expiresAt(LocalDateTime.now().minusMinutes(16))
                .build();

        when(orderRepository.findByExpiresAtBeforeAndStatus(any(LocalDateTime.class), eq("TO_PAY")))
                .thenReturn(List.of(expiredOrder));
        when(orderItemRepository.findByOrderId(20L))
                .thenReturn(Collections.emptyList());

        // --- 2. ACT ---
        scheduler.cleanupExpiredOrders();

        // --- 3. ASSERT ---
        assertEquals("EXPIRED", expiredOrder.getStatus());
        verify(couponService, times(1)).releaseCoupon(50L, 99L);
        verify(orderRepository, times(1)).saveAll(List.of(expiredOrder));
    }

    @Test
    @DisplayName("Dọn dẹp: Đơn quá hạn có Coupon nhưng user bị null thì releaseCoupon với userId = null an toàn")
    void cleanupExpiredOrders_WithCouponAndNullUser_ReleasesCouponSafely() {
        // --- 1. ARRANGE ---
        Coupon coupon = Coupon.builder().id(55L).code("FREESHIP").build();

        Order expiredOrder = Order.builder()
                .id(21L)
                .orderCode("PW-1726000000021")
                .status("TO_PAY")
                .user(null)
                .coupon(coupon)
                .expiresAt(LocalDateTime.now().minusMinutes(20))
                .build();

        when(orderRepository.findByExpiresAtBeforeAndStatus(any(LocalDateTime.class), eq("TO_PAY")))
                .thenReturn(List.of(expiredOrder));
        when(orderItemRepository.findByOrderId(21L))
                .thenReturn(Collections.emptyList());

        // --- 2. ACT ---
        scheduler.cleanupExpiredOrders();

        // --- 3. ASSERT ---
        assertEquals("EXPIRED", expiredOrder.getStatus());
        verify(couponService, times(1)).releaseCoupon(55L, null);
        verify(orderRepository, times(1)).saveAll(List.of(expiredOrder));
    }

    @Test
    @DisplayName("Idempotency: Đơn đã EXPIRED thì scheduler lần sau không quét lại, không hoàn kho lần 2")
    void cleanupExpiredOrders_Idempotency_DoesNotReExpireAlreadyExpiredOrders() {
        // Giả sử scheduler quét lại, DB chỉ trả về đơn TO_PAY (đơn EXPIRED không nằm trong kết quả query)
        when(orderRepository.findByExpiresAtBeforeAndStatus(any(LocalDateTime.class), eq("TO_PAY")))
                .thenReturn(Collections.emptyList());

        scheduler.cleanupExpiredOrders();

        // Không có thao tác hoàn kho hay cập nhật nào được thực thi
        verify(productRepository, never()).addStock(anyLong(), anyInt());
        verify(orderRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Scheduler Invariant: Scheduler chỉ tìm kiếm đơn TO_PAY, tuyệt đối không expire PROCESSING, SHIPPING, DELIVERED hay CANCELLED")
    void cleanupExpiredOrders_OnlyQueriesToPayStatus() {
        when(orderRepository.findByExpiresAtBeforeAndStatus(any(LocalDateTime.class), eq("TO_PAY")))
                .thenReturn(Collections.emptyList());

        scheduler.cleanupExpiredOrders();

        // Kiểm tra query chính xác tham số "TO_PAY"
        verify(orderRepository, times(1)).findByExpiresAtBeforeAndStatus(any(LocalDateTime.class), eq("TO_PAY"));
        verify(orderRepository, never()).findByExpiresAtBeforeAndStatus(any(LocalDateTime.class), eq("PROCESSING"));
        verify(orderRepository, never()).findByExpiresAtBeforeAndStatus(any(LocalDateTime.class), eq("SHIPPING"));
        verify(orderRepository, never()).findByExpiresAtBeforeAndStatus(any(LocalDateTime.class), eq("DELIVERED"));
        verify(orderRepository, never()).findByExpiresAtBeforeAndStatus(any(LocalDateTime.class), eq("CANCELLED"));
        verify(orderRepository, never()).findByExpiresAtBeforeAndStatus(any(LocalDateTime.class), eq("EXPIRED"));
    }
}
