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
 * 3. Bất biến trạng thái: RESERVED không được unbox, EXPIRED/CANCELLED không được mua/unbox
 * 4. Không âm kho và rollback an toàn
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
                // Đã bị luồng 1 chuyển thành HELD
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

        startLatch.countDown(); // Khởi chạy đồng thời

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
                dbRowLock.unlock(); // Commit luồng 1
            }
            return createdItem;
        });

        when(ownedItemRepository.findByReservationId(501L)).thenAnswer(inv -> {
            if (dbRowLock.isHeldByCurrentThread()) {
                dbRowLock.unlock(); // Commit luồng 2
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

        // Đảm bảo chỉ gọi save OwnedItem duy nhất 1 lần
        verify(ownedItemRepository, times(1)).save(any(OwnedItem.class));
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

        when(boxReservationRepository.findByReservationCode("PN-CANCEL-001")).thenReturn(Optional.of(cancelled));
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

        when(boxReservationRepository.findByReservationCode("PN-EXPIRED-001")).thenReturn(Optional.of(expired));
        assertThrows(BadRequestException.class, () -> popNowService.markPurchased("PN-EXPIRED-001", "ORD-2"));

        when(boxReservationRepository.findByReservationCodeForUpdate("PN-EXPIRED-001")).thenReturn(Optional.of(expired));
        assertThrows(BadRequestException.class, () -> popNowService.unbox(1L, "PN-EXPIRED-001"));
    }
}
