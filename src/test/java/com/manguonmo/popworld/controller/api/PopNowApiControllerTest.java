package com.manguonmo.popworld.controller.api;

import com.manguonmo.popworld.dto.request.BoxReservationRequest;
import com.manguonmo.popworld.dto.request.ShipCabinetRequest;
import com.manguonmo.popworld.dto.request.UnboxRequest;
import com.manguonmo.popworld.dto.response.ApiResponse;
import com.manguonmo.popworld.dto.response.BlindBoxItemResponse;
import com.manguonmo.popworld.dto.response.BoxReservationResponse;
import com.manguonmo.popworld.dto.response.OwnedItemResponse;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.service.PopNowService;
import com.manguonmo.popworld.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PopNowApiControllerTest {

    @Mock
    private PopNowService popNowService;

    @Mock
    private UserService userService;

    @Mock
    private com.manguonmo.popworld.service.OrderService orderService;

    @Mock
    private Principal principal;

    @InjectMocks
    private PopNowApiController controller;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder().id(1L).email("user@test.com").enabled(true).build();
    }

    @Test
    @DisplayName("reserveBox: Đăng nhập thành công -> Gọi service giữ hộp")
    void reserveBox_Success() {
        when(principal.getName()).thenReturn("user@test.com");
        when(userService.getUserByEmail("user@test.com")).thenReturn(sampleUser);

        BoxReservationRequest request = BoxReservationRequest.builder().productId(10L).boxIndex(2).build();
        BoxReservationResponse mockRes = BoxReservationResponse.builder().reservationCode("PN-123").boxIndex(2).build();
        when(popNowService.reserveBox(1L, request)).thenReturn(mockRes);

        ResponseEntity<ApiResponse<BoxReservationResponse>> response = controller.reserveBox(request, principal);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        assertEquals("PN-123", response.getBody().getData().getReservationCode());
    }

    @Test
    @DisplayName("unbox: Khách gọi unbox với reservationCode -> Quyết định kết quả từ server")
    void unbox_Success() {
        when(principal.getName()).thenReturn("user@test.com");
        when(userService.getUserByEmail("user@test.com")).thenReturn(sampleUser);

        UnboxRequest request = UnboxRequest.builder().reservationCode("PN-123").build();
        OwnedItemResponse mockOwned = OwnedItemResponse.builder().id(100L).itemName("The Monster").build();
        when(popNowService.unbox(1L, "PN-123")).thenReturn(mockOwned);

        ResponseEntity<ApiResponse<OwnedItemResponse>> response = controller.unbox(request, principal);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("The Monster", response.getBody().getData().getItemName());
    }

    @Test
    @DisplayName("getCabinet: Lấy danh sách tủ đồ ảo của người dùng")
    void getCabinet_Success() {
        when(principal.getName()).thenReturn("user@test.com");
        when(userService.getUserByEmail("user@test.com")).thenReturn(sampleUser);

        when(popNowService.getUserCabinet(1L)).thenReturn(List.of(
                OwnedItemResponse.builder().id(1L).itemName("Molly").build()
        ));

        ResponseEntity<ApiResponse<List<OwnedItemResponse>>> response = controller.getCabinet(principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    @DisplayName("getSeriesItems: Lấy danh sách mô hình (công khai không cần đăng nhập)")
    void getSeriesItems_Success() {
        when(popNowService.getSeriesItems(10L)).thenReturn(List.of(
                BlindBoxItemResponse.builder().id(1L).name("Figure 1").isSecret(false).build()
        ));

        ResponseEntity<ApiResponse<List<BlindBoxItemResponse>>> response = controller.getSeriesItems(10L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    @DisplayName("reserveBox: Chưa đăng nhập (principal null) -> Ném ngoại lệ BadRequestException")
    void unauthenticated_ThrowsException() {
        BoxReservationRequest request = BoxReservationRequest.builder().productId(10L).boxIndex(1).build();
        assertThrows(BadRequestException.class, () -> controller.reserveBox(request, null));
    }

    @Test
    @DisplayName("getProductSlots: Lấy danh sách 12 ô hộp công khai")
    void getProductSlots_Success() {
        when(popNowService.getProductSlots(10L)).thenReturn(List.of(
                com.manguonmo.popworld.dto.response.BlindBoxSlotResponse.builder().slotIndex(1).status("AVAILABLE").build()
        ));

        ResponseEntity<ApiResponse<List<com.manguonmo.popworld.dto.response.BlindBoxSlotResponse>>> response = controller.getProductSlots(10L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
        assertEquals("AVAILABLE", response.getBody().getData().get(0).getStatus());
    }

    @Test
    @DisplayName("getReservation: Lấy thông tin phiếu giữ hộp của user hiện tại")
    void getReservation_Success() {
        when(principal.getName()).thenReturn("user@test.com");
        when(userService.getUserByEmail("user@test.com")).thenReturn(sampleUser);

        BoxReservationResponse mockRes = BoxReservationResponse.builder()
                .reservationCode("PN-RES-01")
                .boxIndex(5)
                .status("RESERVED")
                .build();
        when(popNowService.getReservationByCode(1L, "PN-RES-01")).thenReturn(mockRes);

        ResponseEntity<ApiResponse<BoxReservationResponse>> response = controller.getReservation("PN-RES-01", principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("PN-RES-01", response.getBody().getData().getReservationCode());
    }

    @Test
    @DisplayName("checkoutReservation: Tạo đơn hàng thanh toán cho phiếu giữ hộp")
    void checkoutReservation_Success() {
        when(principal.getName()).thenReturn("user@test.com");
        when(userService.getUserByEmail("user@test.com")).thenReturn(sampleUser);

        com.manguonmo.popworld.entity.Order mockOrder = com.manguonmo.popworld.entity.Order.builder()
                .orderCode("PW-POP-123")
                .totalAmount(java.math.BigDecimal.valueOf(350000))
                .paymentMethod("SEPAY")
                .build();
        when(orderService.createOrderForReservation(1L, "PN-RES-01", "SEPAY")).thenReturn(mockOrder);

        ResponseEntity<ApiResponse<java.util.Map<String, Object>>> response = controller.checkoutReservation("PN-RES-01", "SEPAY", principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("PW-POP-123", response.getBody().getData().get("orderCode"));
        assertEquals("/checkout/payment/PW-POP-123", response.getBody().getData().get("paymentUrl"));
    }

    @Test
    @DisplayName("shipCabinetItems: Yêu cầu giao hàng thành công -> Trả về mã đơn vận và URL điều hướng")
    void shipCabinetItems_Success() {
        when(principal.getName()).thenReturn("user@test.com");
        when(userService.getUserByEmail("user@test.com")).thenReturn(sampleUser);

        ShipCabinetRequest req = ShipCabinetRequest.builder().addressId(10L).ownedItemId(101L).build();
        com.manguonmo.popworld.entity.Order mockOrder = com.manguonmo.popworld.entity.Order.builder()
                .orderCode("ORD-SHIP-999")
                .status("PROCESSING")
                .recipientName("Nguyen Van A")
                .detailedAddress("123 Street")
                .build();

        when(popNowService.requestShipment(1L, req)).thenReturn(mockOrder);

        ResponseEntity<ApiResponse<java.util.Map<String, Object>>> response = controller.shipCabinetItems(req, principal);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        assertEquals("ORD-SHIP-999", response.getBody().getData().get("orderCode"));
        assertEquals("/orders/ORD-SHIP-999", response.getBody().getData().get("redirectUrl"));
    }
}
