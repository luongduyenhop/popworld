package com.manguonmo.popworld.service;

import com.manguonmo.popworld.service.impl.OrderServiceImpl;
import com.manguonmo.popworld.dto.response.CouponDiscountResponse;
import com.manguonmo.popworld.dto.response.OrderResponse;
import com.manguonmo.popworld.dto.response.OrderStatusCountResponse;
import com.manguonmo.popworld.entity.*;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.OutOfStockException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.mapper.OrderMapper;
import com.manguonmo.popworld.repository.*;
import org.springframework.data.domain.Sort;
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
    private CouponService couponService;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User sampleUser;
    private Product sampleProductSingle;
    private Product sampleProductSet;

    @BeforeEach
    void setUp() {
        // Mặc định tồn kho đủ cho các test case thông thường
        lenient().when(productRepository.updateStock(anyLong(), anyInt())).thenReturn(1);

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
        when(cartItemRepository.findByUserIdAndIsSelectedTrue(1L)).thenReturn(cartList);
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
        when(cartItemRepository.findByUserIdAndIsSelectedTrue(1L)).thenReturn(cartList);
        when(couponService.calculateDiscount(eq("POP10"), eq(1L), any(BigDecimal.class)))
                .thenReturn(CouponDiscountResponse.builder()
                        .couponCode("POP10")
                        .discountAmount(new BigDecimal("60000"))
                        .build());
        when(couponService.applyCoupon(eq("POP10"), eq(1L), any(BigDecimal.class)))
                .thenReturn(coupon);
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
    @DisplayName("Tạo đơn hàng: Ném BadRequestException khi mã giảm giá không hợp lệ hoặc hết hạn")
    void createOrder_ThrowsBadRequestException_WhenCouponInvalid() {
        // --- 1. ARRANGE ---
        CartItem cartItem = CartItem.builder()
                .id(3L)
                .user(sampleUser)
                .product(sampleProductSingle)
                .purchaseType("SINGLE_BOX")
                .quantity(1) // 200.000đ
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(cartItemRepository.findByUserIdAndIsSelectedTrue(1L)).thenReturn(List.of(cartItem));
        when(couponService.calculateDiscount(eq("EXPIRED"), eq(1L), any(BigDecimal.class)))
                .thenThrow(new BadRequestException("Mã giảm giá đã hết hạn sử dụng!"));

        // --- 2. ACT & ASSERT ---
        assertThrows(BadRequestException.class, () -> orderService.createOrder(
                1L, "Nguyen Van A", "0987654321",
                "Đà Nẵng", "Hải Châu", "Thạch Thang",
                "12 Bạch Đằng", "COD", "EXPIRED"
        ));
    }

    // =========================================================================
    // TEST CASE 4: Không tìm thấy User -> Ném ResourceNotFoundException
    // =========================================================================
    @Test
    @DisplayName("Ném ResourceNotFoundException khi không tìm thấy User")
    void createOrder_ThrowsException_WhenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
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
    // TEST CASE 5: Giỏ hàng rỗng -> Ném BadRequestException
    // =========================================================================
    @Test
    @DisplayName("Ném BadRequestException khi giỏ hàng của User đang rỗng hoặc không có món được chọn")
    void createOrder_ThrowsException_WhenCartIsEmpty() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(cartItemRepository.findByUserIdAndIsSelectedTrue(1L)).thenReturn(Collections.emptyList());

        BadRequestException exception = assertThrows(
                BadRequestException.class,
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

    // =========================================================================
    // TEST CASE 8: Thất bại khi hết hàng (updateStock trả về 0)
    // =========================================================================
    @Test
    @DisplayName("Thất bại khi hết hàng: updateStock trả về 0 sẽ ném OutOfStockException và không tạo đơn")
    void createOrder_Fail_OutOfStock_ThrowsException() {
        CartItem cartItem = CartItem.builder()
                .id(1L)
                .user(sampleUser)
                .product(sampleProductSingle)
                .purchaseType("SINGLE_BOX")
                .quantity(3)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(cartItemRepository.findByUserIdAndIsSelectedTrue(1L)).thenReturn(List.of(cartItem));
        when(productRepository.updateStock(eq(sampleProductSingle.getId()), eq(3))).thenReturn(0); // Kho không đủ

        OutOfStockException ex = assertThrows(OutOfStockException.class, () ->
                orderService.createOrder(
                        1L, "Nguyen Van A", "0987654321",
                        "Hà Nội", "Cầu Giấy", "Dịch Vọng",
                        "Số 123 Đường Cầu Giấy", "COD", null
                )
        );

        assertTrue(ex.getMessage().contains("đã hết hàng hoặc không đủ số lượng tồn kho"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    // =========================================================================
    // TEST CASE 9: Chỉ mua và xóa các món được chọn (isSelected = true)
    // =========================================================================
    @Test
    @DisplayName("Chỉ đặt hàng và xóa khỏi giỏ các món isSelected == true, giữ lại món chưa chọn")
    void createOrder_OnlySelectedItemsProcessedAndDeleted() {
        CartItem selectedItem = CartItem.builder()
                .id(1L)
                .user(sampleUser)
                .product(sampleProductSingle)
                .purchaseType("SINGLE_BOX")
                .quantity(1)
                .isSelected(true)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        // Repository chỉ trả về món đã chọn
        when(cartItemRepository.findByUserIdAndIsSelectedTrue(1L)).thenReturn(List.of(selectedItem));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order order = orderService.createOrder(
                1L, "Nguyen Van A", "0987654321",
                "Hà Nội", "Cầu Giấy", "Dịch Vọng",
                "123 Cầu Giấy", "COD", null
        );

        assertNotNull(order);
        // Verify chỉ xóa danh sách các món ĐƯỢC CHỌN (selectedItem)
        verify(cartItemRepository, times(1)).deleteAll(List.of(selectedItem));
    }

    // =========================================================================
    // NHÓM TEST: cancelOrder (Hủy đơn hàng chủ động bởi người dùng)
    // =========================================================================

    @Test
    @DisplayName("Hủy đơn hàng thành công: Hoàn kho, hoàn mã coupon và chuyển status sang CANCELLED (chỉ khi TO_PAY)")
    void cancelOrder_Success_RestoresStockAndReleasesCoupon() {
        // --- 1. ARRANGE ---
        Coupon coupon = Coupon.builder().id(99L).code("POP10").build();
        Order order = Order.builder()
                .id(100L)
                .orderCode("PW-100")
                .user(sampleUser) // id = 1L
                .status("TO_PAY") // Khách hàng chỉ được hủy khi TO_PAY
                .coupon(coupon)
                .build();

        Product prod1 = Product.builder().id(101L).name("Hirono Box").build();
        Product prod2 = Product.builder().id(102L).name("Skullpanda Set").build();

        OrderItem item1 = OrderItem.builder().id(1L).product(prod1).purchaseType("SINGLE_BOX").quantity(2).build();
        OrderItem item2 = OrderItem.builder().id(2L).product(prod2).purchaseType("WHOLE_SET").quantity(1).build();

        when(orderRepository.findByOrderCode("PW-100")).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(100L)).thenReturn(List.of(item1, item2));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // --- 2. ACT ---
        Order cancelledOrder = orderService.cancelOrder(1L, "PW-100", "Đổi ý không mua nữa");

        // --- 3. ASSERT ---
        assertEquals("CANCELLED", cancelledOrder.getStatus());
        assertTrue(cancelledOrder.getNote().contains("Đổi ý không mua nữa"));

        // Hoàn kho SINGLE_BOX (2 hộp lẻ)
        verify(productRepository, times(1)).addStock(101L, 2);
        // Hoàn kho WHOLE_SET (1 set = 12 hộp lẻ)
        verify(productRepository, times(1)).addStock(102L, 12);
        // Hoàn mã coupon
        verify(couponService, times(1)).releaseCoupon(99L, 1L);
        // Lưu lại đơn hàng
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    @DisplayName("Hủy đơn hàng thất bại: Ném ResourceNotFoundException khi không tìm thấy mã đơn")
    void cancelOrder_ThrowsResourceNotFoundException_WhenOrderNotFound() {
        when(orderRepository.findByOrderCode("PW-UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                orderService.cancelOrder(1L, "PW-UNKNOWN", "Lý do")
        );

        verify(orderRepository, never()).save(any(Order.class));
        verify(productRepository, never()).addStock(anyLong(), anyInt());
        verify(couponService, never()).releaseCoupon(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Hủy đơn hàng thất bại: Chống IDOR - Ném BadRequestException khi User không sở hữu đơn")
    void cancelOrder_ThrowsBadRequestException_WhenUserNotOwner() {
        User otherUser = User.builder().id(2L).fullName("User Khác").build();
        Order order = Order.builder()
                .id(101L)
                .orderCode("PW-101")
                .user(otherUser) // Đơn thuộc user 2
                .status("TO_PAY")
                .build();

        when(orderRepository.findByOrderCode("PW-101")).thenReturn(Optional.of(order));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.cancelOrder(1L, "PW-101", "Cố tình hủy trộm đơn")
        );

        assertTrue(ex.getMessage().contains("Bạn không có quyền hủy đơn hàng này"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Hủy đơn hàng thất bại: Khách hàng KHÔNG được hủy khi đơn đang PROCESSING")
    void cancelOrder_ThrowsBadRequestException_WhenProcessing() {
        Order processingOrder = Order.builder()
                .id(102L)
                .orderCode("PW-102")
                .user(sampleUser)
                .status("PROCESSING")
                .build();

        when(orderRepository.findByOrderCode("PW-102")).thenReturn(Optional.of(processingOrder));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.cancelOrder(1L, "PW-102", "Hủy khi đang đóng gói")
        );

        assertTrue(ex.getMessage().contains("Chờ thanh toán (TO_PAY)"));
        verify(orderRepository, never()).save(any(Order.class));
        verify(productRepository, never()).addStock(anyLong(), anyInt());
    }

    @Test
    @DisplayName("Hủy đơn hàng thất bại: Ném BadRequestException khi đơn đã giao (SHIPPING) hoặc hoàn tất (DELIVERED)")
    void cancelOrder_ThrowsBadRequestException_WhenOrderStatusInvalid() {
        Order shippingOrder = Order.builder()
                .id(103L)
                .orderCode("PW-103")
                .user(sampleUser)
                .status("SHIPPING")
                .build();

        when(orderRepository.findByOrderCode("PW-103")).thenReturn(Optional.of(shippingOrder));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.cancelOrder(1L, "PW-103", "Đơn đã đi giao rồi")
        );

        assertTrue(ex.getMessage().contains("Chờ thanh toán (TO_PAY)"));
        verify(orderRepository, never()).save(any(Order.class));
    }


    @Test
    @DisplayName("getOrderItems trả về danh sách OrderItem của đơn hàng từ orderItemRepository")
    void getOrderItems_ShouldReturnListFromRepository() {
        OrderItem item = OrderItem.builder().id(10L).quantity(2).build();
        when(orderItemRepository.findByOrderId(100L)).thenReturn(List.of(item));

        List<OrderItem> result = orderService.getOrderItems(100L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
        verify(orderItemRepository, times(1)).findByOrderId(100L);
    }

    // =========================================================================
    // ADMIN ORDER MANAGEMENT TESTS
    // =========================================================================

    @Test
    @DisplayName("getAllOrders: Lấy tất cả đơn hàng khi status là ALL hoặc null")
    void getAllOrders_WhenStatusAllOrNull_ShouldReturnAllOrdersSorted() {
        Order mockOrder = Order.builder().id(1L).orderCode("PW-ALL-1").build();
        OrderItem mockItem = OrderItem.builder().id(11L).build();
        OrderResponse mockResponse = OrderResponse.builder().orderCode("PW-ALL-1").build();

        when(orderRepository.findAll(any(Sort.class))).thenReturn(List.of(mockOrder));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(mockItem));
        when(orderMapper.toResponse(eq(mockOrder), anyList())).thenReturn(mockResponse);

        List<OrderResponse> result = orderService.getAllOrders("ALL");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("PW-ALL-1", result.get(0).getOrderCode());
        verify(orderRepository).findAll(any(Sort.class));
        verify(orderRepository, never()).findByStatusOrderByCreatedAtDesc(anyString());
    }

    @Test
    @DisplayName("getAllOrders: Lọc theo status cụ thể (PROCESSING)")
    void getAllOrders_WhenStatusSpecific_ShouldFilterByStatus() {
        Order mockOrder = Order.builder().id(2L).orderCode("PW-PROC-2").status("PROCESSING").build();
        OrderResponse mockResponse = OrderResponse.builder().orderCode("PW-PROC-2").status("PROCESSING").build();

        when(orderRepository.findByStatusOrderByCreatedAtDesc("PROCESSING")).thenReturn(List.of(mockOrder));
        when(orderItemRepository.findByOrderId(2L)).thenReturn(List.of());
        when(orderMapper.toResponse(eq(mockOrder), anyList())).thenReturn(mockResponse);

        List<OrderResponse> result = orderService.getAllOrders("PROCESSING");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("PW-PROC-2", result.get(0).getOrderCode());
        verify(orderRepository).findByStatusOrderByCreatedAtDesc("PROCESSING");
        verify(orderRepository, never()).findAll(any(Sort.class));
    }

    @Test
    @DisplayName("searchOrders: Tìm kiếm đơn hàng theo từ khóa")
    void searchOrders_WithKeyword_ShouldReturnMatchingOrders() {
        Order mockOrder = Order.builder().id(3L).orderCode("PW-SRCH-3").build();
        OrderResponse mockResponse = OrderResponse.builder().orderCode("PW-SRCH-3").build();

        when(orderRepository.searchOrders("0987654321")).thenReturn(List.of(mockOrder));
        when(orderItemRepository.findByOrderId(3L)).thenReturn(List.of());
        when(orderMapper.toResponse(eq(mockOrder), anyList())).thenReturn(mockResponse);

        List<OrderResponse> result = orderService.searchOrders("  0987654321  ");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("PW-SRCH-3", result.get(0).getOrderCode());
        verify(orderRepository).searchOrders("0987654321");
    }

    @Test
    @DisplayName("searchOrders: Khi keyword là null hoặc rỗng -> fallback về getAllOrders(ALL)")
    void searchOrders_BlankKeyword_ShouldFallbackToAllOrders() {
        Order mockOrder = Order.builder().id(4L).orderCode("PW-FALLBACK").build();
        OrderResponse mockResponse = OrderResponse.builder().orderCode("PW-FALLBACK").build();

        when(orderRepository.findAll(any(Sort.class))).thenReturn(List.of(mockOrder));
        when(orderItemRepository.findByOrderId(4L)).thenReturn(List.of());
        when(orderMapper.toResponse(eq(mockOrder), anyList())).thenReturn(mockResponse);

        List<OrderResponse> result = orderService.searchOrders("   ");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("PW-FALLBACK", result.get(0).getOrderCode());
        verify(orderRepository).findAll(any(Sort.class));
    }

    @Test
    @DisplayName("getOrderStatusCounts: Trả về thống kê số lượng đơn theo từng trạng thái chuẩn hóa")
    void getOrderStatusCounts_ShouldReturnAllCounts() {
        when(orderRepository.count()).thenReturn(51L);
        when(orderRepository.countByStatus("TO_PAY")).thenReturn(5L);
        when(orderRepository.countByStatus("PROCESSING")).thenReturn(10L);
        when(orderRepository.countByStatus("SHIPPING")).thenReturn(15L);
        when(orderRepository.countByStatus("DELIVERED")).thenReturn(18L);
        when(orderRepository.countByStatus("CANCELLED")).thenReturn(2L);
        when(orderRepository.countByStatus("EXPIRED")).thenReturn(1L);

        OrderStatusCountResponse counts = orderService.getOrderStatusCounts();

        assertNotNull(counts);
        assertEquals(51L, counts.getAll());
        assertEquals(5L, counts.getToPay());
        assertEquals(10L, counts.getProcessing());
        assertEquals(15L, counts.getShipping());
        assertEquals(18L, counts.getDelivered());
        assertEquals(2L, counts.getCancelled());
        assertEquals(1L, counts.getExpired());
    }

    @Test
    @DisplayName("shipOrder: Chuyển trạng thái đơn hàng từ PROCESSING sang SHIPPING thành công")
    void shipOrder_Success() {
        Order order = Order.builder().id(5L).orderCode("PW-SHIP-1").status("PROCESSING").build();
        when(orderRepository.findByOrderCode("PW-SHIP-1")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.shipOrder("pw-ship-1");

        assertNotNull(result);
        assertEquals("SHIPPING", result.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("shipOrder: Ném BadRequestException nếu đơn không ở trạng thái PROCESSING")
    void shipOrder_ThrowsBadRequestException_WhenNotProcessing() {
        Order order = Order.builder().id(6L).orderCode("PW-SHIP-2").status("TO_PAY").build();
        when(orderRepository.findByOrderCode("PW-SHIP-2")).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class, () -> orderService.shipOrder("PW-SHIP-2"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("completeOrder: Chuyển trạng thái từ SHIPPING sang DELIVERED và cập nhật paidAt")
    void completeOrder_Success() {
        Order order = Order.builder().id(7L).orderCode("PW-COMP-1").status("SHIPPING").build();
        when(orderRepository.findByOrderCode("PW-COMP-1")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.completeOrder("pw-comp-1");

        assertNotNull(result);
        assertEquals("DELIVERED", result.getStatus());
        assertNotNull(result.getPaidAt());
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("completeOrder: Ném BadRequestException nếu đơn không ở trạng thái SHIPPING")
    void completeOrder_ThrowsBadRequestException_WhenNotShipping() {
        Order order = Order.builder().id(8L).orderCode("PW-COMP-2").status("PROCESSING").build();
        when(orderRepository.findByOrderCode("PW-COMP-2")).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class, () -> orderService.completeOrder("PW-COMP-2"));
        verify(orderRepository, never()).save(any(Order.class));
    }


    // =========================================================================
    // TASK 005: CONCURRENCY / TRANSACTION ROLLBACK SIMULATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Transaction Rollback: Giỏ hàng 2 món, món 1 trừ kho thành công, món 2 hết hàng -> Ném OutOfStockException, không lưu Order và không xóa Cart")
    void createOrder_RollbackSimulation_WhenSecondItemOutOfStock() {
        CartItem item1 = CartItem.builder()
                .id(1L)
                .user(sampleUser)
                .product(sampleProductSingle)
                .purchaseType("SINGLE_BOX")
                .quantity(1)
                .build();

        CartItem item2 = CartItem.builder()
                .id(2L)
                .user(sampleUser)
                .product(sampleProductSet)
                .purchaseType("WHOLE_SET")
                .quantity(1) // 12 hộp lẻ
                .build();

        List<CartItem> cartList = List.of(item1, item2);

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(cartItemRepository.findByUserIdAndIsSelectedTrue(1L)).thenReturn(cartList);

        // Món 1: trừ kho 1 hộp -> Thành công (1 row updated)
        when(productRepository.updateStock(sampleProductSingle.getId(), 1)).thenReturn(1);
        // Món 2: trừ kho 12 hộp -> Hết hàng (0 row updated do race condition hoặc thiếu hàng)
        when(productRepository.updateStock(sampleProductSet.getId(), 12)).thenReturn(0);

        OutOfStockException ex = assertThrows(OutOfStockException.class, () ->
                orderService.createOrder(
                        1L, "Nguyen Van A", "0987654321",
                        "Hà Nội", "Cầu Giấy", "Dịch Vọng",
                        "Số 123 Đường Cầu Giấy", "COD", null
                )
        );

        assertTrue(ex.getMessage().contains("đã hết hàng hoặc không đủ số lượng tồn kho"));
        // Đảm bảo không tạo Order và không xóa Cart (Transaction sẽ rollback các thay đổi trước đó)
        verify(orderRepository, never()).save(any(Order.class));
        verify(orderItemRepository, never()).saveAll(anyList());
        verify(cartItemRepository, never()).deleteAll(anyList());
    }

    // =========================================================================
    // TASK 005: WHOLE SET * 12 CONSISTENCY TESTS
    // =========================================================================

    @Test
    @DisplayName("Whole Set Consistency: Mua 2 Whole Set thì phải trừ đúng 24 hộp đơn từ tồn kho")
    void createOrder_WholeSet_DeductsTwentyFourUnitsFromStock() {
        CartItem wholeSetItem = CartItem.builder()
                .id(10L)
                .user(sampleUser)
                .product(sampleProductSet)
                .purchaseType("WHOLE_SET")
                .quantity(2) // 2 set * 12 = 24 boxes
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(cartItemRepository.findByUserIdAndIsSelectedTrue(1L)).thenReturn(List.of(wholeSetItem));
        when(productRepository.updateStock(sampleProductSet.getId(), 24)).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order order = orderService.createOrder(
                1L, "Nguyen Van A", "0987654321",
                "Hà Nội", "Cầu Giấy", "Dịch Vọng",
                "Số 123 Đường Cầu Giấy", "COD", null
        );

        assertNotNull(order);
        // Verify updateStock được gọi với chính xác 24 đơn vị
        verify(productRepository, times(1)).updateStock(sampleProductSet.getId(), 24);
    }

    @Test
    @DisplayName("Whole Set Consistency: Hủy đơn hàng chứa 2 Whole Set thì phải hoàn trả đúng 24 hộp đơn vào tồn kho")
    void cancelOrder_WholeSet_RestoresTwentyFourUnitsToStock() {
        Order order = Order.builder()
                .id(200L)
                .orderCode("PW-200")
                .user(sampleUser)
                .status("TO_PAY")
                .build();

        OrderItem item = OrderItem.builder()
                .id(20L)
                .product(sampleProductSet)
                .purchaseType("WHOLE_SET")
                .quantity(2) // 2 set * 12 = 24 boxes
                .build();

        when(orderRepository.findByOrderCode("PW-200")).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(200L)).thenReturn(List.of(item));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.cancelOrder(1L, "PW-200", "Đổi ý");

        assertEquals("CANCELLED", result.getStatus());
        // Verify hoàn đúng 24 hộp
        verify(productRepository, times(1)).addStock(sampleProductSet.getId(), 24);
    }

    // =========================================================================
    // TASK 005: ORDER STATE MACHINE INVARIANTS & TRANSITIONS
    // =========================================================================

    @Test
    @DisplayName("cancelOrder: Hủy thành công từ trạng thái TO_PAY")
    void cancelOrder_Success_FromToPayStatus() {
        Order order = Order.builder()
                .id(201L)
                .orderCode("PW-201")
                .user(sampleUser)
                .status("TO_PAY")
                .build();

        OrderItem item = OrderItem.builder()
                .id(21L)
                .product(sampleProductSingle)
                .purchaseType("SINGLE_BOX")
                .quantity(1)
                .build();

        when(orderRepository.findByOrderCode("PW-201")).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(201L)).thenReturn(List.of(item));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.cancelOrder(1L, "PW-201", "Hủy đơn chờ thanh toán");

        assertEquals("CANCELLED", result.getStatus());
        verify(productRepository, times(1)).addStock(sampleProductSingle.getId(), 1);
    }

    @Test
    @DisplayName("cancelOrder: Ném BadRequestException khi đơn đã bị hủy trước đó (CANCELLED)")
    void cancelOrder_ThrowsBadRequestException_WhenAlreadyCancelled() {
        Order order = Order.builder()
                .id(202L)
                .orderCode("PW-202")
                .user(sampleUser)
                .status("CANCELLED")
                .build();

        when(orderRepository.findByOrderCode("PW-202")).thenReturn(Optional.of(order));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.cancelOrder(1L, "PW-202", "Hủy tiếp lần 2")
        );

        assertTrue(ex.getMessage().contains("đã bị hủy"));
        verify(orderRepository, never()).save(any(Order.class));
        verify(productRepository, never()).addStock(anyLong(), anyInt());
    }

    @Test
    @DisplayName("cancelOrder: Ném BadRequestException khi đơn đã giao hàng (DELIVERED)")
    void cancelOrder_ThrowsBadRequestException_WhenDelivered() {
        Order order = Order.builder()
                .id(203L)
                .orderCode("PW-203")
                .user(sampleUser)
                .status("DELIVERED")
                .build();

        when(orderRepository.findByOrderCode("PW-203")).thenReturn(Optional.of(order));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.cancelOrder(1L, "PW-203", "Hủy sau khi nhận hàng")
        );

        assertTrue(ex.getMessage().contains("Chờ thanh toán (TO_PAY)"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("cancelOrder: Ném BadRequestException khi đơn đã hết hạn 15 phút (EXPIRED)")
    void cancelOrder_ThrowsBadRequestException_WhenExpired() {
        Order order = Order.builder()
                .id(204L)
                .orderCode("PW-204")
                .user(sampleUser)
                .status("EXPIRED")
                .build();

        when(orderRepository.findByOrderCode("PW-204")).thenReturn(Optional.of(order));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.cancelOrder(1L, "PW-204", "Hủy đơn hết hạn")
        );

        assertTrue(ex.getMessage().contains("hết hạn thanh toán"));
        verify(orderRepository, never()).save(any(Order.class));
        verify(productRepository, never()).addStock(anyLong(), anyInt());
    }

    @Test
    @DisplayName("adminCancelOrder: Admin hủy đơn TO_PAY thành công, hoàn kho và giải phóng coupon")
    void adminCancelOrder_Success_FromToPayStatus() {
        Coupon coupon = Coupon.builder().id(50L).code("SALE50").build();
        Order order = Order.builder()
                .id(301L)
                .orderCode("PW-ADMIN-1")
                .user(sampleUser)
                .status("TO_PAY")
                .coupon(coupon)
                .build();

        OrderItem item = OrderItem.builder()
                .id(31L)
                .product(sampleProductSet)
                .purchaseType("WHOLE_SET")
                .quantity(1)
                .build();

        when(orderRepository.findByOrderCode("PW-ADMIN-1")).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(301L)).thenReturn(List.of(item));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.adminCancelOrder("PW-ADMIN-1", "Khách yêu cầu hủy qua hotline");

        assertEquals("CANCELLED", result.getStatus());
        assertTrue(result.getNote().contains("Admin hủy"));
        assertTrue(result.getNote().contains("Khách yêu cầu hủy qua hotline"));

        verify(productRepository, times(1)).addStock(sampleProductSet.getId(), 12);
        verify(couponService, times(1)).releaseCoupon(50L, 1L);
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    @DisplayName("adminCancelOrder: Admin hủy đơn PROCESSING thành công")
    void adminCancelOrder_Success_FromProcessingStatus() {
        Order order = Order.builder()
                .id(302L)
                .orderCode("PW-ADMIN-2")
                .user(sampleUser)
                .status("PROCESSING")
                .build();

        OrderItem item = OrderItem.builder()
                .id(32L)
                .product(sampleProductSingle)
                .purchaseType("SINGLE_BOX")
                .quantity(3)
                .build();

        when(orderRepository.findByOrderCode("PW-ADMIN-2")).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(302L)).thenReturn(List.of(item));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.adminCancelOrder("PW-ADMIN-2", "Hết hàng kho đột xuất");

        assertEquals("CANCELLED", result.getStatus());
        verify(productRepository, times(1)).addStock(sampleProductSingle.getId(), 3);
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    @DisplayName("adminCancelOrder: Thất bại khi đơn đang SHIPPING")
    void adminCancelOrder_ThrowsBadRequestException_WhenShipping() {
        Order order = Order.builder()
                .id(303L)
                .orderCode("PW-ADMIN-3")
                .status("SHIPPING")
                .build();

        when(orderRepository.findByOrderCode("PW-ADMIN-3")).thenReturn(Optional.of(order));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.adminCancelOrder("PW-ADMIN-3", "Hủy ngang")
        );

        assertTrue(ex.getMessage().contains("SHIPPING") || ex.getMessage().contains("vận chuyển"));
        verify(orderRepository, never()).save(any(Order.class));
        verify(productRepository, never()).addStock(anyLong(), anyInt());
    }

    @Test
    @DisplayName("adminCancelOrder: Thất bại khi đơn đã DELIVERED")
    void adminCancelOrder_ThrowsBadRequestException_WhenDelivered() {
        Order order = Order.builder()
                .id(304L)
                .orderCode("PW-ADMIN-4")
                .status("DELIVERED")
                .build();

        when(orderRepository.findByOrderCode("PW-ADMIN-4")).thenReturn(Optional.of(order));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.adminCancelOrder("PW-ADMIN-4", "Hủy đơn đã giao thành công")
        );

        assertTrue(ex.getMessage().contains("DELIVERED") || ex.getMessage().contains("thành công"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("adminCancelOrder: Thất bại khi đơn đã CANCELLED")
    void adminCancelOrder_ThrowsBadRequestException_WhenAlreadyCancelled() {
        Order order = Order.builder()
                .id(305L)
                .orderCode("PW-ADMIN-5")
                .status("CANCELLED")
                .build();

        when(orderRepository.findByOrderCode("PW-ADMIN-5")).thenReturn(Optional.of(order));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.adminCancelOrder("PW-ADMIN-5", "Hủy lại")
        );

        assertTrue(ex.getMessage().contains("đã bị hủy"));
        verify(orderRepository, never()).save(any(Order.class));
        verify(productRepository, never()).addStock(anyLong(), anyInt());
    }

    @Test
    @DisplayName("adminCancelOrder: Thất bại khi đơn đã EXPIRED")
    void adminCancelOrder_ThrowsBadRequestException_WhenExpired() {
        Order order = Order.builder()
                .id(306L)
                .orderCode("PW-ADMIN-6")
                .status("EXPIRED")
                .build();

        when(orderRepository.findByOrderCode("PW-ADMIN-6")).thenReturn(Optional.of(order));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.adminCancelOrder("PW-ADMIN-6", "Hủy đơn đã hết hạn")
        );

        assertTrue(ex.getMessage().contains("hết hạn thanh toán"));
        verify(orderRepository, never()).save(any(Order.class));
        verify(productRepository, never()).addStock(anyLong(), anyInt());
    }

    // =========================================================================
    // TASK 005: IDEMPOTENCY INVARIANT TESTS (RESTORE STOCK CHÍNH XÁC MỘT LẦN)
    // =========================================================================

    @Test
    @DisplayName("Idempotency: Hủy đơn lần 2 thì ném ngoại lệ và tuyệt đối không hoàn kho lần 2")
    void cancelOrder_Idempotency_ThrowsExceptionAndNeverRestoresStockTwice() {
        Order order = Order.builder()
                .id(401L)
                .orderCode("PW-IDEM-1")
                .user(sampleUser)
                .status("CANCELLED")
                .build();

        when(orderRepository.findByOrderCode("PW-IDEM-1")).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class, () ->
                orderService.cancelOrder(1L, "PW-IDEM-1", "Hủy lại lần 2")
        );

        // Đảm bảo không tương tác hoàn kho lần 2
        verify(productRepository, never()).addStock(anyLong(), anyInt());
        verify(couponService, never()).releaseCoupon(anyLong(), anyLong());
    }
}




