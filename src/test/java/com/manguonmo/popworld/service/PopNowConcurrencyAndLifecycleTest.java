package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.request.BoxReservationRequest;
import com.manguonmo.popworld.dto.response.BoxReservationResponse;
import com.manguonmo.popworld.dto.response.OwnedItemResponse;
import com.manguonmo.popworld.entity.*;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.repository.*;
import com.manguonmo.popworld.service.impl.PopNowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * POP NOW Concurrency, Invariants & State Machine Regression Tests
 *
 * Kiểm thử chuyên sâu theo benchmark POP MART:
 * 1. Slot exclusivity dưới đa luồng đồng thời (chống tranh chấp cùng 1 slot)
 * 2. Chống double unbox đồng thời (tuần tự hóa và tính lũy đẳng)
 * 3. Race condition Cancel vs Expiry: Hoàn tồn kho và mở slot đúng 1 lần duy nhất
 * 4. Race condition Cancel vs Payment: Không hủy và không hoàn kho khi đã thanh toán
 * 5. Race condition Expiry vs Payment: Từ chối thanh toán khi đã hết hạn
 * 6. Webhook thanh toán lặp lại (Duplicate Payment Callback Idempotency)
 * 7. Bất biến trạng thái: RESERVED không được unbox, EXPIRED/CANCELLED không được mua/unbox
 */
