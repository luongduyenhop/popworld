package com.manguonmo.popworld.e2e.tier2;

import com.manguonmo.popworld.dto.request.BoxReservationRequest;
import com.manguonmo.popworld.dto.response.BoxReservationResponse;
import com.manguonmo.popworld.e2e.base.BaseE2ETest;
import com.manguonmo.popworld.entity.*;
import com.manguonmo.popworld.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tier 2 - State Machine and Transition Invariants E2E Tests")
public class Tier2StateTransitionsE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("T2-TRANS-01: Chống hủy đơn hàng 2 lần (Double Cancel Prevention) - Ném BadRequestException")
    void whenOrderAlreadyCancelled_subsequentCancelShouldFail() {
        User user = createTestUser("double_cancel_user_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");
        Product product = createTestProduct("Transition Toy", 10, BigDecimal.valueOf(100000), null);

        cartService.addToCart(user.getId(), product.getId(), "SINGLE_BOX", 1);
        Order order = orderService.createOrder(
                user.getId(), "Nguyen Van A", "0987654321",
                "Ha Noi", "Cau Giay", "Dich Vong", "123 Xuan Thuy",
                "COD", null
        );

        // Hủy lần 1 thành công
        Order cancelledOrder = orderService.cancelOrder(user.getId(), order.getOrderCode(), "Hủy lần 1");
        assertEquals("CANCELLED", cancelledOrder.getStatus());

        // Hủy lần 2 phải ném BadRequestException
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.cancelOrder(user.getId(), order.getOrderCode(), "Hủy lần 2")
        );
        assertTrue(ex.getMessage().contains("đã bị hủy từ trước"));
    }

    @Test
    @DisplayName("T2-TRANS-02: Đơn hàng đã EXPIRED không thể hủy - Ném BadRequestException")
    void whenOrderExpired_cancelShouldFail() {
        User user = createTestUser("expired_order_user_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");
        Product product = createTestProduct("Expired Order Toy", 10, BigDecimal.valueOf(100000), null);

        cartService.addToCart(user.getId(), product.getId(), "SINGLE_BOX", 1);
        Order order = orderService.createOrder(
                user.getId(), "Nguyen Van A", "0987654321",
                "Ha Noi", "Cau Giay", "Dich Vong", "123 Xuan Thuy",
                "COD", null
        );

        // Đổi trạng thái sang EXPIRED
        order.setStatus("EXPIRED");
        orderRepository.save(order);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.cancelOrder(user.getId(), order.getOrderCode(), "Hủy đơn hết hạn")
        );
        assertTrue(ex.getMessage().contains("đã hết hạn thanh toán từ trước"));
    }

    @Test
    @DisplayName("T2-TRANS-03: Khách hàng không thể hủy đơn hàng ở trạng thái PROCESSING hoặc SHIPPING")
    void whenOrderProcessingOrShipping_customerCannotCancel() {
        User user = createTestUser("processing_cancel_user_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");
        Product product = createTestProduct("Shipping Toy", 10, BigDecimal.valueOf(100000), null);

        cartService.addToCart(user.getId(), product.getId(), "SINGLE_BOX", 1);
        Order order = orderService.createOrder(
                user.getId(), "Nguyen Van A", "0987654321",
                "Ha Noi", "Cau Giay", "Dich Vong", "123 Xuan Thuy",
                "COD", null
        );

        order.setStatus("PROCESSING");
        orderRepository.save(order);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.cancelOrder(user.getId(), order.getOrderCode(), "Khách hủy khi đang xử lý")
        );
        assertTrue(ex.getMessage().contains("Chờ thanh toán (TO_PAY)"));
    }

    @Test
    @DisplayName("T2-TRANS-04: Đóng gói đơn hàng không hợp lệ (Chỉ cho phép khi đơn ở trạng thái PROCESSING)")
    void whenPackingOrderNotInProcessing_shouldFail() {
        User user = createTestUser("pack_invalid_user_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");
        Product product = createTestProduct("Pack Toy", 10, BigDecimal.valueOf(100000), null);

        cartService.addToCart(user.getId(), product.getId(), "SINGLE_BOX", 1);
        Order order = orderService.createOrder(
                user.getId(), "Nguyen Van A", "0987654321",
                "Ha Noi", "Cau Giay", "Dich Vong", "123 Xuan Thuy",
                "COD", null
        );

        // Đơn đang ở TO_PAY -> admin đóng gói phải bị từ chối
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.packOrder(order.getOrderCode(), "Admin", "Đóng gói sớm")
        );
        assertTrue(ex.getMessage().contains("Chờ xử lý (PROCESSING)"));
    }

    @Test
    @DisplayName("T2-TRANS-05: Giao hàng đơn hàng không hợp lệ (Chỉ cho phép khi PACKED hoặc PROCESSING)")
    void whenShippingOrderNotInPackedOrProcessing_shouldFail() {
        User user = createTestUser("ship_invalid_user_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");
        Product product = createTestProduct("Ship Toy", 10, BigDecimal.valueOf(100000), null);

        cartService.addToCart(user.getId(), product.getId(), "SINGLE_BOX", 1);
        Order order = orderService.createOrder(
                user.getId(), "Nguyen Van A", "0987654321",
                "Ha Noi", "Cau Giay", "Dich Vong", "123 Xuan Thuy",
                "COD", null
        );

        // Đơn đang ở TO_PAY -> admin chuyển giao hàng phải bị từ chối
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.shipOrder(order.getOrderCode())
        );
        assertTrue(ex.getMessage().contains("PACKED") || ex.getMessage().contains("PROCESSING"));
    }

    @Test
    @DisplayName("T2-TRANS-06: Hoàn tất đơn hàng không hợp lệ (Chỉ cho phép khi đơn đang SHIPPING)")
    void whenCompletingOrderNotInShipping_shouldFail() {
        User user = createTestUser("complete_invalid_user_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");
        Product product = createTestProduct("Complete Toy", 10, BigDecimal.valueOf(100000), null);

        cartService.addToCart(user.getId(), product.getId(), "SINGLE_BOX", 1);
        Order order = orderService.createOrder(
                user.getId(), "Nguyen Van A", "0987654321",
                "Ha Noi", "Cau Giay", "Dich Vong", "123 Xuan Thuy",
                "COD", null
        );

        // Đơn chưa được giao hàng mà đòi hoàn tất -> ném lỗi
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.completeOrder(order.getOrderCode())
        );
        assertTrue(ex.getMessage().contains("SHIPPING"));
    }

    @Test
    @DisplayName("T2-TRANS-07: Mở hộp POP NOW khi chưa thanh toán (RESERVED) - Từ chối với BadRequestException")
    void whenUnboxingUnpaidReservation_shouldFail() {
        User user = createTestUser("unbox_unpaid_user_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");
        Product product = createTestProduct("Unpaid Box Toy", 5, BigDecimal.valueOf(150000), null);

        BoxReservationRequest request = new BoxReservationRequest();
        request.setProductId(product.getId());
        request.setBoxIndex(1);

        BoxReservationResponse res = popNowService.reserveBox(user.getId(), request);

        // Chưa thanh toán (đang RESERVED), gọi unbox phải bị từ chối
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                popNowService.unbox(user.getId(), res.getReservationCode())
        );
        assertTrue(ex.getMessage().contains("chưa được thanh toán"));
    }

    @Test
    @DisplayName("T2-TRANS-08: Thanh toán hoặc mở hộp phiếu giữ hộp đã CANCELLED hoặc EXPIRED - Từ chối hợp lệ")
    void whenOperatingOnCancelledOrExpiredReservation_shouldFail() {
        User user = createTestUser("cancelled_res_user_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");
        Product product = createTestProduct("Cancelled Res Toy", 5, BigDecimal.valueOf(150000), null);

        BoxReservationRequest request = new BoxReservationRequest();
        request.setProductId(product.getId());
        request.setBoxIndex(1);

        BoxReservationResponse res = popNowService.reserveBox(user.getId(), request);
        popNowService.cancelReservation(user.getId(), res.getReservationCode());

        // Đã hủy -> markPurchased phải thất bại
        BadRequestException ex1 = assertThrows(BadRequestException.class, () ->
                popNowService.markPurchased(res.getReservationCode(), "PW-TEST999")
        );
        assertTrue(ex1.getMessage().contains("đã bị hủy"));

        // Đã hủy -> unbox phải thất bại
        BadRequestException ex2 = assertThrows(BadRequestException.class, () ->
                popNowService.unbox(user.getId(), res.getReservationCode())
        );
        assertTrue(ex2.getMessage().contains("không hợp lệ") || ex2.getMessage().contains("hủy") || ex2.getMessage().contains("chưa được"));
    }
}
