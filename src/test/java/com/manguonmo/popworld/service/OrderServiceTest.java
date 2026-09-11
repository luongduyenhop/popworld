package com.manguonmo.popworld.service;

import com.manguonmo.popworld.entity.*;
import com.manguonmo.popworld.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho OrderService (sử dụng JUnit 5 và Mockito)
 * 
 * Mục đích:
 * - Kiểm thử độc lập tầng nghiệp vụ (Service Layer) mà không cần bật toàn bộ Spring Context.
 * - Giả lập (Mock) tất cả Repository phụ thuộc: OrderRepository, CartItemRepository,
 *   CouponRepository, OrderItemRepository, UserRepository.
 * - Tuân thủ mô hình Arrange - Act - Assert (AAA).
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User sampleUser;
    private Product sampleProductSingle;
    private Product sampleProductSet;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .fullName("Nguyen Van A")
                .email("nguyenvana@example.com")
                .phone("0987654321")
                .role("ROLE_CUSTOMER")
                .build();

        sampleProductSingle = Product.builder()
                .id(101L)
                .name("Labubu The Monsters Tasty Macarons")
                .singlePrice(new BigDecimal("200000"))
                .wholeSetPrice(new BigDecimal("1200000"))
                .build();

        sampleProductSet = Product.builder()
                .id(102L)
                .name("Skullpanda City of Night")
                .singlePrice(new BigDecimal("250000"))
                .wholeSetPrice(new BigDecimal("600000"))
                .build();
    }

    /**
     * Helper so sánh 2 số BigDecimal bỏ qua sự khác biệt về scale (ví dụ 60000 vs 60000.00)
     */
    private void assertBigDecimalEquals(String expected, BigDecimal actual) {
        assertNotNull(actual, "Giá trị BigDecimal không được null");
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "Kỳ vọng " + expected + " nhưng nhận được " + actual);
    }

    // =========================================================================
    // TEST CASE 1: Đơn hàng dưới 500k -> Có phí ship 30.000đ, không có coupon
    // =========================================================================
    @Test
    @DisplayName("Tạo đơn hàng thành công: Dưới 500k tính phí ship 30.000đ, không dùng coupon")
    void createOrder_Success_WithShippingFee() {
        // --- 1. ARRANGE (Chuẩn bị dữ liệu và mock) ---
        CartItem cartItem = CartItem.builder()
                .id(1L)
                .user(sampleUser)
                .product(sampleProductSingle)
                .purchaseType("SINGLE_BOX")
                .quantity(1) // 1 x 200.000đ = 200.000đ (< 500.000đ)
                .build();
        List<CartItem> cartList = List.of(cartItem);

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(cartItemRepository.findByUserId(1L)).thenReturn(cartList);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // --- 2. ACT (Thực thi hàm cần test) ---
        Order createdOrder = orderService.createOrder(
                1L, "Nguyen Van A", "0987654321",
                "Hà Nội", "Cầu Giấy", "Dịch Vọng",
                "Số 123 Đường Cầu Giấy", "COD", null
        );

        // --- 3. ASSERT (Kiểm tra kết quả) ---
        assertNotNull(createdOrder);
        assertTrue(createdOrder.getOrderCode().startsWith("PW-"));
        assertEquals(sampleUser, createdOrder.getUser());
        assertEquals("Nguyen Van A", createdOrder.getRecipientName());
        assertEquals("0987654321", createdOrder.getRecipientPhone());
        assertEquals("TO_PAY", createdOrder.getStatus());
        assertEquals("COD", createdOrder.getPaymentMethod());

        // Kiểm tra tiền: Subtotal 200.000đ + Ship 30.000đ - Discount 0đ = 230.000đ
        assertBigDecimalEquals("200000", createdOrder.getSubtotalAmount());
        assertBigDecimalEquals("30000", createdOrder.getShippingFee());
        assertBigDecimalEquals("0", createdOrder.getDiscountAmount());
        assertBigDecimalEquals("230000", createdOrder.getTotalAmount());
        assertNull(createdOrder.getCoupon());

        // Kiểm tra verify các Repository tương tác đúng
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderItemRepository, times(1)).saveAll(anyList());
        verify(cartItemRepository, times(1)).deleteAll(cartList);
    }

    // =========================================================================
    // TEST CASE 2: Đơn hàng từ 500k trở lên -> Freeship, áp dụng Coupon % hợp lệ
    // =========================================================================
    @Test
    @DisplayName("Tạo đơn hàng thành công: Trên 500k Freeship, áp dụng coupon giảm giá 10%")
    void createOrder_Success_FreeShipping_WithPercentCoupon() {
        // --- 1. ARRANGE ---
        CartItem cartItem = CartItem.builder()
                .id(2L)
                .user(sampleUser)
                .product(sampleProductSet)
                .purchaseType("WHOLE_SET")
                .quantity(1) // 1 x 600.000đ = 600.000đ (>= 500.000đ -> Freeship)
                .build();
        List<CartItem> cartList = List.of(cartItem);

        Coupon coupon = Coupon.builder()
                .id(10L)
                .code("POP10")
                .discountType("PERCENT")
                .discountValue(new BigDecimal("10")) // 10%
                .minOrderAmount(new BigDecimal("500000"))
                .maxDiscountAmount(new BigDecimal("100000"))
                .endDate(LocalDate.now().plusDays(7)) // Còn hạn
                .active(true)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(cartItemRepository.findByUserId(1L)).thenReturn(cartList);
        when(couponRepository.findByCodeAndActiveTrue("POP10")).thenReturn(Optional.of(coupon));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // --- 2. ACT ---
        Order createdOrder = orderService.createOrder(
                1L, "Nguyen Van A", "0987654321",
                "TP. Hồ Chí Minh", "Quận 1", "Bến Nghé",
                "Số 45 Lê Duẩn", "VNPAY", "POP10"
        );

        // --- 3. ASSERT ---
        assertNotNull(createdOrder);
        // Subtotal = 600.000đ
        // ShippingFee = 0đ (do >= 500.000đ)
        // Discount = 600.000đ * 10% = 60.000đ
        // Total = 600.000đ + 0đ - 60.000đ = 540.000đ
        assertBigDecimalEquals("600000", createdOrder.getSubtotalAmount());
        assertBigDecimalEquals("0", createdOrder.getShippingFee());
        assertBigDecimalEquals("60000", createdOrder.getDiscountAmount());
        assertBigDecimalEquals("540000", createdOrder.getTotalAmount());
        assertNotNull(createdOrder.getCoupon());
        assertEquals("POP10", createdOrder.getCoupon().getCode());
        assertEquals("VNPAY", createdOrder.getPaymentMethod());

        // Kiểm tra danh sách OrderItem được lưu đúng thuộc tính
        ArgumentCaptor<List<OrderItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(orderItemRepository).saveAll(captor.capture());
        List<OrderItem> savedItems = captor.getValue();
        assertEquals(1, savedItems.size());
        assertEquals("WHOLE_SET", savedItems.get(0).getPurchaseType());
        assertBigDecimalEquals("600000", savedItems.get(0).getUnitPrice());
        assertBigDecimalEquals("600000", savedItems.get(0).getTotalPrice());
        assertEquals(createdOrder, savedItems.get(0).getOrder());

        verify(cartItemRepository, times(1)).deleteAll(cartList);
    }

    // =========================================================================
    // TEST CASE 3: Coupon hết hạn hoặc không đủ điều kiện đơn hàng tối thiểu
    // =========================================================================
    @Test
    @DisplayName("Tạo đơn hàng: Coupon đã hết hạn thì không được giảm giá (discount = 0)")
    void createOrder_Success_CouponExpired_NoDiscount() {
        // --- 1. ARRANGE ---
        CartItem cartItem = CartItem.builder()
                .id(3L)
                .user(sampleUser)
                .product(sampleProductSingle)
                .purchaseType("SINGLE_BOX")
                .quantity(1) // 200.000đ
                .build();

        Coupon expiredCoupon = Coupon.builder()
                .code("EXPIRED")
                .discountType("FIXED")
                .discountValue(new BigDecimal("50000"))
                .endDate(LocalDate.now().minusDays(1)) // Đã hết hạn hôm qua
                .active(true)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(cartItemRepository.findByUserId(1L)).thenReturn(List.of(cartItem));
        when(couponRepository.findByCodeAndActiveTrue("EXPIRED")).thenReturn(Optional.of(expiredCoupon));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // --- 2. ACT ---
        Order order = orderService.createOrder(
                1L, "Nguyen Van A", "0987654321",
                "Đà Nẵng", "Hải Châu", "Thạch Thang",
                "12 Bạch Đằng", "COD", "EXPIRED"
        );

        // --- 3. ASSERT ---
        assertBigDecimalEquals("200000", order.getSubtotalAmount());
        assertBigDecimalEquals("30000", order.getShippingFee());
        assertBigDecimalEquals("0", order.getDiscountAmount()); // Không được giảm
        assertBigDecimalEquals("230000", order.getTotalAmount());
        assertNull(order.getCoupon()); // Không được gắn coupon
    }

    // =========================================================================
    // TEST CASE 4: Không tìm thấy User với ID truyền vào -> Ném IllegalArgumentException
    // =========================================================================
    @Test
    @DisplayName("Ném IllegalArgumentException khi không tìm thấy User")
    void createOrder_ThrowsException_WhenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.createOrder(
                        999L, "Nguyen Van A", "0987654321",
                        "Hà Nội", "Cầu Giấy", "Dịch Vọng",
                        "Số 123 Cầu Giấy", "COD", null
                )
        );

        assertTrue(exception.getMessage().contains("Không tìm thấy người dùng với ID: 999"));

        // Tuyệt đối không gọi lưu Order hay xóa giỏ hàng khi ném lỗi
        verifyNoInteractions(cartItemRepository);
        verifyNoInteractions(orderRepository);
        verifyNoInteractions(orderItemRepository);
    }

    // =========================================================================
    // TEST CASE 5: Giỏ hàng rỗng -> Ném IllegalStateException
    // =========================================================================
    @Test
    @DisplayName("Ném IllegalStateException khi giỏ hàng của User đang rỗng")
    void createOrder_ThrowsException_WhenCartIsEmpty() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(cartItemRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> orderService.createOrder(
                        1L, "Nguyen Van A", "0987654321",
                        "Hà Nội", "Cầu Giấy", "Dịch Vọng",
                        "Số 123 Cầu Giấy", "COD", null
                )
        );

        assertTrue(exception.getMessage().contains("Giỏ hàng của bạn đang trống"));
        verifyNoInteractions(orderRepository);
        verifyNoInteractions(orderItemRepository);
    }

    // =========================================================================
    // TEST CASE 6: Tìm đơn hàng theo mã (getOrderByCode)
    // =========================================================================
    @Test
    @DisplayName("getOrderByCode: Trả về đơn hàng khi tìm thấy mã")
    void getOrderByCode_Found() {
        Order mockOrder = Order.builder()
                .id(10L)
                .orderCode("PW-1710000000000")
                .totalAmount(new BigDecimal("500000"))
                .build();

        when(orderRepository.findByOrderCode("PW-1710000000000")).thenReturn(Optional.of(mockOrder));

        Order result = orderService.getOrderByCode("PW-1710000000000");

        assertNotNull(result);
        assertEquals("PW-1710000000000", result.getOrderCode());
        assertBigDecimalEquals("500000", result.getTotalAmount());
    }

    @Test
    @DisplayName("getOrderByCode: Trả về null khi không tìm thấy mã đơn hàng")
    void getOrderByCode_NotFound_ReturnsNull() {
        when(orderRepository.findByOrderCode("PW-NOT-EXIST")).thenReturn(Optional.empty());

        Order result = orderService.getOrderByCode("PW-NOT-EXIST");

        assertNull(result);
    }

    // =========================================================================
    // TEST CASE 7: Lấy danh sách đơn hàng theo User (getOrdersByUser)
    // =========================================================================
    @Test
    @DisplayName("getOrdersByUser: Trả về danh sách đơn hàng của người dùng sắp xếp mới nhất")
    void getOrdersByUser_Success() {
        Order order1 = Order.builder().id(1L).orderCode("PW-001").user(sampleUser).build();
        Order order2 = Order.builder().id(2L).orderCode("PW-002").user(sampleUser).build();

        when(orderRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(order2, order1));

        List<Order> orders = orderService.getOrdersByUser(1L);

        assertNotNull(orders);
        assertEquals(2, orders.size());
        assertEquals("PW-002", orders.get(0).getOrderCode());
        assertEquals("PW-001", orders.get(1).getOrderCode());
        verify(orderRepository, times(1)).findByUserIdOrderByCreatedAtDesc(1L);
    }
}
