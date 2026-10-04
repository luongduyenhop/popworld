package com.manguonmo.popworld.e2e.tier3;

import com.manguonmo.popworld.dto.request.BoxReservationRequest;
import com.manguonmo.popworld.dto.response.BoxReservationResponse;
import com.manguonmo.popworld.dto.response.OwnedItemResponse;
import com.manguonmo.popworld.e2e.base.BaseE2ETest;
import com.manguonmo.popworld.entity.*;
import com.manguonmo.popworld.exception.OutOfStockException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tier 3 - Cross-Feature Combinations E2E Tests")
public class Tier3CrossFeatureCombinationsE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("T3-COMB-01: Đặt hàng kết hợp mã giảm giá + Tồn kho thấp -> Hủy đơn khôi phục chính xác cả tồn kho và mã giảm giá")
    void whenCheckoutWithCouponAndLowStock_thenCancelShouldRestoreBoth() {
        User user1 = createTestUser("combo1_u1_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");
        User user2 = createTestUser("combo1_u2_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");

        Product lowStockProduct = createTestProduct("Low Stock Exclusive Toy", 2, BigDecimal.valueOf(250000), null);
        String couponCode = "COMBO20-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Coupon coupon = createTestCoupon(
                couponCode,
                "PERCENT",
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(200000),
                null,
                LocalDate.now().minusDays(1),
                LocalDate.now().plusMonths(1),
                1
        );

        // 1. User 1 thêm 2 hộp (toàn bộ tồn kho) vào giỏ và đặt hàng có mã giảm giá
        cartService.addToCart(user1.getId(), lowStockProduct.getId(), "SINGLE_BOX", 2);
        Order order = orderService.createOrder(
                user1.getId(), "User 1", "0987654321", "Ha Noi", "Cau Giay", "Dich Vong", "123 Xuan Thuy",
                "COD", couponCode
        );

        assertNotNull(order);
        assertEquals("TO_PAY", order.getStatus());

        // Kiểm tra tồn kho đã về 0
        Product productAfterOrder = productRepository.findById(lowStockProduct.getId()).orElseThrow();
        assertEquals(0, productAfterOrder.getStockQuantity(), "Tồn kho phải giảm về 0");

        // Kiểm tra coupon usedCount tăng lên 1
        Coupon couponAfterOrder = couponRepository.findById(coupon.getId()).orElseThrow();
        assertEquals(1, couponAfterOrder.getUsedCount());

        // User 2 cố gắng mua sản phẩm này -> phải bị từ chối OutOfStock
        assertThrows(OutOfStockException.class, () ->
                cartService.addToCart(user2.getId(), lowStockProduct.getId(), "SINGLE_BOX", 1)
        );

        // 2. Hủy đơn hàng có coupon: Kiểm tra tồn kho phải hoàn lại 2, coupon usedCount phải giảm về 0
        // (Lưu ý: adminCancelOrder kiểm tra trọn vẹn luồng phục hồi cả kho và coupon)
        orderService.adminCancelOrder(order.getOrderCode(), "Đổi ý không mua nữa", "admin_user");

        Product productAfterCancel = productRepository.findById(lowStockProduct.getId()).orElseThrow();
        assertEquals(2, productAfterCancel.getStockQuantity(), "Tồn kho phải được hoàn lại đúng bằng 2");

        Coupon couponAfterCancel = couponRepository.findById(coupon.getId()).orElseThrow();
        assertEquals(0, couponAfterCancel.getUsedCount(), "Lượt dùng coupon phải được hoàn lại về 0");

        // Sau khi hoàn mã, User 1 có thể sử dụng lại mã này thành công
        var discountRes = couponService.calculateDiscount(couponCode, user1.getId(), BigDecimal.valueOf(300000));
        assertNotNull(discountRes);
        assertEquals(0, BigDecimal.valueOf(60000).compareTo(discountRes.getDiscountAmount()));
    }

    @Test
    @DisplayName("T3-COMB-02: Giữ hộp POP NOW -> Khởi tạo đơn thanh toán -> Xác nhận thanh toán -> Mở hộp (Unbox Idempotent)")
    void whenBlindBoxReserved_checkoutToOrderAndUnbox_shouldFlowSmoothly() {
        User user = createTestUser("blindbox_flow_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");
        Product product = createTestProduct("Molly Space Blind Box", 6, BigDecimal.valueOf(250000), null);

        BlindBoxItem item = createTestBlindBoxItem(product, "Molly Astronaut", RarityType.REGULAR, 100);

        // 1. Giữ hộp vị trí số 2
        BoxReservationRequest reserveReq = new BoxReservationRequest();
        reserveReq.setProductId(product.getId());
        reserveReq.setBoxIndex(2);

        BoxReservationResponse reservation = popNowService.reserveBox(user.getId(), reserveReq);
        assertNotNull(reservation);
        String resCode = reservation.getReservationCode();

        // Kiểm tra tồn kho bị trừ 1 (còn 5)
        Product prod1 = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(5, prod1.getStockQuantity());

        // 2. Khởi tạo đơn hàng thanh toán cho phiếu giữ hộp
        Order order = orderService.createOrderForReservation(user.getId(), resCode, "SEPAY");
        assertNotNull(order);
        assertEquals("POP_NOW_CABINET", order.getDeliveryMethod());
        assertEquals("TO_PAY", order.getStatus());

        // Kiểm tra tồn kho KHÔNG bị trừ kép (vẫn giữ nguyên 5)
        Product prod2 = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(5, prod2.getStockQuantity());

        // 3. Giả lập thanh toán thành công (markPurchased)
        popNowService.markPurchased(resCode, order.getOrderCode());

        BoxReservation updatedRes = boxReservationRepository.findByReservationCode(resCode).orElseThrow();
        assertEquals(ReservationStatus.PURCHASED, updatedRes.getStatus());
        BlindBoxSlot slot = blindBoxSlotRepository.findByProductIdAndSlotIndex(product.getId(), 2).orElseThrow();
        assertEquals(SlotStatus.SOLD, slot.getStatus());

        // 4. Mở hộp online (Unbox)
        OwnedItemResponse unboxRes = popNowService.unbox(user.getId(), resCode);
        assertNotNull(unboxRes);
        assertEquals(item.getName(), unboxRes.getItemName());

        BoxReservation unboxedRes = boxReservationRepository.findByReservationCode(resCode).orElseThrow();
        assertEquals(ReservationStatus.UNBOXED, unboxedRes.getStatus());

        // 5. Kiểm tra tính lũy đẳng (Idempotent Unbox): Mở lại trả về đúng vật phẩm đã mở, không sinh trùng
        OwnedItemResponse secondUnboxRes = popNowService.unbox(user.getId(), resCode);
        assertEquals(unboxRes.getId(), secondUnboxRes.getId());
        assertEquals(unboxRes.getItemName(), secondUnboxRes.getItemName());
    }

    @Test
    @DisplayName("T3-COMB-03: Giỏ hàng nhiều món + Chọn một phần (Partial Selection) -> Chỉ đặt các món đã chọn, món chưa chọn vẫn nằm trong giỏ")
    void whenMultiItemCartWithPartialSelection_shouldOnlyOrderSelectedItems() {
        User user = createTestUser("multi_cart_user_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");

        Product productA = createTestProduct("Toy A", 10, BigDecimal.valueOf(200000), null);
        Product productB = createTestProduct("Toy B", 10, BigDecimal.valueOf(350000), null);

        // Thêm cả 2 món vào giỏ
        CartItem itemA = cartService.addToCart(user.getId(), productA.getId(), "SINGLE_BOX", 1);
        CartItem itemB = cartService.addToCart(user.getId(), productB.getId(), "SINGLE_BOX", 1);

        // Bỏ chọn Item B, chỉ chọn Item A
        cartService.updateSelection(user.getId(), itemB.getId(), false);

        // Tính tiền chọn hiện tại: chỉ gồm Item A = 200,000 VND (< 500,000 -> phí ship 30,000)
        BigDecimal selectedTotal = cartService.calculateSelectedTotal(user.getId());
        assertEquals(0, BigDecimal.valueOf(200000).compareTo(selectedTotal));

        // Đặt hàng
        Order order = orderService.createOrder(
                user.getId(), "Nguyen Van A", "0987654321", "Ha Noi", "Cau Giay", "Dich Vong", "123 Xuan Thuy",
                "COD", null
        );

        assertNotNull(order);
        // Tổng tiền = 200,000 (hàng) + 30,000 (ship) = 230,000 VND
        assertEquals(0, BigDecimal.valueOf(230000).compareTo(order.getTotalAmount()));

        // Kiểm tra giỏ hàng sau khi đặt:
        // Item A (đã chọn) phải bị xóa khỏi giỏ
        // Item B (chưa chọn) phải VẪN CÒN trong giỏ của người dùng
        List<CartItem> remainingCart = cartService.getCartItems(user.getId());
        assertEquals(1, remainingCart.size(), "Giỏ hàng phải còn lại đúng 1 món chưa được chọn");
        assertEquals(productB.getId(), remainingCart.get(0).getProduct().getId(), "Món còn lại trong giỏ phải là Toy B");
    }
}