@ExtendWith(MockitoExtension.class)
class PopNowConcurrencyAndLifecycleTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private BlindBoxItemRepository blindBoxItemRepository;

    @Mock
    private BoxReservationRepository boxReservationRepository;

    @Mock
    private OwnedItemRepository ownedItemRepository;

    @Mock
    private BlindBoxSlotRepository blindBoxSlotRepository;

    @Mock
    private UserAddressRepository userAddressRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private PopNowServiceImpl popNowService;

    private User user1;
    private User user2;
    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        user1 = User.builder().id(1L).email("user1@test.com").fullName("User One").enabled(true).build();
        user2 = User.builder().id(2L).email("user2@test.com").fullName("User Two").enabled(true).build();
        sampleProduct = Product.builder().id(10L).name("SKULLPANDA Everyday Wonderland").active(true).singlePrice(BigDecimal.valueOf(380000)).stockQuantity(12).build();
    }

    @Test
    @DisplayName("Concurrency: Hai người dùng cùng chọn giữ một ô hộp (same slot) đồng thời -> Chỉ 1 người thành công")
    void concurrentSameSlotReservation_OnlyOneSucceeds() throws InterruptedException, ExecutionException {
        BlindBoxSlot sharedSlot = BlindBoxSlot.builder().id(30L).product(sampleProduct).slotIndex(7).status(SlotStatus.AVAILABLE).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(productRepository.findById(10L)).thenReturn(Optional.of(sampleProduct));

        // Giả lập khóa bi quan: luồng đầu tiên chiếm được slot AVAILABLE và đổi sang HELD, luồng thứ hai thấy slot đã HELD
        AtomicInteger lockAttempts = new AtomicInteger(0);
        when(blindBoxSlotRepository.findByProductIdAndSlotIndexForUpdate(10L, 7)).thenAnswer(inv -> {
            int attempt = lockAttempts.incrementAndGet();
            if (attempt == 1) {
                return Optional.of(sharedSlot); // Available
            } else {
                return Optional.of(BlindBoxSlot.builder().id(30L).product(sampleProduct).slotIndex(7).status(SlotStatus.HELD).build());
            }
        });

        when(productRepository.updateStock(10L, 1)).thenReturn(1);
        when(boxReservationRepository.save(any(BoxReservation.class))).thenAnswer(inv -> inv.getArgument(0));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Callable<BoxReservationResponse> task1 = () -> {
            startLatch.await();
            return popNowService.reserveBox(1L, BoxReservationRequest.builder().productId(10L).boxIndex(7).build());
        };

        Callable<BoxReservationResponse> task2 = () -> {
            startLatch.await();
            return popNowService.reserveBox(2L, BoxReservationRequest.builder().productId(10L).boxIndex(7).build());
        };

        Future<BoxReservationResponse> future1 = executor.submit(task1);
        Future<BoxReservationResponse> future2 = executor.submit(task2);

        startLatch.countDown();

        int successCount = 0;
        int failureCount = 0;

        try {
            BoxReservationResponse res1 = future1.get();
            if (res1 != null) successCount++;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof BadRequestException) failureCount++;
        }

        try {
            BoxReservationResponse res2 = future2.get();
            if (res2 != null) successCount++;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof BadRequestException) failureCount++;
        }

        executor.shutdown();

        assertEquals(1, successCount, "Chính xác 1 request thành công giữ ô hộp");
        assertEquals(1, failureCount, "Request còn lại phải thất bại do vị trí ô đã bị giữ");
        verify(productRepository, times(1)).updateStock(10L, 1);
    }

    @Test
    @DisplayName("Concurrency: Hai request unbox cùng 1 phiếu đồng thời -> Tuần tự hóa qua DB row lock và chỉ tạo 1 OwnedItem (Idempotency)")
    void concurrentDoubleUnbox_SerializesAndIdempotent() throws InterruptedException, ExecutionException {
        BoxReservation purchasedReservation = BoxReservation.builder()
                .id(501L)
                .user(user1)
                .product(sampleProduct)
                .reservationCode("PN-DOUBLE-UNBOX")
                .status(ReservationStatus.PURCHASED)
                .orderCode("PW-ORD-99")
                .build();

        BlindBoxItem item = BlindBoxItem.builder().id(88L).name("The Hermit").rarity(RarityType.REGULAR).probabilityWeight(100).build();
        when(blindBoxItemRepository.findByProductIdAndActiveTrue(10L)).thenReturn(List.of(item));

        OwnedItem createdItem = OwnedItem.builder()
                .id(901L)
                .user(user1)
                .product(sampleProduct)
                .blindBoxItem(item)
                .reservation(purchasedReservation)
                .status(OwnedItemStatus.IN_CABINET)
                .unboxedAt(LocalDateTime.now())
                .build();

        // Giả lập cơ chế Pessimistic Row Lock: Luồng 1 giữ lock đến khi commit, Luồng 2 phải đợi
        java.util.concurrent.locks.ReentrantLock dbRowLock = new java.util.concurrent.locks.ReentrantLock();
        when(boxReservationRepository.findByReservationCodeForUpdate("PN-DOUBLE-UNBOX")).thenAnswer(inv -> {
            dbRowLock.lock();
            return Optional.of(purchasedReservation);
        });

        when(ownedItemRepository.save(any(OwnedItem.class))).thenAnswer(inv -> {
            if (dbRowLock.isHeldByCurrentThread()) {
                dbRowLock.unlock();
            }
            return createdItem;
        });

        when(ownedItemRepository.findByReservationId(501L)).thenAnswer(inv -> {
            if (dbRowLock.isHeldByCurrentThread()) {
                dbRowLock.unlock();
            }
            return Optional.of(createdItem);
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Callable<OwnedItemResponse> call1 = () -> {
            startLatch.await();
            return popNowService.unbox(1L, "PN-DOUBLE-UNBOX");
        };

        Callable<OwnedItemResponse> call2 = () -> {
            startLatch.await();
            return popNowService.unbox(1L, "PN-DOUBLE-UNBOX");
        };

        Future<OwnedItemResponse> f1 = executor.submit(call1);
        Future<OwnedItemResponse> f2 = executor.submit(call2);

        startLatch.countDown();

        OwnedItemResponse r1 = f1.get();
        OwnedItemResponse r2 = f2.get();

        executor.shutdown();

        assertNotNull(r1);
        assertNotNull(r2);
        assertEquals(r1.getId(), r2.getId(), "Cả hai request đều trả về cùng một OwnedItem ID (Idempotent)");
        assertEquals("The Hermit", r1.getItemName());
        assertEquals("The Hermit", r2.getItemName());

        verify(ownedItemRepository, times(1)).save(any(OwnedItem.class));
    }

    @Test
    @DisplayName("Race Condition: Cancel vs Expiry Scheduler -> Khóa bi quan và chỉ hoàn tồn kho/giải phóng slot đúng 1 lần")
    void cancelVsExpiryRace_ExactlyOnceStockRestoreAndSlotRelease() throws InterruptedException, ExecutionException {
        BlindBoxSlot slot = BlindBoxSlot.builder().id(70L).product(sampleProduct).slotIndex(4).status(SlotStatus.HELD).build();
        BoxReservation res = BoxReservation.builder()
                .id(701L)
                .user(user1)
                .product(sampleProduct)
                .slot(slot)
                .boxIndex(4)
                .reservationCode("PN-RACE-EXP-CANCEL")
                .status(ReservationStatus.RESERVED)
                .expiresAt(LocalDateTime.now().minusMinutes(1)) // Quá hạn
                .build();
        slot.setCurrentReservation(res);

        // Giả lập khóa bi quan: luồng đầu tiên giữ lock, luồng sau phải đợi
        java.util.concurrent.locks.ReentrantLock lock = new java.util.concurrent.locks.ReentrantLock();
        when(boxReservationRepository.findByReservationCodeForUpdate("PN-RACE-EXP-CANCEL")).thenAnswer(inv -> {
            lock.lock();
            return Optional.of(res);
        });
        when(boxReservationRepository.findByIdForUpdate(701L)).thenAnswer(inv -> {
            lock.lock();
            return Optional.of(res);
        });

        when(boxReservationRepository.save(any(BoxReservation.class))).thenAnswer(inv -> {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
            return inv.getArgument(0);
        });

        when(boxReservationRepository.findByStatusAndExpiresAtBefore(eq(ReservationStatus.RESERVED), any(LocalDateTime.class)))
                .thenReturn(List.of(res));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Callable<String> cancelTask = () -> {
            startLatch.await();
            try {
                popNowService.cancelReservation(1L, "PN-RACE-EXP-CANCEL");
                return "CANCEL_OK";
            } catch (Exception e) {
                if (lock.isHeldByCurrentThread()) lock.unlock();
                return "CANCEL_FAIL: " + e.getMessage();
            }
        };

        Callable<Integer> expiryTask = () -> {
            startLatch.await();
            try {
                return popNowService.releaseExpiredReservations();
            } finally {
                if (lock.isHeldByCurrentThread()) lock.unlock();
            }
        };

        Future<String> f1 = executor.submit(cancelTask);
        Future<Integer> f2 = executor.submit(expiryTask);

        startLatch.countDown();

        f1.get();
        f2.get();

        executor.shutdown();

        // Bất biến: Tồn kho chỉ được hoàn đúng 1 lần duy nhất (times = 1)
        verify(productRepository, times(1)).addStock(sampleProduct.getId(), 1);
        assertEquals(SlotStatus.AVAILABLE, slot.getStatus());
        assertNull(slot.getCurrentReservation());
    }

    @Test
    @DisplayName("Race Condition: Cancel vs Payment -> Khi thanh toán thành công trước thì cancel bị chặn và không hoàn tồn kho")
    void cancelVsPaymentRace_PaymentWins_CancelRejected() {
        BlindBoxSlot slot = BlindBoxSlot.builder().id(71L).product(sampleProduct).slotIndex(5).status(SlotStatus.HELD).build();
        BoxReservation res = BoxReservation.builder()
                .id(702L)
                .user(user1)
                .product(sampleProduct)
                .slot(slot)
                .boxIndex(5)
                .reservationCode("PN-RACE-PAY-CANCEL")
                .status(ReservationStatus.RESERVED)
                .expiresAt(LocalDateTime.now().plusMinutes(4))
                .build();
        slot.setCurrentReservation(res);

        when(boxReservationRepository.findByReservationCodeForUpdate("PN-RACE-PAY-CANCEL")).thenReturn(Optional.of(res));

        // 1. Payment callback đến trước và hoàn tất thành công
        popNowService.markPurchased("PN-RACE-PAY-CANCEL", "PW-ORD-PAID-01");
        assertEquals(ReservationStatus.PURCHASED, res.getStatus());
        assertEquals(SlotStatus.SOLD, slot.getStatus());

        // 2. Khách bấm cancel sau khi đã thanh toán -> Bị chặn
        BadRequestException ex = assertThrows(BadRequestException.class, () -> popNowService.cancelReservation(1L, "PN-RACE-PAY-CANCEL"));
        assertTrue(ex.getMessage().contains("Chỉ có thể hủy phiếu giữ hộp đang ở trạng thái RESERVED"));

        // Tuyệt đối không hoàn lại tồn kho
        verify(productRepository, never()).addStock(any(), any());
    }

    @Test
    @DisplayName("Race Condition: Expiry vs Payment -> Khi phiếu đã bị Scheduler đánh dấu EXPIRED thì Payment bị từ chối")
    void expiryVsPaymentRace_ExpiredFirst_PaymentRejected() {
        BlindBoxSlot slot = BlindBoxSlot.builder().id(72L).product(sampleProduct).slotIndex(6).status(SlotStatus.HELD).build();
        BoxReservation res = BoxReservation.builder()
                .id(703L)
                .user(user1)
                .product(sampleProduct)
                .slot(slot)
                .boxIndex(6)
                .reservationCode("PN-RACE-EXP-PAY")
                .status(ReservationStatus.RESERVED)
                .expiresAt(LocalDateTime.now().minusMinutes(1)) // Đã quá hạn
                .build();
        slot.setCurrentReservation(res);

        when(boxReservationRepository.findByStatusAndExpiresAtBefore(eq(ReservationStatus.RESERVED), any(LocalDateTime.class)))
                .thenReturn(List.of(res));
        when(boxReservationRepository.findByIdForUpdate(703L)).thenReturn(Optional.of(res));
        when(boxReservationRepository.findByReservationCodeForUpdate("PN-RACE-EXP-PAY")).thenReturn(Optional.of(res));

        // 1. Scheduler giải phóng phiếu quá hạn trước
        popNowService.releaseExpiredReservations();
        assertEquals(ReservationStatus.EXPIRED, res.getStatus());
        assertEquals(SlotStatus.AVAILABLE, slot.getStatus());
        verify(productRepository, times(1)).addStock(sampleProduct.getId(), 1);

        // 2. Payment callback đến sau -> Bị từ chối
        BadRequestException ex = assertThrows(BadRequestException.class, () -> popNowService.markPurchased("PN-RACE-EXP-PAY", "PW-ORD-LATE"));
        assertTrue(ex.getMessage().contains("đã hết hạn, không thể thanh toán"));

        // Tồn kho không bị hoàn thêm lần nữa
        verify(productRepository, times(1)).addStock(sampleProduct.getId(), 1);
    }

    @Test
    @DisplayName("Idempotency: Callback thanh toán lặp lại (duplicate payment callbacks) -> Xử lý an toàn và không đổi trạng thái 2 lần")
    void duplicatePaymentCallbacks_IsIdempotent() {
        BlindBoxSlot slot = BlindBoxSlot.builder().id(73L).product(sampleProduct).slotIndex(8).status(SlotStatus.HELD).build();
        BoxReservation res = BoxReservation.builder()
                .id(704L)
                .user(user1)
                .product(sampleProduct)
                .slot(slot)
                .boxIndex(8)
                .reservationCode("PN-DUP-PAY")
                .status(ReservationStatus.RESERVED)
                .expiresAt(LocalDateTime.now().plusMinutes(3))
                .build();

        when(boxReservationRepository.findByReservationCodeForUpdate("PN-DUP-PAY")).thenReturn(Optional.of(res));

        // Lần 1: Thành công chuyển sang PURCHASED và slot SOLD
        popNowService.markPurchased("PN-DUP-PAY", "PW-ORD-DUP-01");
        assertEquals(ReservationStatus.PURCHASED, res.getStatus());
        assertEquals(SlotStatus.SOLD, slot.getStatus());
        verify(boxReservationRepository, times(1)).save(res);

        // Lần 2 (Webhook retry): Lũy đẳng, không quăng lỗi và không gọi save lại
        assertDoesNotThrow(() -> popNowService.markPurchased("PN-DUP-PAY", "PW-ORD-DUP-01"));
        assertEquals(ReservationStatus.PURCHASED, res.getStatus());
        verify(boxReservationRepository, times(1)).save(res);
    }

    @Test
    @DisplayName("State Machine: Phiếu ở trạng thái CANCELLED không được phép thanh toán hoặc mở hộp")
    void cancelledReservation_CannotBePurchasedOrUnboxed() {
        BoxReservation cancelled = BoxReservation.builder()
                .id(601L)
                .user(user1)
                .reservationCode("PN-CANCEL-001")
                .status(ReservationStatus.CANCELLED)
                .build();

        when(boxReservationRepository.findByReservationCodeForUpdate("PN-CANCEL-001")).thenReturn(Optional.of(cancelled));
        assertThrows(BadRequestException.class, () -> popNowService.markPurchased("PN-CANCEL-001", "ORD-1"));

        when(boxReservationRepository.findByReservationCodeForUpdate("PN-CANCEL-001")).thenReturn(Optional.of(cancelled));
        assertThrows(BadRequestException.class, () -> popNowService.unbox(1L, "PN-CANCEL-001"));
    }

    @Test
    @DisplayName("State Machine: Phiếu ở trạng thái EXPIRED không được phép thanh toán hoặc mở hộp")
    void expiredReservation_CannotBePurchasedOrUnboxed() {
        BoxReservation expired = BoxReservation.builder()
                .id(602L)
                .user(user1)
                .reservationCode("PN-EXPIRED-001")
                .status(ReservationStatus.EXPIRED)
                .build();

        when(boxReservationRepository.findByReservationCodeForUpdate("PN-EXPIRED-001")).thenReturn(Optional.of(expired));
        assertThrows(BadRequestException.class, () -> popNowService.markPurchased("PN-EXPIRED-001", "ORD-2"));

        when(boxReservationRepository.findByReservationCodeForUpdate("PN-EXPIRED-001")).thenReturn(Optional.of(expired));
        assertThrows(BadRequestException.class, () -> popNowService.unbox(1L, "PN-EXPIRED-001"));
    }

    @Test
    @DisplayName("Race condition: Yêu cầu giao hàng đồng thời cho cùng 1 mô hình -> Đúng 1 đơn hàng được tạo (Exactly-once / Idempotency)")
    void concurrentShipNow_SameOwnedItem_ExactlyOneOrderCreated() throws InterruptedException {
        UserAddress address = UserAddress.builder()
                .id(100L)
                .user(user1)
                .recipientName("User One")
                .recipientPhone("0901234567")
                .provinceCity("HCM")
                .district("Q1")
                .detailedAddress("123 Le Loi")
                .build();

        BlindBoxItem bbItem = BlindBoxItem.builder().id(50L).name("Dimoo").rarity(RarityType.REGULAR).build();
        OwnedItem item = OwnedItem.builder()
                .id(200L)
                .user(user1)
                .product(sampleProduct)
                .blindBoxItem(bbItem)
                .status(OwnedItemStatus.IN_CABINET)
                .unboxedAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userAddressRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(address));

        // Giả lập tuần tự hóa của khóa bi quan: Luồng đầu tiên lấy được vật phẩm IN_CABINET, luồng tiếp theo thấy trạng thái đã chuyển sang REQUESTED_SHIPPING
        AtomicInteger lockAttempts = new AtomicInteger(0);
        when(ownedItemRepository.findByIdForUpdate(200L)).thenAnswer(inv -> {
            int attempt = lockAttempts.incrementAndGet();
            if (attempt == 1) {
                return Optional.of(item);
            } else {
                return Optional.of(OwnedItem.builder()
                        .id(200L)
                        .user(user1)
                        .product(sampleProduct)
                        .blindBoxItem(bbItem)
                        .status(OwnedItemStatus.REQUESTED_SHIPPING)
                        .build());
            }
        });

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(888L);
            return o;
        });

        int numThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CyclicBarrier barrier = new CyclicBarrier(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    barrier.await();
                    popNowService.requestShipment(1L, 100L, List.of(200L));
                    successCount.incrementAndGet();
                } catch (BadRequestException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    // unexpected error
                }
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        // Đúng 1 luồng thành công tạo đơn, luồng còn lại bị từ chối do trạng thái không còn IN_CABINET
        assertEquals(1, successCount.get(), "Chỉ duy nhất 1 đơn hàng giao vận được tạo");
        assertEquals(1, failureCount.get(), "Yêu cầu thứ 2 bị từ chối an toàn");
        assertEquals(OwnedItemStatus.REQUESTED_SHIPPING, item.getStatus());
        verify(orderRepository, times(1)).save(any(Order.class));
    }
}
