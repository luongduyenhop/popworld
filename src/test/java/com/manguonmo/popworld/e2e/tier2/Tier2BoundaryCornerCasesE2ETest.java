package com.manguonmo.popworld.e2e.tier2;

import com.manguonmo.popworld.dto.request.BoxReservationRequest;
import com.manguonmo.popworld.e2e.base.BaseE2ETest;
import com.manguonmo.popworld.entity.Coupon;
import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.OutOfStockException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tier 2 - Boundary and Corner Cases E2E Tests")
public class Tier2BoundaryCornerCasesE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("T2-BOUND-01: Tồn kho bằng 0 - Từ chối thêm vào giỏ và từ chối giữ hộp Blind Box")
    void whenStockIsZero_shouldRejectPurchaseAndReservation() {
        User user = createTestUser("zero_stock_user_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");
        Product product = createTestProduct("Zero Stock Toy", 0, BigDecimal.valueOf(100000), null);

        // 1. Thêm vào giỏ phải ném OutOfStockException
        assertThrows(OutOfStockException.class, () ->
                cartService.addToCart(user.getId(), product.getId(), "SINGLE_BOX", 1)
        );

        // 2. Giữ hộp POP NOW phải ném BadRequestException báo hết hàng
        BoxReservationRequest request = new BoxReservationRequest();
        request.setProductId(product.getId());
        request.setBoxIndex(1);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                popNowService.reserveBox(user.getId(), request)
        );
        assertTrue(ex.getMessage().contains("hết hàng"));
    }

    @Test
    @DisplayName("T2-BOUND-02: Yêu cầu số lượng lớn hơn tồn kho thực tế - Ném OutOfStockException")
    void whenRequestedQuantityExceedsStock_shouldThrowOutOfStockException() {
        User user = createTestUser("exceed_stock_user_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");
        Product product = createTestProduct("Limited Stock Toy", 2, BigDecimal.valueOf(100000), null);

        // Yêu cầu mua 3 hộp trong khi tồn chỉ còn 2
        OutOfStockException ex = assertThrows(OutOfStockException.class, () ->
                cartService.addToCart(user.getId(), product.getId(), "SINGLE_BOX", 3)
        );
        assertTrue(ex.getMessage().contains("không đủ số lượng tồn kho"));
    }

    @Test
    @DisplayName("T2-BOUND-03: Số lượng mua <= 0 - Từ chối với BadRequestException")
    void whenQuantityIsZeroOrNegative_shouldThrowBadRequestException() {
        User user = createTestUser("negative_qty_user_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");
        Product product = createTestProduct("Valid Toy", 10, BigDecimal.valueOf(100000), null);

        assertThrows(BadRequestException.class, () ->
                cartService.addToCart(user.getId(), product.getId(), "SINGLE_BOX", 0)
        );

        assertThrows(BadRequestException.class, () ->
                cartService.addToCart(user.getId(), product.getId(), "SINGLE_BOX", -5)
        );
    }

    @Test
    @DisplayName("T2-BOUND-04: Sản phẩm đang tạm dừng mở bán (active = false) - Từ chối đặt hàng và giữ hộp")
    void whenProductInactive_shouldRejectOperations() {
        User user = createTestUser("inactive_prod_user_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");
        Product product = createTestProduct("Inactive Toy", 10, BigDecimal.valueOf(100000), null);
        product.setActive(false);
        productRepository.save(product);

        assertThrows(BadRequestException.class, () ->
                cartService.addToCart(user.getId(), product.getId(), "SINGLE_BOX", 1)
        );

        BoxReservationRequest request = new BoxReservationRequest();
        request.setProductId(product.getId());
        request.setBoxIndex(1);

        assertThrows(BadRequestException.class, () ->
                popNowService.reserveBox(user.getId(), request)
        );
    }

    @Test
    @DisplayName("T2-BOUND-05: Mã giảm giá đã hết hạn sử dụng - Từ chối với thông báo hết hạn")
    void whenCouponExpired_shouldThrowBadRequestException() {
        User user = createTestUser("expired_coupon_user_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");
        String code = "EXPIRED-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Coupon coupon = createTestCoupon(
                code,
                "PERCENT",
                BigDecimal.valueOf(15),
                BigDecimal.valueOf(50000),
                null,
                LocalDate.now().minusMonths(2),
                LocalDate.now().minusDays(1), // Hết hạn hôm qua
                100
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                couponService.calculateDiscount(code, user.getId(), BigDecimal.valueOf(200000))
        );
        assertTrue(ex.getMessage().contains("hết hạn sử dụng"));
    }

    @Test
    @DisplayName("T2-BOUND-06: Mã giảm giá chưa đến ngày có hiệu lực - Từ chối với thông báo chưa hiệu lực")
    void whenCouponNotYetActive_shouldThrowBadRequestException() {
        User user = createTestUser("future_coupon_user_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");
        String code = "FUTURE-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Coupon coupon = createTestCoupon(
                code,
                "PERCENT",
                BigDecimal.valueOf(15),
                BigDecimal.valueOf(50000),
                null,
                LocalDate.now().plusDays(2), // Bắt đầu sau 2 ngày
                LocalDate.now().plusMonths(1),
                100
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                couponService.calculateDiscount(code, user.getId(), BigDecimal.valueOf(200000))
        );
        assertTrue(ex.getMessage().contains("chưa đến ngày có hiệu lực"));
    }

    @Test
    @DisplayName("T2-BOUND-07: Mã giảm giá đã hết lượt sử dụng (usageLimit reached) - Từ chối với thông báo hết lượt")
    void whenCouponUsageLimitReached_shouldThrowBadRequestException() {
        User user = createTestUser("limit_coupon_user_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");
        String code = "LIMIT0-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Coupon coupon = createTestCoupon(
                code,
                "FIXED",
                BigDecimal.valueOf(20000),
                BigDecimal.valueOf(50000),
                null,
                LocalDate.now().minusDays(1),
                LocalDate.now().plusMonths(1),
                10
        );
        coupon.setUsedCount(10); // Đã dùng hết 10/10 lượt
        couponRepository.save(coupon);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                couponService.calculateDiscount(code, user.getId(), BigDecimal.valueOf(200000))
        );
        assertTrue(ex.getMessage().contains("hết lượt sử dụng"));
    }

    @Test
    @DisplayName("T2-BOUND-08: Đơn hàng chưa đạt giá trị tối thiểu của mã giảm giá - Từ chối với thông báo yêu cầu tối thiểu")
    void whenSubtotalBelowMinOrderAmount_shouldThrowBadRequestException() {
        User user = createTestUser("min_coupon_user_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");
        String code = "MIN500K-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Coupon coupon = createTestCoupon(
                code,
                "PERCENT",
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(500000), // Yêu cầu đơn 500,000 VND
                null,
                LocalDate.now().minusDays(1),
                LocalDate.now().plusMonths(1),
                100
        );

        // Subtotal chỉ có 300,000 VND (< 500,000)
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                couponService.calculateDiscount(code, user.getId(), BigDecimal.valueOf(300000))
        );
        assertTrue(ex.getMessage().contains("chưa đạt giá trị tối thiểu"));
    }

    @Test
    @DisplayName("T2-BOUND-09: Mã giảm giá rỗng hoặc tổng tiền <= 0 - Từ chối hợp lệ")
    void whenCouponInputInvalid_shouldThrowBadRequestException() {
        User user = createTestUser("invalid_input_user_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");

        assertThrows(BadRequestException.class, () ->
                couponService.calculateDiscount("", user.getId(), BigDecimal.valueOf(100000))
        );

        assertThrows(BadRequestException.class, () ->
                couponService.calculateDiscount(null, user.getId(), BigDecimal.valueOf(100000))
        );

        assertThrows(BadRequestException.class, () ->
                couponService.calculateDiscount("SOMECODE", user.getId(), BigDecimal.ZERO)
        );
    }
}
