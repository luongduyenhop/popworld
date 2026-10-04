package com.manguonmo.popworld.e2e.tier1;

import com.manguonmo.popworld.dto.request.BoxReservationRequest;
import com.manguonmo.popworld.dto.response.BoxReservationResponse;
import com.manguonmo.popworld.dto.response.CouponDiscountResponse;
import com.manguonmo.popworld.e2e.base.BaseE2ETest;
import com.manguonmo.popworld.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tier 1 - Core Feature Coverage E2E Tests (Stock, Blind Box, Coupons)")
public class Tier1FeatureCoverageE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("T1-FEAT-01: Khấu trừ tồn kho chính xác khi tạo đơn hàng thành công")
    void whenOrderCreated_stockShouldBeDeductedCleanly() {
        User user = createTestUser("stock_user_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");
        Product product = createTestProduct("Stock Test Toy", 10, BigDecimal.valueOf(120000), null);

        cartService.addToCart(user.getId(), product.getId(), "SINGLE_BOX", 3);
        Order order = orderService.createOrder(
                user.getId(),
                "Nguyen Van A",
                "0987654321",
                "Ha Noi",
                "Cau Giay",
                "Dich Vong",
                "123 Xuan Thuy",
                "COD",
                null
        );

        assertNotNull(order);
        assertEquals("TO_PAY", order.getStatus());

        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(7, updatedProduct.getStockQuantity(), "Tồn kho ban đầu là 10, mua 3 thì tồn kho phải giảm còn 7");
    }

    @Test
    @DisplayName("T1-FEAT-02: Giữ hộp POP NOW thành công - Khóa vị trí slot HELD và giảm 1 tồn kho")
    void whenReserveBox_slotShouldBeHeldAndStockDeducted() {
        User user = createTestUser("popnow_user_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");
        Product product = createTestProduct("PopNow Series Box", 8, BigDecimal.valueOf(200000), null);

        BoxReservationRequest request = new BoxReservationRequest();
        request.setProductId(product.getId());
        request.setBoxIndex(3);

        BoxReservationResponse response = popNowService.reserveBox(user.getId(), request);

        assertNotNull(response);
        assertNotNull(response.getReservationCode());
        assertEquals(ReservationStatus.RESERVED.name(), response.getStatus());
        assertEquals(3, response.getBoxIndex());

        // Kiểm tra tồn kho đã bị trừ 1
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(7, updatedProduct.getStockQuantity(), "Tồn kho ban đầu là 8, giữ 1 hộp thì tồn kho phải còn 7");

        // Kiểm tra slot trong DB chuyển sang HELD
        BlindBoxSlot slot = blindBoxSlotRepository.findByProductIdAndSlotIndex(product.getId(), 3).orElse(null);
        assertNotNull(slot);
        assertEquals(SlotStatus.HELD, slot.getStatus());
    }

    @Test
    @DisplayName("T1-FEAT-03: Áp dụng mã giảm giá PERCENT - Giảm theo % và áp trần tối đa (Max Discount)")
    void whenCouponPercentApplied_shouldCalculateCorrectDiscountWithCap() {
        User user = createTestUser("coupon_user_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");
        String couponCode = "DISC10-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        // Coupon 10%, trần tối đa 50,000 VND
        Coupon coupon = createTestCoupon(
                couponCode,
                "PERCENT",
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(50000),
                LocalDate.now().minusDays(1),
                LocalDate.now().plusMonths(1),
                50
        );

        // Trường hợp 1: Đơn 300,000 -> 10% là 30,000 (< 50,000 cap)
        CouponDiscountResponse res1 = couponService.calculateDiscount(couponCode, user.getId(), BigDecimal.valueOf(300000));
        assertEquals(0, BigDecimal.valueOf(30000).compareTo(res1.getDiscountAmount()));
        assertEquals(0, BigDecimal.valueOf(270000).compareTo(res1.getNewTotal()));

        // Trường hợp 2: Đơn 800,000 -> 10% là 80,000 (> 50,000 cap) -> Áp trần 50,000
        CouponDiscountResponse res2 = couponService.calculateDiscount(couponCode, user.getId(), BigDecimal.valueOf(800000));
        assertEquals(0, BigDecimal.valueOf(50000).compareTo(res2.getDiscountAmount()));
        assertEquals(0, BigDecimal.valueOf(750000).compareTo(res2.getNewTotal()));
    }

    @Test
    @DisplayName("T1-FEAT-04: Áp dụng mã giảm giá FIXED - Trừ đúng số tiền cố định khi đạt đơn tối thiểu")
    void whenCouponFixedApplied_shouldDeductFixedAmount() {
        User user = createTestUser("fixed_user_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");
        String couponCode = "FIXED50K-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Coupon coupon = createTestCoupon(
                couponCode,
                "FIXED",
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(200000),
                null,
                LocalDate.now().minusDays(1),
                LocalDate.now().plusMonths(1),
                50
        );

        CouponDiscountResponse res = couponService.calculateDiscount(couponCode, user.getId(), BigDecimal.valueOf(350000));
        assertEquals(0, BigDecimal.valueOf(50000).compareTo(res.getDiscountAmount()));
        assertEquals(0, BigDecimal.valueOf(300000).compareTo(res.getNewTotal()));
    }
}
