package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.request.SePayWebhookRequest;
import com.manguonmo.popworld.entity.*;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.OutOfStockException;
import com.manguonmo.popworld.mapper.OrderMapper;
import com.manguonmo.popworld.repository.*;
import com.manguonmo.popworld.service.impl.OrderServiceImpl;
import com.manguonmo.popworld.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Concurrency, Transaction & Order Lifecycle Regression Tests
 * 
 * Kiểm thử chuyên sâu các kịch bản:
 * 1. Chống va chạm mã đơn hàng (orderCode collision) dưới nhiều luồng đồng thời
 * 2. Tính toàn vẹn quy cách đóng gói (Whole Set 12 boxes vs Single Box)
 * 3. Chống hủy đơn hàng 2 lần (Double Cancel Prevention & Coupon Release Idempotency)
 * 4. Ngăn chặn SePay Webhook ghi đè đơn hàng đã CANCELLED hoặc EXPIRED
 * 5. Tính nguyên tử khi trừ tồn kho thất bại (OutOfStockException rollback)
 */
@ExtendWith(MockitoExtension.class)
class OrderConcurrencyAndLifecycleTest {

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

    private PaymentServiceImpl paymentService;

    private static final String API_KEY = "test_secret_api_key_2026";

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(orderRepository);
        ReflectionTestUtils.setField(paymentService, "apiKey", API_KEY);
    }

    @Test
    @DisplayName("Concurrency: Sinh 200 mã orderCode đồng thời từ 20 threads không có va chạm trùng lặp")
    void test_OrderCodeGeneration_NoCollisions_UnderConcurrentThreads() throws InterruptedException, ExecutionException {
        int threadCount = 20;
        int ordersPerThread = 10;
        int totalOrders = threadCount * ordersPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        Set<String> generatedCodes = ConcurrentHashMap.newKeySet();
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<Void>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                startLatch.await();
                for (int j = 0; j < ordersPerThread; j++) {
                    String code = orderService.generateOrderCode();
                    generatedCodes.add(code);
                }
                return null;
            }));
        }

        startLatch.countDown();
        for (Future<Void> future : futures) {
            future.get();
        }
        executor.shutdown();

        assertEquals(totalOrders, generatedCodes.size(), "Tất cả " + totalOrders + " mã đơn hàng sinh ra phải là duy nhất, không trùng lặp");
    }

    @Test
    @DisplayName("Stock Invariant: WHOLE_SET với số lượng 2 bắt buộc yêu cầu trừ 24 hộp đơn (12 x 2)")
    void test_CreateOrder_WholeSet_CalculatesCorrectStockBoxes() {
        User user = User.builder().id(1L).email("user@popworld.com").build();
        Product product = Product.builder()
                .id(10L)
                .name("Labubu Box")
                .singlePrice(new BigDecimal("100000"))
                .wholeSetPrice(new BigDecimal("1100000"))
                .stockQuantity(50)
                .build();

        CartItem setItem = CartItem.builder()
                .id(1L)
                .user(user)
                .product(product)
                .purchaseType("WHOLE_SET")
                .quantity(2)
                .isSelected(true)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartItemRepository.findByUserIdAndIsSelectedTrue(1L)).thenReturn(List.of(setItem));
        when(productRepository.updateStock(10L, 24)).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order order = orderService.createOrder(1L, "Test", "0900000000", "HN", "HK", "TT", "1 Tran Phu", "COD", null);

        assertNotNull(order);
        verify(productRepository, times(1)).updateStock(10L, 24);
    }

    @Test
    @DisplayName("Stock Atomicity: Hết hàng khi trừ tồn kho ném OutOfStockException và hủy tiến trình tạo đơn")
    void test_CreateOrder_OutOfStock_AbortsCreation() {
        User user = User.builder().id(1L).email("user@popworld.com").build();
        Product product = Product.builder()
                .id(10L)
                .name("Labubu Box")
                .singlePrice(new BigDecimal("100000"))
                .stockQuantity(1)
                .build();

        CartItem item = CartItem.builder()
                .id(1L)
                .user(user)
                .product(product)
                .purchaseType("SINGLE_BOX")
                .quantity(5)
                .isSelected(true)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartItemRepository.findByUserIdAndIsSelectedTrue(1L)).thenReturn(List.of(item));
        when(productRepository.updateStock(10L, 5)).thenReturn(0);

        assertThrows(OutOfStockException.class, () ->
                orderService.createOrder(1L, "Test", "0900000000", "HN", "HK", "TT", "1 Tran Phu", "COD", null)
        );

        verify(orderRepository, never()).save(any());
        verify(cartItemRepository, never()).deleteAll(any());
    }

    @Test
    @DisplayName("Lifecycle Idempotency: Không cho phép hủy đơn hàng 2 lần (chống hoàn kho lặp và double coupon release)")
    void test_CancelOrder_SecondAttempt_ThrowsBadRequestException() {
        User user = User.builder().id(1L).email("user@popworld.com").build();
        Coupon coupon = Coupon.builder().id(5L).code("POP10").build();
        Order order = Order.builder()
                .id(100L)
                .orderCode("PW-100")
                .user(user)
                .status("CANCELLED")
                .coupon(coupon)
                .build();

        when(orderRepository.findByOrderCode("PW-100")).thenReturn(Optional.of(order));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.cancelOrder(1L, "PW-100", "Muon huy lai")
        );

        assertTrue(ex.getMessage().contains("đã bị hủy từ trước"));
        verify(couponService, never()).releaseCoupon(anyLong(), anyLong());
        verify(productRepository, never()).addStock(anyLong(), anyInt());
    }

    @Test
    @DisplayName("Payment vs Cancel Race: SePay Webhook từ chối cập nhật PROCESSING nếu đơn đã bị CANCELLED")
    void test_SePayWebhook_RejectsCancelledOrder() {
        Order cancelledOrder = Order.builder()
                .orderCode("PW-999")
                .status("CANCELLED")
                .totalAmount(new BigDecimal("500000"))
                .build();

        when(orderRepository.findByOrderCode("PW-999")).thenReturn(Optional.of(cancelledOrder));

        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .content("PW-999")
                .transferAmount(new BigDecimal("500000"))
                .build();

        boolean result = paymentService.processSePayWebhook(request, "Bearer " + API_KEY);

        assertFalse(result, "Webhook phải từ chối ghi nhận thanh toán cho đơn đã CANCELLED");
        assertEquals("CANCELLED", cancelledOrder.getStatus(), "Trạng thái không được đổi sang PROCESSING");
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Payment vs Expire Race: SePay Webhook từ chối cập nhật PROCESSING nếu đơn đã bị EXPIRED bởi Scheduler")
    void test_SePayWebhook_RejectsExpiredOrder() {
        Order expiredOrder = Order.builder()
                .orderCode("PW-888")
                .status("EXPIRED")
                .totalAmount(new BigDecimal("300000"))
                .build();

        when(orderRepository.findByOrderCode("PW-888")).thenReturn(Optional.of(expiredOrder));

        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .content("PW-888")
                .transferAmount(new BigDecimal("300000"))
                .build();

        boolean result = paymentService.processSePayWebhook(request, "Bearer " + API_KEY);

        assertFalse(result, "Webhook phải từ chối ghi nhận thanh toán cho đơn đã EXPIRED");
        assertEquals("EXPIRED", expiredOrder.getStatus(), "Trạng thái không được đổi sang PROCESSING");
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Points Lifecycle: Khi hoàn tất giao hàng (DELIVERED), cộng điểm thưởng (10.000đ = 1 pt) cho user")
    void test_Points_EarnOnDelivered() {
        User user = User.builder().id(1L).rewardPoints(100).build();
        Order order = Order.builder()
                .id(1L)
                .orderCode("PW-DELIVERY-1")
                .status("SHIPPING")
                .user(user)
                .subtotalAmount(new BigDecimal("500000"))
                .pointsEarned(50) // 500.000 / 10.000 = 50 điểm
                .build();

        when(orderRepository.findByOrderCode("PW-DELIVERY-1")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Order completed = orderService.completeOrder("PW-DELIVERY-1");

        assertEquals("DELIVERED", completed.getStatus());
        assertEquals(150, user.getRewardPoints(), "User phải được cộng thêm 50 điểm vào tổng 100 điểm hiện có");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Points Redemption: Dùng điểm hợp lệ trong checkout (1 pt = 100đ, <= 20% subtotal) -> Trừ điểm và giảm tiền")
    void test_Points_RedeemAtCheckout_Success() {
        User user = User.builder().id(1L).rewardPoints(200).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Product product = Product.builder().id(10L).name("Labubu").singlePrice(new BigDecimal("500000")).build();
        CartItem cartItem = CartItem.builder().id(1L).user(user).product(product).quantity(1).purchaseType("SINGLE_BOX").isSelected(true).build();
        when(cartItemRepository.findByUserIdAndIsSelectedTrue(1L)).thenReturn(List.of(cartItem));
        when(productRepository.updateStock(eq(10L), eq(1))).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        // Subtotal = 500.000 đ. 20% = 100.000 đ -> max 1000 points.
        // User dùng 100 points = 10.000 đ giảm giá.
        Order order = orderService.createOrder(1L, "A", "0912", "HN", "CG", "DV", "123", "COD", null, 100);

        assertEquals(100, user.getRewardPoints(), "User bị trừ 100 điểm từ 200 điểm");
        assertEquals(100, order.getPointsUsed());
        assertEquals(new BigDecimal("10000"), order.getPointsDiscount());
        // Subtotal 500.000 + ship 0 (>= 500k freeship) - pointsDiscount 10.000 = 490.000 đ
        assertEquals(new BigDecimal("490000"), order.getTotalAmount());
        assertEquals(50, order.getPointsEarned());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Points Redemption: Vượt quá số điểm hiện có của khách hàng -> Ném BadRequestException")
    void test_Points_RedeemAtCheckout_ExceedsUserPoints_ThrowsBadRequest() {
        User user = User.builder().id(1L).rewardPoints(50).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Product product = Product.builder().id(10L).name("Labubu").singlePrice(new BigDecimal("500000")).build();
        CartItem cartItem = CartItem.builder().id(1L).user(user).product(product).quantity(1).purchaseType("SINGLE_BOX").isSelected(true).build();
        when(cartItemRepository.findByUserIdAndIsSelectedTrue(1L)).thenReturn(List.of(cartItem));
        when(productRepository.updateStock(eq(10L), eq(1))).thenReturn(1);

        assertThrows(BadRequestException.class, () ->
                orderService.createOrder(1L, "A", "0912", "HN", "CG", "DV", "123", "COD", null, 100));
    }

    @Test
    @DisplayName("Points Redemption: Vượt quá hạn mức tối đa 20% giá trị tiền hàng -> Ném BadRequestException")
    void test_Points_RedeemAtCheckout_Exceeds20PercentCap_ThrowsBadRequest() {
        // Subtotal = 100.000 đ -> 20% là 20.000 đ (tối đa 200 điểm)
        // User có 500 điểm và cố dùng 300 điểm (30.000 đ > 20.000 đ)
        User user = User.builder().id(1L).rewardPoints(500).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Product product = Product.builder().id(10L).name("Labubu").singlePrice(new BigDecimal("100000")).build();
        CartItem cartItem = CartItem.builder().id(1L).user(user).product(product).quantity(1).purchaseType("SINGLE_BOX").isSelected(true).build();
        when(cartItemRepository.findByUserIdAndIsSelectedTrue(1L)).thenReturn(List.of(cartItem));
        when(productRepository.updateStock(eq(10L), eq(1))).thenReturn(1);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.createOrder(1L, "A", "0912", "HN", "CG", "DV", "123", "COD", null, 300));
        assertTrue(ex.getMessage().contains("tối đa 20%"));
    }

    @Test
    @DisplayName("Points Refund: Khách hàng hủy đơn (cancelOrder) -> Hoàn trả điểm đã dùng lại cho user")
    void test_Points_RefundOnCustomerCancel() {
        User user = User.builder().id(1L).rewardPoints(50).build();
        Order order = Order.builder()
                .id(1L)
                .orderCode("PW-CANCEL-1")
                .status("TO_PAY")
                .user(user)
                .pointsUsed(100)
                .build();

        when(orderRepository.findByOrderCode("PW-CANCEL-1")).thenReturn(Optional.of(order));

        when(orderItemRepository.findByOrderId(1L)).thenReturn(Collections.emptyList());
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        orderService.cancelOrder(1L, "PW-CANCEL-1", "Đổi ý");

        assertEquals(150, user.getRewardPoints(), "User được hoàn trả 100 điểm");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Points Refund: Admin hủy đơn (adminCancelOrder) -> Hoàn trả điểm đã dùng lại cho user")
    void test_Points_RefundOnAdminCancel() {
        User user = User.builder().id(1L).rewardPoints(20).build();
        Order order = Order.builder()
                .id(2L)
                .orderCode("PW-CANCEL-2")
                .status("PROCESSING")
                .user(user)
                .pointsUsed(80)
                .build();

        when(orderRepository.findByOrderCode("PW-CANCEL-2")).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(2L)).thenReturn(Collections.emptyList());
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        orderService.adminCancelOrder("PW-CANCEL-2", "Hết hàng");

        assertEquals(100, user.getRewardPoints(), "User được hoàn trả 80 điểm");
        verify(userRepository).save(user);
    }
}

