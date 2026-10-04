package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.request.SePayWebhookRequest;
import com.manguonmo.popworld.dto.response.BlindBoxSlotResponse;
import com.manguonmo.popworld.dto.response.OwnedItemResponse;
import com.manguonmo.popworld.entity.*;
import com.manguonmo.popworld.exception.BadRequestException;
import org.springframework.security.access.AccessDeniedException;
import com.manguonmo.popworld.mapper.OrderMapper;
import com.manguonmo.popworld.repository.*;
import com.manguonmo.popworld.service.impl.OrderServiceImpl;
import com.manguonmo.popworld.service.impl.PaymentServiceImpl;
import com.manguonmo.popworld.service.impl.PopNowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PopNowCustomerIntegrationTest {

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
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private UserAddressRepository userAddressRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CouponService couponService;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private PopNowServiceImpl popNowService;

    private OrderServiceImpl orderService;
    private PaymentServiceImpl paymentService;

    private User sampleUser;
    private User otherUser;
    private Product sampleProduct;

    private static final String VALID_API_KEY = "sepay_secret_token_123456";
    private static final String VALID_AUTH_HEADER = "Apikey sepay_secret_token_123456";

    @BeforeEach
    void setUp() {
        sampleUser = User.builder().id(1L).email("user1@test.com").fullName("Customer One").enabled(true).build();
        otherUser = User.builder().id(2L).email("user2@test.com").fullName("Customer Two").enabled(true).build();
        sampleProduct = Product.builder().id(10L).name("SKULLPANDA Everyday Wonderland").active(true)
                .singlePrice(BigDecimal.valueOf(350000)).stockQuantity(20).build();

        orderService = new OrderServiceImpl(
                orderRepository, cartItemRepository, couponService,
                orderItemRepository, userRepository, productRepository,
                orderMapper, boxReservationRepository, null
        );

        paymentService = new PaymentServiceImpl(orderRepository, boxReservationRepository, popNowService, null);
        ReflectionTestUtils.setField(paymentService, "apiKey", VALID_API_KEY);
    }

    @Test
    @DisplayName("getProductSlots: Trả về đầy đủ 12 ô hộp với trạng thái chính xác (AVAILABLE, HELD, SOLD)")
    void getProductSlots_Returns12Slots_WithAccurateStatuses() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(sampleProduct));

        BlindBoxSlot slot1 = BlindBoxSlot.builder().product(sampleProduct).slotIndex(1).status(SlotStatus.SOLD).build();
        BlindBoxSlot slot3 = BlindBoxSlot.builder().product(sampleProduct).slotIndex(3).status(SlotStatus.HELD)
                .currentReservation(BoxReservation.builder().expiresAt(LocalDateTime.now().plusMinutes(3)).build())
                .build();
        // Slot 5 is HELD but its reservation expired -> Should be evaluated as AVAILABLE
        BlindBoxSlot slot5 = BlindBoxSlot.builder().product(sampleProduct).slotIndex(5).status(SlotStatus.HELD)
                .currentReservation(BoxReservation.builder().expiresAt(LocalDateTime.now().minusMinutes(1)).build())
                .build();

        when(blindBoxSlotRepository.findByProductIdOrderBySlotIndexAsc(10L)).thenReturn(List.of(slot1, slot3, slot5));

        List<BlindBoxSlotResponse> result = popNowService.getProductSlots(10L);

        assertNotNull(result);
        assertEquals(12, result.size());
        assertEquals("SOLD", result.get(0).getStatus()); // slot 1
        assertEquals("AVAILABLE", result.get(1).getStatus()); // slot 2
        assertEquals("HELD", result.get(2).getStatus()); // slot 3
        assertEquals("AVAILABLE", result.get(4).getStatus()); // slot 5 (expired held -> available)
    }

    @Test
    @DisplayName("createOrderForReservation: Tạo đơn hàng thành công và KHÔNG trừ tồn kho lần 2")
    void createOrderForReservation_Success_DoesNotDecrementStockTwice() {
        LocalDateTime now = LocalDateTime.now();
        BoxReservation reservation = BoxReservation.builder()
                .id(100L)
                .user(sampleUser)
                .product(sampleProduct)
                .reservationCode("PN-RESERVE-01")
                .boxIndex(4)
                .status(ReservationStatus.RESERVED)
                .reservedAt(now)
                .expiresAt(now.plusMinutes(5))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(boxReservationRepository.findByReservationCodeForUpdate("PN-RESERVE-01")).thenReturn(Optional.of(reservation));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(999L);
            return o;
        });

        Order createdOrder = orderService.createOrderForReservation(1L, "PN-RESERVE-01", "SEPAY");

        assertNotNull(createdOrder);
        assertTrue(createdOrder.getOrderCode().startsWith("PW-"));
        assertEquals("TO_PAY", createdOrder.getStatus());
        assertEquals("SEPAY", createdOrder.getPaymentMethod());
        assertEquals(BigDecimal.valueOf(350000), createdOrder.getTotalAmount());
        assertEquals("POP_NOW_CABINET", createdOrder.getDeliveryMethod());
        assertEquals(reservation.getExpiresAt(), createdOrder.getExpiresAt());

        // Bất biến cực kỳ quan trọng: updateStock KHÔNG ĐƯỢC gọi vì reserveBox đã trừ kho trước đó
        verify(productRepository, never()).updateStock(anyLong(), anyInt());
        verify(orderItemRepository, times(1)).save(any(OrderItem.class));
        verify(boxReservationRepository, times(1)).save(reservation);
        assertEquals(createdOrder.getOrderCode(), reservation.getOrderCode());
    }

    @Test
    @DisplayName("createOrderForReservation: IDOR Protection - Người dùng khác không thể thanh toán phiếu của người khác")
    void createOrderForReservation_IDOR_RejectsDifferentUser() {
        BoxReservation reservation = BoxReservation.builder()
                .id(100L)
                .user(sampleUser) // thuộc User 1
                .product(sampleProduct)
                .reservationCode("PN-RESERVE-01")
                .status(ReservationStatus.RESERVED)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));
        when(boxReservationRepository.findByReservationCodeForUpdate("PN-RESERVE-01")).thenReturn(Optional.of(reservation));

        assertThrows(BadRequestException.class, () ->
                orderService.createOrderForReservation(2L, "PN-RESERVE-01", "SEPAY")
        );
    }

    @Test
    @DisplayName("createOrderForReservation: Từ chối nếu phiếu giữ hộp đã hết hạn hoặc bị hủy")
    void createOrderForReservation_ExpiredOrCancelled_Rejects() {
        BoxReservation expiredRes = BoxReservation.builder()
                .id(101L)
                .user(sampleUser)
                .product(sampleProduct)
                .reservationCode("PN-EXPIRED-01")
                .status(ReservationStatus.RESERVED)
                .expiresAt(LocalDateTime.now().minusMinutes(2)) // đã quá hạn
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(boxReservationRepository.findByReservationCodeForUpdate("PN-EXPIRED-01")).thenReturn(Optional.of(expiredRes));

        assertThrows(BadRequestException.class, () ->
                orderService.createOrderForReservation(1L, "PN-EXPIRED-01", "SEPAY")
        );
    }

    @Test
    @DisplayName("SePay Webhook: Xác nhận thanh toán đơn hàng thành công tự động kích hoạt markPurchased cho POP NOW")
    void sePayWebhook_PaymentSuccess_TriggersMarkPurchased() {
        String orderCode = "PW-1726000000099";
        Order order = Order.builder()
                .id(50L)
                .orderCode(orderCode)
                .user(sampleUser)
                .status("TO_PAY")
                .totalAmount(BigDecimal.valueOf(350000))
                .build();

        BoxReservation reservation = BoxReservation.builder()
                .id(100L)
                .user(sampleUser)
                .product(sampleProduct)
                .reservationCode("PN-RESERVE-99")
                .orderCode(orderCode)
                .status(ReservationStatus.RESERVED)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(orderRepository.findByOrderCode(orderCode)).thenReturn(Optional.of(order));
        when(boxReservationRepository.findByOrderCode(orderCode)).thenReturn(Optional.of(reservation));
        when(boxReservationRepository.findByReservationCodeForUpdate("PN-RESERVE-99")).thenReturn(Optional.of(reservation));

        SePayWebhookRequest webhook = SePayWebhookRequest.builder()
                .content("Thanh toan don " + orderCode)
                .transferAmount(BigDecimal.valueOf(350000))
                .referenceCode("SEPAY-REF-999")
                .build();

        boolean processed = paymentService.processSePayWebhook(webhook, VALID_AUTH_HEADER);

        assertTrue(processed);
        assertEquals("PROCESSING", order.getStatus());
        assertEquals(ReservationStatus.PURCHASED, reservation.getStatus());
        verify(boxReservationRepository, times(1)).save(reservation);
    }

    @Test
    @DisplayName("unbox: IDOR Protection - Người khác không thể mở hộp của chủ sở hữu")
    void unbox_IDOR_RejectsNonOwner() {
        BoxReservation reservation = BoxReservation.builder()
                .id(100L)
                .user(sampleUser) // User 1
                .product(sampleProduct)
                .reservationCode("PN-PAID-01")
                .status(ReservationStatus.PURCHASED)
                .build();

        when(boxReservationRepository.findByReservationCodeForUpdate("PN-PAID-01")).thenReturn(Optional.of(reservation));

        assertThrows(AccessDeniedException.class, () ->
                popNowService.unbox(2L, "PN-PAID-01") // User 2 cố gắng unbox
        );
    }

    @Test
    @DisplayName("getUserCabinet: IDOR Isolation - Chỉ trả về các vật phẩm thuộc về đúng userId")
    void getUserCabinet_IDOR_ReturnsOnlyUserOwnedItems() {
        BlindBoxItem item = BlindBoxItem.builder().id(10L).name("Crybaby Sad Club").rarity(RarityType.REGULAR).build();
        OwnedItem owned1 = OwnedItem.builder().id(1L).user(sampleUser).product(sampleProduct).blindBoxItem(item).status(OwnedItemStatus.IN_CABINET).build();

        when(ownedItemRepository.findByUserIdOrderByUnboxedAtDesc(1L)).thenReturn(List.of(owned1));

        List<OwnedItemResponse> cabinet = popNowService.getUserCabinet(1L);

        assertEquals(1, cabinet.size());
        assertEquals("Crybaby Sad Club", cabinet.get(0).getItemName());
        verify(ownedItemRepository, times(1)).findByUserIdOrderByUnboxedAtDesc(1L);
    }

    @Test
    @DisplayName("requestShipment: Yêu cầu gồm cả item hợp lệ và item bất hợp lệ -> Rollback, không lưu đơn và không đổi trạng thái item hợp lệ")
    void requestShipment_MixedValidAndInvalid_RollsBackAllChanges() {
        UserAddress address = UserAddress.builder().id(100L).user(sampleUser).recipientName("A").recipientPhone("1").provinceCity("HN").district("CG").detailedAddress("123").build();

        BlindBoxItem bbItem1 = BlindBoxItem.builder().id(11L).name("Item 1").rarity(RarityType.REGULAR).build();
        OwnedItem validItem = OwnedItem.builder().id(101L).user(sampleUser).product(sampleProduct).blindBoxItem(bbItem1).status(OwnedItemStatus.IN_CABINET).build();

        BlindBoxItem bbItem2 = BlindBoxItem.builder().id(12L).name("Item 2").rarity(RarityType.SECRET).build();
        OwnedItem foreignItem = OwnedItem.builder().id(102L).user(otherUser).product(sampleProduct).blindBoxItem(bbItem2).status(OwnedItemStatus.IN_CABINET).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userAddressRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(address));
        when(ownedItemRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(validItem));
        when(ownedItemRepository.findByIdForUpdate(102L)).thenReturn(Optional.of(foreignItem));

        assertThrows(BadRequestException.class, () ->
                popNowService.requestShipment(1L, 100L, List.of(101L, 102L))
        );

        // Khẳng định: Không tạo bất kỳ đơn hàng nào
        verify(orderRepository, never()).save(any(Order.class));
        verify(orderItemRepository, never()).saveAll(any());

        // Khẳng định: validItem không bị chuyển trạng thái thành REQUESTED_SHIPPING
        assertEquals(OwnedItemStatus.IN_CABINET, validItem.getStatus());
        verify(ownedItemRepository, never()).save(validItem);
    }
}
