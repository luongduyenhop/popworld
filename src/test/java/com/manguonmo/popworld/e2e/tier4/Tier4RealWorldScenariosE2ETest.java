package com.manguonmo.popworld.e2e.tier4;

import com.manguonmo.popworld.dto.request.BoxReservationRequest;
import com.manguonmo.popworld.dto.request.SePayWebhookRequest;
import com.manguonmo.popworld.dto.request.ShipCabinetRequest;
import com.manguonmo.popworld.dto.response.BoxReservationResponse;
import com.manguonmo.popworld.dto.response.OwnedItemResponse;
import com.manguonmo.popworld.e2e.base.BaseE2ETest;
import com.manguonmo.popworld.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tier 4 - Real-World Application Scenarios E2E Tests")
public class Tier4RealWorldScenariosE2ETest extends BaseE2ETest {

    @Value("${sepay.webhook.api-key:popworld_secret_key_2026}")
    private String sepayApiKey;

    @Test
    @DisplayName("T4-SCEN-01: Quy trình mua sắm E-Commerce hoàn chỉnh: Giỏ hàng -> Mã giảm giá -> Đặt hàng -> Webhook thanh toán SePay -> Đóng gói -> Giao hàng -> Hoàn tất -> Đối soát Timeline")
    void completeShoppingAndFulfillmentLifecycle() {
        // 1. Khách hàng đăng nhập & chọn sản phẩm
        User customer = createTestUser("shopper_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");
        Product product = createTestProduct("Skullpanda City of Night", 15, BigDecimal.valueOf(600000), null);

        // Tạo mã giảm giá 50,000 VND
        String couponCode = "LIFECYCLE50-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        createTestCoupon(
                couponCode,
                "FIXED",
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(500000),
                null,
                LocalDate.now().minusDays(1),
                LocalDate.now().plusMonths(1),
                10
        );

        // 2. Thêm vào giỏ hàng
        CartItem cartItem = cartService.addToCart(customer.getId(), product.getId(), "SINGLE_BOX", 1);
        assertNotNull(cartItem);

        // 3. Đặt hàng với mã giảm giá
        Order order = orderService.createOrder(
                customer.getId(),
                "Tran Thi B",
                "0912345678",
                "TP Ho Chi Minh",
                "Quan 1",
                "Ben Nghe",
                "45 Le Duan",
                "VIETQR",
                couponCode
        );

        assertNotNull(order);
        String orderCode = order.getOrderCode();
        assertEquals("TO_PAY", order.getStatus());
        // Tiền hàng: 600,000, Ship: 0 (vì >= 500k), Giảm giá: 50,000 -> Tổng thanh toán: 550,000 VND
        assertEquals(0, BigDecimal.valueOf(550000).compareTo(order.getTotalAmount()));

        // 4. Nhận Webhook thanh toán VietQR từ SePay
        SePayWebhookRequest webhook = new SePayWebhookRequest();
        webhook.setContent("Thanh toan don hang " + orderCode);
        webhook.setTransferAmount(BigDecimal.valueOf(550000));
        webhook.setReferenceCode("REF-" + System.currentTimeMillis());

        boolean webhookSuccess = paymentService.processSePayWebhook(webhook, "Apikey " + sepayApiKey);
        assertTrue(webhookSuccess, "Webhook SePay phải xử lý thành công");

        Order paidOrder = orderService.getOrderByCode(orderCode);
        assertEquals("PROCESSING", paidOrder.getStatus(), "Trạng thái đơn hàng phải chuyển sang PROCESSING sau khi thanh toán");
        assertNotNull(paidOrder.getPaidAt(), "paidAt phải được ghi nhận thời điểm thanh toán");

        // 5. Quản trị viên kho đóng gói hàng (PROCESSING -> PACKED)
        Order packedOrder = orderService.packOrder(orderCode, "Admin Kho Ha Noi", "Đã kiểm đếm seal nguyên vẹn");
        assertEquals("PACKED", packedOrder.getStatus());
        assertNotNull(packedOrder.getPackedAt());

        // 6. Quản trị viên bàn giao đơn vị vận chuyển (PACKED -> SHIPPING)
        Order shippedOrder = orderService.shipOrder(orderCode, "Viettel Post", "VTP-888999", "Admin Kho Ha Noi", "Bàn giao bưu tá");
        assertEquals("SHIPPING", shippedOrder.getStatus());
        assertEquals("Viettel Post", shippedOrder.getCarrier());
        assertEquals("VTP-888999", shippedOrder.getTrackingNumber());
        assertNotNull(shippedOrder.getShippedAt());

        // 7. Khách hàng đã nhận hàng và hoàn tất đơn (SHIPPING -> DELIVERED)
        Order deliveredOrder = orderService.completeOrder(orderCode, "Shipper ViettelPost", "Khách hàng đã ký nhận");
        assertEquals("DELIVERED", deliveredOrder.getStatus());
        assertNotNull(deliveredOrder.getDeliveredAt());

