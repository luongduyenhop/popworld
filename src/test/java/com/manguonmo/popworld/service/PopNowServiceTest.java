package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.request.BoxReservationRequest;
import com.manguonmo.popworld.dto.request.ShipCabinetRequest;
import com.manguonmo.popworld.dto.response.BlindBoxItemResponse;
import com.manguonmo.popworld.dto.response.BlindBoxSlotResponse;
import com.manguonmo.popworld.dto.response.BoxReservationResponse;
import com.manguonmo.popworld.dto.response.OwnedItemResponse;
import com.manguonmo.popworld.entity.*;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PopNowServiceTest {

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

    private User sampleUser;
    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder().id(1L).email("user@test.com").fullName("Test User").enabled(true).build();
        sampleProduct = Product.builder().id(10L).name("Hirono Little Mischief").active(true).singlePrice(BigDecimal.valueOf(350000)).stockQuantity(20).build();
    }

    @Test
    @DisplayName("reserveBox: Thành công trừ tồn kho nguyên tử và tạo phiếu giữ hộp TTL 5 phút chuẩn POP MART")
    void reserveBox_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(productRepository.findById(10L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.updateStock(10L, 1)).thenReturn(1);

        BlindBoxSlot slot = BlindBoxSlot.builder().id(1L).product(sampleProduct).slotIndex(3).status(SlotStatus.AVAILABLE).build();
        when(blindBoxSlotRepository.findByProductIdAndSlotIndexForUpdate(10L, 3)).thenReturn(Optional.of(slot));

        when(boxReservationRepository.save(any(BoxReservation.class))).thenAnswer(inv -> {
            BoxReservation r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        BoxReservationRequest request = BoxReservationRequest.builder().productId(10L).boxIndex(3).build();
        BoxReservationResponse response = popNowService.reserveBox(1L, request);

        assertNotNull(response);
        assertNotNull(response.getReservationCode());
        assertEquals(3, response.getBoxIndex());
        assertEquals("RESERVED", response.getStatus());

        // Kiểm tra đúng 5 phút TTL
        assertEquals(response.getReservedAt().plusMinutes(5), response.getExpiresAt());

        verify(productRepository).updateStock(10L, 1);
        verify(blindBoxSlotRepository).findByProductIdAndSlotIndexForUpdate(10L, 3);
        verify(blindBoxSlotRepository).save(slot);
        assertEquals(SlotStatus.HELD, slot.getStatus());
        verify(boxReservationRepository).save(any(BoxReservation.class));
    }

    @Test
    @DisplayName("reserveBox: Hết tồn kho (updateStock = 0) -> Ném ngoại lệ BadRequestException, kho không âm")
    void reserveBox_InventoryExhaustion_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(productRepository.findById(10L)).thenReturn(Optional.of(sampleProduct));

        BlindBoxSlot slot = BlindBoxSlot.builder().id(1L).product(sampleProduct).slotIndex(1).status(SlotStatus.AVAILABLE).build();
        when(blindBoxSlotRepository.findByProductIdAndSlotIndexForUpdate(10L, 1)).thenReturn(Optional.of(slot));

        when(productRepository.updateStock(10L, 1)).thenReturn(0);

        BoxReservationRequest request = BoxReservationRequest.builder().productId(10L).boxIndex(1).build();

        BadRequestException ex = assertThrows(BadRequestException.class, () -> popNowService.reserveBox(1L, request));
        assertTrue(ex.getMessage().contains("hết hàng"));
        verify(boxReservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("reserveBox: Vị trí ô hộp đã bị giữ (HELD) -> Ném ngoại lệ và không tạo phiếu")
    void reserveBox_SlotTaken_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(productRepository.findById(10L)).thenReturn(Optional.of(sampleProduct));

        BlindBoxSlot takenSlot = BlindBoxSlot.builder().id(5L).product(sampleProduct).slotIndex(5).status(SlotStatus.HELD).build();
        when(blindBoxSlotRepository.findByProductIdAndSlotIndexForUpdate(10L, 5)).thenReturn(Optional.of(takenSlot));

        BoxReservationRequest request = BoxReservationRequest.builder().productId(10L).boxIndex(5).build();

        BadRequestException ex = assertThrows(BadRequestException.class, () -> popNowService.reserveBox(1L, request));
        assertTrue(ex.getMessage().contains("đang được người khác giữ hoặc đã bán"));
        verify(productRepository, never()).updateStock(any(), anyInt());
        verify(boxReservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelReservation: Khách chủ động hủy -> Đổi trạng thái CANCELLED, hoàn tồn kho và mở lại slot AVAILABLE")
    void cancelReservation_Success() {
        BlindBoxSlot slot = BlindBoxSlot.builder().id(10L).product(sampleProduct).slotIndex(2).status(SlotStatus.HELD).build();
        BoxReservation res = BoxReservation.builder()
                .id(101L)
                .user(sampleUser)
                .product(sampleProduct)
                .slot(slot)
                .boxIndex(2)
                .reservationCode("PN-CANCEL01")
                .status(ReservationStatus.RESERVED)
                .build();
        slot.setCurrentReservation(res);

        when(boxReservationRepository.findByReservationCodeForUpdate("PN-CANCEL01")).thenReturn(Optional.of(res));

        popNowService.cancelReservation(1L, "PN-CANCEL01");

        assertEquals(ReservationStatus.CANCELLED, res.getStatus());
        verify(boxReservationRepository).save(res);
        verify(productRepository).addStock(sampleProduct.getId(), 1);
        assertEquals(SlotStatus.AVAILABLE, slot.getStatus());
        assertNull(slot.getCurrentReservation());
        verify(blindBoxSlotRepository).save(slot);
    }

    @Test
    @DisplayName("cancelReservation: Người dùng khác hủy (IDOR) -> Ném ngoại lệ bảo mật")
    void cancelReservation_UnauthorizedUser_ThrowsException() {
        BoxReservation res = BoxReservation.builder()
                .id(102L)
                .user(sampleUser) // ID = 1
                .product(sampleProduct)
                .reservationCode("PN-RES-IDOR")
                .status(ReservationStatus.RESERVED)
                .build();
        when(boxReservationRepository.findByReservationCodeForUpdate("PN-RES-IDOR")).thenReturn(Optional.of(res));

        assertThrows(BadRequestException.class, () -> popNowService.cancelReservation(999L, "PN-RES-IDOR"));
        verify(productRepository, never()).addStock(any(), any());
    }

    @Test
    @DisplayName("unbox: Khóa bi quan và mở hộp thành công khi trạng thái là PURCHASED")
    void unbox_ServerSideAuthority_Success() {
        BlindBoxSlot slot = BlindBoxSlot.builder().id(1L).product(sampleProduct).slotIndex(3).status(SlotStatus.HELD).build();
        BoxReservation res = BoxReservation.builder()
                .id(200L)
                .user(sampleUser)
                .product(sampleProduct)
                .slot(slot)
                .reservationCode("PN-UNBOX01")
                .status(ReservationStatus.PURCHASED) // Đã thanh toán
                .reservedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        BlindBoxItem regularItem = BlindBoxItem.builder().id(11L).name("The Monster").rarity(RarityType.REGULAR).imageUrl("img1").probabilityWeight(100).build();
        BlindBoxItem secretItem = BlindBoxItem.builder().id(12L).name("The King (Secret)").rarity(RarityType.SECRET).imageUrl("img_sec").probabilityWeight(10).build();

        when(boxReservationRepository.findByReservationCodeForUpdate("PN-UNBOX01")).thenReturn(Optional.of(res));
        when(blindBoxItemRepository.findByProductIdAndActiveTrue(sampleProduct.getId())).thenReturn(List.of(regularItem, secretItem));
        when(ownedItemRepository.save(any(OwnedItem.class))).thenAnswer(inv -> {
            OwnedItem oi = inv.getArgument(0);
            oi.setId(500L);
            return oi;
        });

        OwnedItemResponse response = popNowService.unbox(1L, "PN-UNBOX01");

        assertNotNull(response);
        assertEquals(500L, response.getId());
        assertTrue(response.getItemName().equals("The Monster") || response.getItemName().equals("The King (Secret)"));
        assertEquals(ReservationStatus.UNBOXED, res.getStatus());
        assertEquals(SlotStatus.SOLD, slot.getStatus());
        verify(ownedItemRepository).save(any(OwnedItem.class));
    }

    @Test
    @DisplayName("unbox: Từ chối mở khi chưa thanh toán (trạng thái RESERVED)")
    void unbox_WhenStatusIsReserved_ThrowsBadRequestException() {
        BoxReservation res = BoxReservation.builder()
                .id(205L)
                .user(sampleUser)
                .product(sampleProduct)
                .reservationCode("PN-UNBOX-UNPAID")
                .status(ReservationStatus.RESERVED)
                .build();

        when(boxReservationRepository.findByReservationCodeForUpdate("PN-UNBOX-UNPAID")).thenReturn(Optional.of(res));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> popNowService.unbox(1L, "PN-UNBOX-UNPAID"));
        assertTrue(ex.getMessage().contains("chưa được thanh toán"));
        verify(ownedItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("unbox: Từ chối mở khi phiếu đã bị hủy hoặc hết hạn (CANCELLED/EXPIRED)")
    void unbox_WhenStatusIsCancelledOrExpired_ThrowsBadRequestException() {
        BoxReservation resCancelled = BoxReservation.builder().id(206L).user(sampleUser).reservationCode("PN-CANCEL").status(ReservationStatus.CANCELLED).build();
        when(boxReservationRepository.findByReservationCodeForUpdate("PN-CANCEL")).thenReturn(Optional.of(resCancelled));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> popNowService.unbox(1L, "PN-CANCEL"));
        assertTrue(ex.getMessage().contains("quá hạn hoặc bị hủy"));

        BoxReservation resExpired = BoxReservation.builder().id(207L).user(sampleUser).reservationCode("PN-EXPIRED").status(ReservationStatus.EXPIRED).build();
        when(boxReservationRepository.findByReservationCodeForUpdate("PN-EXPIRED")).thenReturn(Optional.of(resExpired));

        BadRequestException ex2 = assertThrows(BadRequestException.class, () -> popNowService.unbox(1L, "PN-EXPIRED"));
        assertTrue(ex2.getMessage().contains("quá hạn hoặc bị hủy"));
    }

    @Test
    @DisplayName("unbox: Không tạo fallback BlindBoxItem khi thiếu cấu hình series -> Thất bại an toàn")
    void unbox_WhenMissingSeriesConfiguration_ThrowsException_NoFallbackCreated() {
        BoxReservation res = BoxReservation.builder()
                .id(208L)
                .user(sampleUser)
                .product(sampleProduct)
                .reservationCode("PN-NO-SERIES")
                .status(ReservationStatus.PURCHASED)
                .build();

        when(boxReservationRepository.findByReservationCodeForUpdate("PN-NO-SERIES")).thenReturn(Optional.of(res));
        when(blindBoxItemRepository.findByProductIdAndActiveTrue(sampleProduct.getId())).thenReturn(Collections.emptyList());

        BadRequestException ex = assertThrows(BadRequestException.class, () -> popNowService.unbox(1L, "PN-NO-SERIES"));
        assertTrue(ex.getMessage().contains("Chưa cấu hình danh sách mô hình"));

        // Tuyệt đối không tự sinh BlindBoxItem fallback hay OwnedItem
        verify(blindBoxItemRepository, never()).save(any());
        verify(ownedItemRepository, never()).save(any());
        assertEquals(ReservationStatus.PURCHASED, res.getStatus()); // Giữ nguyên trạng thái
    }

    @Test
    @DisplayName("unbox: Gọi lại trên hộp đã unbox (Idempotency) -> Trả về kết quả cũ, không tạo OwnedItem mới")
    void unbox_AlreadyUnboxed_Idempotent() {
        BoxReservation res = BoxReservation.builder()
                .id(201L)
                .user(sampleUser)
                .product(sampleProduct)
                .reservationCode("PN-UNBOX-DUP")
                .status(ReservationStatus.UNBOXED)
                .build();

        BlindBoxItem item = BlindBoxItem.builder().id(11L).name("The Monster").rarity(RarityType.REGULAR).imageUrl("img1").build();
        OwnedItem existingOwnedItem = OwnedItem.builder()
                .id(555L)
                .user(sampleUser)
                .product(sampleProduct)
                .blindBoxItem(item)
                .reservation(res)
                .status(OwnedItemStatus.IN_CABINET)
                .unboxedAt(LocalDateTime.now())
                .build();

        when(boxReservationRepository.findByReservationCodeForUpdate("PN-UNBOX-DUP")).thenReturn(Optional.of(res));
        when(ownedItemRepository.findByReservationId(201L)).thenReturn(Optional.of(existingOwnedItem));

        OwnedItemResponse response = popNowService.unbox(1L, "PN-UNBOX-DUP");

        assertNotNull(response);
        assertEquals(555L, response.getId());
        assertEquals("The Monster", response.getItemName());
        verify(ownedItemRepository, never()).save(any(OwnedItem.class)); // Không tạo bản ghi mới!
    }

    @Test
    @DisplayName("markPurchased: Đổi trạng thái sang PURCHASED khi thanh toán thành công và đánh dấu slot SOLD")
    void markPurchased_Success() {
        BlindBoxSlot slot = BlindBoxSlot.builder().id(1L).product(sampleProduct).slotIndex(3).status(SlotStatus.HELD).build();
        BoxReservation res = BoxReservation.builder().id(401L).status(ReservationStatus.RESERVED).slot(slot).build();
        when(boxReservationRepository.findByReservationCodeForUpdate("PN-BUY")).thenReturn(Optional.of(res));

        popNowService.markPurchased("PN-BUY", "PW-ORDER-001");

        assertEquals(ReservationStatus.PURCHASED, res.getStatus());
        assertEquals("PW-ORDER-001", res.getOrderCode());
        assertEquals(SlotStatus.SOLD, slot.getStatus());
        verify(boxReservationRepository).save(res);
        verify(blindBoxSlotRepository).save(slot);
    }

    @Test
    @DisplayName("markPurchased: Phiếu đã hết hạn hoặc bị hủy -> Ném ngoại lệ, không cho phép thanh toán")
    void markPurchased_WhenExpiredOrCancelled_ThrowsBadRequestException() {
        BoxReservation expiredRes = BoxReservation.builder().id(402L).status(ReservationStatus.EXPIRED).build();
        when(boxReservationRepository.findByReservationCodeForUpdate("PN-EXPIRED")).thenReturn(Optional.of(expiredRes));

        assertThrows(BadRequestException.class, () -> popNowService.markPurchased("PN-EXPIRED", "PW-1"));

        BoxReservation cancelledRes = BoxReservation.builder().id(403L).status(ReservationStatus.CANCELLED).build();
        when(boxReservationRepository.findByReservationCodeForUpdate("PN-CANCEL")).thenReturn(Optional.of(cancelledRes));

        assertThrows(BadRequestException.class, () -> popNowService.markPurchased("PN-CANCEL", "PW-2"));
    }

    @Test
    @DisplayName("releaseExpiredReservations: Quét và giải phóng các phiếu quá hạn, trả lại tồn kho và mở slot")
    void releaseExpiredReservations_Success() {
        BlindBoxSlot slot1 = BlindBoxSlot.builder().id(1L).product(sampleProduct).slotIndex(1).status(SlotStatus.HELD).build();
        BlindBoxSlot slot2 = BlindBoxSlot.builder().id(2L).product(sampleProduct).slotIndex(2).status(SlotStatus.HELD).build();

        BoxReservation res1 = BoxReservation.builder().id(301L).product(sampleProduct).slot(slot1).status(ReservationStatus.RESERVED).build();
        BoxReservation res2 = BoxReservation.builder().id(302L).product(sampleProduct).slot(slot2).status(ReservationStatus.RESERVED).build();

        when(boxReservationRepository.findByStatusAndExpiresAtBefore(eq(ReservationStatus.RESERVED), any(LocalDateTime.class)))
                .thenReturn(List.of(res1, res2));

        int released = popNowService.releaseExpiredReservations();

        assertEquals(2, released);
        assertEquals(ReservationStatus.EXPIRED, res1.getStatus());
        assertEquals(ReservationStatus.EXPIRED, res2.getStatus());
        assertEquals(SlotStatus.AVAILABLE, slot1.getStatus());
        assertEquals(SlotStatus.AVAILABLE, slot2.getStatus());
        verify(productRepository, times(2)).addStock(sampleProduct.getId(), 1);
        verify(blindBoxSlotRepository).save(slot1);
        verify(blindBoxSlotRepository).save(slot2);
    }

    @Test
    @DisplayName("getUserCabinet: Lấy danh sách mô hình trong tủ đồ ảo của khách hàng")
    void getUserCabinet_Success() {
        BlindBoxItem item = BlindBoxItem.builder().id(11L).name("The Monster").rarity(RarityType.REGULAR).imageUrl("img1").build();
        OwnedItem oi = OwnedItem.builder().id(601L).user(sampleUser).product(sampleProduct).blindBoxItem(item).status(OwnedItemStatus.IN_CABINET).unboxedAt(LocalDateTime.now()).build();

        when(ownedItemRepository.findByUserIdOrderByUnboxedAtDesc(1L)).thenReturn(List.of(oi));

        List<OwnedItemResponse> cabinet = popNowService.getUserCabinet(1L);

        assertEquals(1, cabinet.size());
        assertEquals("The Monster", cabinet.get(0).getItemName());
    }

    @Test
    @DisplayName("getSeriesItems: Lấy danh sách mô hình trong series thành công")
    void getSeriesItems_Success() {
        BlindBoxItem item = BlindBoxItem.builder().id(11L).name("The Monster").rarity(RarityType.REGULAR).imageUrl("img1").build();
        when(blindBoxItemRepository.findByProductIdAndActiveTrue(10L)).thenReturn(List.of(item));

        List<BlindBoxItemResponse> series = popNowService.getSeriesItems(10L);

        assertEquals(1, series.size());
        assertEquals("The Monster", series.get(0).getName());
        assertFalse(Boolean.TRUE.equals(series.get(0).getIsSecret()));
    }

    @Test
    @DisplayName("getProductSlots: Tự động điều chỉnh số lượng slot linh hoạt khi series có 6 hộp")
    void getProductSlots_When6SlotsConfigured_Returns6Slots() {
        Product sixBoxProduct = Product.builder().id(10L).name("Hirono After Dark").active(true)
                .packagingType("6 Boxes per Set").boxesPerSet(6).singlePrice(BigDecimal.valueOf(350000)).stockQuantity(20).build();
        when(productRepository.findById(10L)).thenReturn(Optional.of(sixBoxProduct));

        List<BlindBoxSlot> sixSlots = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            sixSlots.add(BlindBoxSlot.builder().id((long) i).product(sixBoxProduct).slotIndex(i).status(SlotStatus.AVAILABLE).build());
        }
        when(blindBoxSlotRepository.findByProductIdOrderBySlotIndexAsc(10L)).thenReturn(sixSlots);

        List<BlindBoxSlotResponse> result = popNowService.getProductSlots(10L);

        assertEquals(6, result.size());
        assertEquals(1, result.get(0).getSlotIndex());
        assertEquals(6, result.get(5).getSlotIndex());
        assertEquals("AVAILABLE", result.get(0).getStatus());
    }

    @Test
    @DisplayName("getProductSlots: Tự động dự phòng 12 slot chuẩn khi chưa cấu hình slot trong DB")
    void getProductSlots_WhenNoSlotsConfigured_DefaultsTo12Slots() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(sampleProduct));
        when(blindBoxSlotRepository.findByProductIdOrderBySlotIndexAsc(10L)).thenReturn(Collections.emptyList());

        List<BlindBoxSlotResponse> result = popNowService.getProductSlots(10L);

        assertEquals(12, result.size());
    }

    @Test
    @DisplayName("requestShipment: Happy path - 1 item + địa chỉ chính chủ -> Tạo đúng 1 đơn hàng giao vận, chuyển trạng thái REQUESTED_SHIPPING và không đổi tồn kho")
    void requestShipment_HappyPath_CreatesOrderAndTransitionsItem() {
        UserAddress address = UserAddress.builder()
                .id(100L)
                .user(sampleUser)
                .recipientName("Nguyễn Văn A")
                .recipientPhone("0987654321")
                .provinceCity("Hà Nội")
                .district("Cầu Giấy")
                .ward("Dịch Vọng Hậu")
                .detailedAddress("123 Duy Tân")
                .build();

        BlindBoxItem bbItem = BlindBoxItem.builder().id(50L).name("Crybaby Crying Again").rarity(RarityType.REGULAR).build();
        OwnedItem ownedItem = OwnedItem.builder()
                .id(200L)
                .user(sampleUser)
                .product(sampleProduct)
                .blindBoxItem(bbItem)
                .status(OwnedItemStatus.IN_CABINET)
                .unboxedAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userAddressRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(address));
        when(ownedItemRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(ownedItem));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(999L);
            return o;
        });

        ShipCabinetRequest request = ShipCabinetRequest.builder()
                .addressId(100L)
                .ownedItemId(200L)
                .build();

        Order createdOrder = popNowService.requestShipment(1L, request);

        assertNotNull(createdOrder);
        assertEquals("POP_NOW_SHIP", createdOrder.getDeliveryMethod());
        assertEquals("PROCESSING", createdOrder.getStatus());
        assertEquals("POP_NOW", createdOrder.getPaymentMethod());
        assertEquals("Nguyễn Văn A", createdOrder.getRecipientName());
        assertEquals("0987654321", createdOrder.getRecipientPhone());
        assertEquals("Hà Nội", createdOrder.getProvinceCity());
        assertEquals("Cầu Giấy", createdOrder.getDistrict());
        assertEquals("Dịch Vọng Hậu", createdOrder.getWard());
        assertEquals("123 Duy Tân", createdOrder.getDetailedAddress());
        assertEquals(BigDecimal.ZERO, createdOrder.getTotalAmount());

        // Trạng thái vật phẩm được cập nhật
        assertEquals(OwnedItemStatus.REQUESTED_SHIPPING, ownedItem.getStatus());
        verify(ownedItemRepository, times(1)).save(ownedItem);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderItemRepository, times(1)).saveAll(anyList());

        // Invariant: Tồn kho sản phẩm tuyệt đối không bị thay đổi (không trừ / không cộng lại)
        verify(productRepository, never()).updateStock(any(), anyInt());
        verify(productRepository, never()).addStock(any(), anyInt());
    }

    @Test
    @DisplayName("requestShipment: Từ chối khi vật phẩm thuộc về người dùng khác (IDOR)")
    void requestShipment_ForeignOwnedItem_ThrowsBadRequest() {
        User otherUser = User.builder().id(2L).email("other@test.com").build();
        UserAddress address = UserAddress.builder().id(100L).user(sampleUser).recipientName("A").recipientPhone("1").provinceCity("HN").district("CG").detailedAddress("123").build();

        BlindBoxItem bbItem = BlindBoxItem.builder().id(50L).name("Secret").rarity(RarityType.SECRET).build();
        OwnedItem foreignItem = OwnedItem.builder()
                .id(200L)
                .user(otherUser) // Không phải sampleUser
                .product(sampleProduct)
                .blindBoxItem(bbItem)
                .status(OwnedItemStatus.IN_CABINET)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userAddressRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(address));
        when(ownedItemRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(foreignItem));

        ShipCabinetRequest request = ShipCabinetRequest.builder().addressId(100L).ownedItemId(200L).build();

        BadRequestException ex = assertThrows(BadRequestException.class, () -> popNowService.requestShipment(1L, request));
        assertTrue(ex.getMessage().contains("Bạn không có quyền yêu cầu giao hàng"));
        verify(orderRepository, never()).save(any());
        assertEquals(OwnedItemStatus.IN_CABINET, foreignItem.getStatus());
    }

    @Test
    @DisplayName("requestShipment: Từ chối khi địa chỉ giao hàng không thuộc về người dùng (Address IDOR)")
    void requestShipment_ForeignAddress_ThrowsResourceNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userAddressRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        ShipCabinetRequest request = ShipCabinetRequest.builder().addressId(999L).ownedItemId(200L).build();

        assertThrows(ResourceNotFoundException.class, () -> popNowService.requestShipment(1L, request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("requestShipment: Từ chối khi vật phẩm đã yêu cầu giao hàng trước đó (Idempotency / State Invariant)")
    void requestShipment_AlreadyRequestedShippingItem_ThrowsBadRequest() {
        UserAddress address = UserAddress.builder().id(100L).user(sampleUser).recipientName("A").recipientPhone("1").provinceCity("HN").district("CG").detailedAddress("123").build();

        BlindBoxItem bbItem = BlindBoxItem.builder().id(50L).name("Mô hình").rarity(RarityType.REGULAR).build();
        OwnedItem alreadyShippedItem = OwnedItem.builder()
                .id(200L)
                .user(sampleUser)
                .product(sampleProduct)
                .blindBoxItem(bbItem)
                .status(OwnedItemStatus.REQUESTED_SHIPPING) // Đã chuyển trạng thái
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userAddressRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(address));
        when(ownedItemRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(alreadyShippedItem));

        ShipCabinetRequest request = ShipCabinetRequest.builder().addressId(100L).ownedItemId(200L).build();

        BadRequestException ex = assertThrows(BadRequestException.class, () -> popNowService.requestShipment(1L, request));
        assertTrue(ex.getMessage().contains("đã được yêu cầu giao hàng"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("requestShipment: Từ chối khi danh sách vật phẩm rỗng")
    void requestShipment_EmptyItems_ThrowsBadRequest() {
        ShipCabinetRequest request = ShipCabinetRequest.builder().addressId(100L).ownedItemIds(Collections.emptyList()).build();
        assertThrows(BadRequestException.class, () -> popNowService.requestShipment(1L, request));
    }
}
