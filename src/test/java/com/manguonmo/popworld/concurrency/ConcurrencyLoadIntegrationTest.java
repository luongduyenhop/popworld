package com.manguonmo.popworld.concurrency;

import com.manguonmo.popworld.dto.request.BoxReservationRequest;
import com.manguonmo.popworld.dto.response.BoxReservationResponse;
import com.manguonmo.popworld.e2e.base.BaseE2ETest;
import com.manguonmo.popworld.entity.BlindBoxSlot;
import com.manguonmo.popworld.entity.Coupon;
import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.entity.SlotStatus;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.OutOfStockException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Requirement R3: Multi-threaded Concurrency & Race Condition Defense Tests")
public class ConcurrencyLoadIntegrationTest extends BaseE2ETest {

    @Test
    @DisplayName("R3-TEST-01: 10 Luồng đồng thời tranh mua 1 sản phẩm tồn kho K=1 -> Đúng 1 đơn thành công, tồn kho về 0, không âm kho")
    void concurrentStockPurchase_WhenStockIsOne_ExactlyOneShouldSucceed() throws InterruptedException {
        int threadCount = 10;
        Product product = createTestProduct("Limited Edition Labubu", 1, BigDecimal.valueOf(250000), null);

        List<User> users = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            User user = createTestUser("concurrency_user_" + i + "_" + UUID.randomUUID().toString().substring(0, 6) + "@popworld.com", "ROLE_USER");
            users.add(user);
            cartService.addToCart(user.getId(), product.getId(), "SINGLE_BOX", 1);
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<String> orderCodes = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final User threadUser = users.get(i);
            executor.submit(() -> {
                try {
                    startLatch.await(); // Đảm bảo toàn bộ 10 luồng xuất phát tại cùng 1 mili-giây
                    Order order = orderService.createOrder(
                            threadUser.getId(),
                            threadUser.getFullName(),
                            "0987654321",
                            "Hồ Chí Minh", "Quận 1", "Bến Nghé", "123 Lê Lợi",
                            "COD", null
                    );
                    if (order != null && order.getId() != null) {
                        successCount.incrementAndGet();
                        orderCodes.add(order.getOrderCode());
                    }
                } catch (OutOfStockException ex) {
                    failCount.incrementAndGet();
                } catch (Exception ex) {
                    failCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Kích hoạt đồng loạt 10 luồng
        boolean completed = endLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Toàn bộ luồng phải hoàn thành trong 15 giây");
        assertEquals(1, successCount.get(), "Chính xác duy nhất 1 đơn hàng được đặt thành công khi tồn kho = 1");
        assertEquals(threadCount - 1, failCount.get(), "Chính xác 9 luồng còn lại phải bị từ chối do hết hàng");

        Product refreshed = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(0, refreshed.getStockQuantity(), "Tồn kho sau tranh mua phải bằng 0 (Tuyệt đối không âm kho: stock >= 0)");
    }

    @Test
    @DisplayName("R3-TEST-02: 5 Luồng đồng thời tranh bóc cùng 1 slot Blind Box POP NOW -> Đúng 1 luồng giữ thành công")
    void concurrentPopNowBlindBoxReservation_SameSlot_ExactlyOneWins() throws InterruptedException {
        int threadCount = 5;
        Product product = createTestProduct("POP NOW Dimoo Series", 10, BigDecimal.valueOf(300000), null);
        int targetSlotIndex = 4;

        List<User> users = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            users.add(createTestUser("popnow_racer_" + i + "_" + UUID.randomUUID().toString().substring(0, 6) + "@popworld.com", "ROLE_USER"));
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<String> reservationCodes = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final User threadUser = users.get(i);
            executor.submit(() -> {
                try {
                    startLatch.await();
                    BoxReservationRequest req = new BoxReservationRequest();
                    req.setProductId(product.getId());
                    req.setBoxIndex(targetSlotIndex);

                    BoxReservationResponse res = popNowService.reserveBox(threadUser.getId(), req);
                    if (res != null && res.getReservationCode() != null) {
                        successCount.incrementAndGet();
                        reservationCodes.add(res.getReservationCode());
                    }
                } catch (BadRequestException ex) {
                    failCount.incrementAndGet();
                } catch (Exception ex) {
                    failCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Toàn bộ luồng bóc hộp phải hoàn thành trong 15 giây");
        assertEquals(1, successCount.get(), "Chính xác duy nhất 1 khách hàng giữ thành công slot #4");
        assertEquals(threadCount - 1, failCount.get(), "4 khách hàng còn lại phải bị từ chối do slot đã bị giữ");

        BlindBoxSlot slot = blindBoxSlotRepository.findByProductIdAndSlotIndex(product.getId(), targetSlotIndex).orElseThrow();
        assertEquals(SlotStatus.HELD, slot.getStatus(), "Trạng thái slot mục tiêu phải là HELD");
        assertNotNull(slot.getCurrentReservation(), "Slot phải gắn đúng với phiếu giữ hộp của người chiến thắng");
    }

    @Test
    @DisplayName("R3-TEST-03: 5 Luồng từ 5 user khác nhau đồng thời dùng Coupon có usageLimit=1 -> Đúng 1 user áp dụng thành công")
    void concurrentCouponUsage_WhenLimitIsOne_ExactlyOneWins() throws InterruptedException {
        int threadCount = 5;
        Coupon coupon = createTestCoupon("RACE_VOUCHER_" + UUID.randomUUID().toString().substring(0, 6),
                "FIXED", BigDecimal.valueOf(50000), BigDecimal.valueOf(100000), null, null, null, 1);

        List<User> users = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            users.add(createTestUser("coupon_racer_" + i + "_" + UUID.randomUUID().toString().substring(0, 6) + "@popworld.com", "ROLE_USER"));
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final User threadUser = users.get(i);
            executor.submit(() -> {
                try {
                    startLatch.await();
                    Coupon applied = couponService.applyCoupon(coupon.getCode(), threadUser.getId(), BigDecimal.valueOf(200000));
                    if (applied != null) {
                        successCount.incrementAndGet();
                    }
                } catch (BadRequestException ex) {
                    failCount.incrementAndGet();
                } catch (Exception ex) {
                    failCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Toàn bộ luồng áp dụng mã phải hoàn thành trong 15 giây");
        assertEquals(1, successCount.get(), "Chính xác duy nhất 1 user áp dụng thành công mã có usageLimit=1");
        assertEquals(threadCount - 1, failCount.get(), "4 user còn lại phải nhận thông báo mã đã hết lượt");

        Coupon refreshed = couponRepository.findById(coupon.getId()).orElseThrow();
        assertEquals(1, refreshed.getUsedCount(), "usedCount của coupon trong CSDL phải bằng chính xác 1");
    }
}