        // 8. Đối soát Timeline toàn bộ hành trình đơn hàng
        var timelines = orderService.getOrderTimelines(orderCode);
        assertNotNull(timelines);
        assertTrue(timelines.size() >= 4, "Timeline phải ghi nhận đủ các mốc TO_PAY, PROCESSING, PACKED, SHIPPING, DELIVERED");

        List<String> statuses = timelines.stream().map(t -> t.getToStatus()).toList();
        assertTrue(statuses.contains("TO_PAY"));
        assertTrue(statuses.contains("PROCESSING"));
        assertTrue(statuses.contains("PACKED"));
        assertTrue(statuses.contains("SHIPPING"));
        assertTrue(statuses.contains("DELIVERED"));
    }

    @Test
    @DisplayName("T4-SCEN-02: Quy trình bóc hộp Blind Box POP NOW hoàn chỉnh: Chọn slot -> Giữ hộp -> Thanh toán -> Bóc hộp Online -> Tủ đồ ảo (Cabinet) -> Yêu cầu ship về nhà")
    void completePopNowBlindBoxToShipmentJourney() {
        // 1. Người chơi đăng nhập & chọn sản phẩm
        User player = createTestUser("player_" + UUID.randomUUID().toString().substring(0, 8) + "@popworld.com", "ROLE_USER");
        Product seriesProduct = createTestProduct("Dimoo Dating Series Blind Box", 12, BigDecimal.valueOf(300000), null);

        BlindBoxItem rareDimoo = createTestBlindBoxItem(seriesProduct, "Dimoo Romantic Dinner", RarityType.REGULAR, 100);

        // 2. Chọn và giữ hộp vị trí số 5 trên khay
        BoxReservationRequest reserveReq = new BoxReservationRequest();
        reserveReq.setProductId(seriesProduct.getId());
        reserveReq.setBoxIndex(5);

        BoxReservationResponse reservation = popNowService.reserveBox(player.getId(), reserveReq);
        assertNotNull(reservation);
        String reservationCode = reservation.getReservationCode();

        // 3. Khởi tạo đơn hàng thanh toán gửi vào tủ đồ ảo
        Order cabinetOrder = orderService.createOrderForReservation(player.getId(), reservationCode, "SEPAY");
        assertNotNull(cabinetOrder);
        assertEquals("POP_NOW_CABINET", cabinetOrder.getDeliveryMethod());

        // 4. Thanh toán đơn hàng qua Webhook SePay
        SePayWebhookRequest webhook = new SePayWebhookRequest();
        webhook.setContent("POP NOW " + cabinetOrder.getOrderCode());
        webhook.setTransferAmount(cabinetOrder.getTotalAmount());
        webhook.setReferenceCode("REF-PN-" + System.currentTimeMillis());

        boolean paid = paymentService.processSePayWebhook(webhook, "Apikey " + sepayApiKey);
        assertTrue(paid);

        BoxReservation purchasedRes = boxReservationRepository.findByReservationCode(reservationCode).orElseThrow();
        assertEquals(ReservationStatus.PURCHASED, purchasedRes.getStatus());

        // 5. Bóc hộp trực tuyến (Reveal Art Toy)
        OwnedItemResponse unboxRes = popNowService.unbox(player.getId(), reservationCode);
        assertNotNull(unboxRes);
        assertEquals(rareDimoo.getName(), unboxRes.getItemName());
        assertEquals("IN_CABINET", unboxRes.getStatus());

        // 6. Kiểm tra tủ đồ ảo (Cabinet) của người chơi
        List<OwnedItemResponse> cabinetItems = popNowService.getUserCabinet(player.getId());
        assertFalse(cabinetItems.isEmpty());
        assertTrue(cabinetItems.stream().anyMatch(i -> i.getId().equals(unboxRes.getId())));

        UserAddress address = createTestAddress(player);
        ShipCabinetRequest shipReq = new ShipCabinetRequest();
        shipReq.setAddressId(address.getId());
        shipReq.setOwnedItemIds(List.of(unboxRes.getId()));

        Order shipmentOrder = popNowService.requestShipment(player.getId(), shipReq);
        assertNotNull(shipmentOrder);
        assertEquals("POP_NOW_SHIP", shipmentOrder.getDeliveryMethod());
        assertEquals(address.getRecipientName(), shipmentOrder.getRecipientName());

        // Kiểm tra vật phẩm trong tủ đồ đã chuyển trạng thái sang PENDING_DELIVERY / REQUESTED_SHIPPING
        OwnedItem itemInDb = ownedItemRepository.findById(unboxRes.getId()).orElseThrow();
        assertEquals(OwnedItemStatus.REQUESTED_SHIPPING, itemInDb.getStatus());
    }
}
