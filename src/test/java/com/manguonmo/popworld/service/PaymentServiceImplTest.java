package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.request.SePayWebhookRequest;
import com.manguonmo.popworld.entity.BoxReservation;
import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.ReservationStatus;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.repository.BoxReservationRepository;
import com.manguonmo.popworld.repository.OrderRepository;
import com.manguonmo.popworld.service.PopNowService;
import com.manguonmo.popworld.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit Test kiểm thử toàn diện cho PaymentServiceImpl
 * 
 * Phạm vi kiểm thử:
 * 1. Bảo mật: Xác thực Authorization Header (Bearer / Apikey)
 * 2. Regex: Trích xuất mã đơn hàng PW-\\d+ từ nội dung chuyển khoản
 * 3. Idempotency: Xử lý chống trùng lặp khi webhook gửi lại
 * 4. Đối soát số tiền (Amount Reconciliation): Khách chuyển thiếu vs đủ tiền
 * 5. Cập nhật trạng thái: Đổi sang PROCESSING, lưu paidAt và mã tham chiếu
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private BoxReservationRepository boxReservationRepository;

    @Mock
    private PopNowService popNowService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private static final String VALID_API_KEY = "sepay_secret_token_123456";
    private static final String VALID_AUTH_HEADER = "Apikey sepay_secret_token_123456";

    @BeforeEach
    void setUp() {
        // Gán giá trị API Key bí mật cho trường private @Value apiKey thông qua ReflectionTestUtils
        ReflectionTestUtils.setField(paymentService, "apiKey", VALID_API_KEY);
    }

    // =========================================================================
    // TEST CASE 1: Bảo mật - Header không hợp lệ hoặc thiếu API Key
    // =========================================================================
    @Test
    @DisplayName("Bảo mật: Từ chối request khi Authorization Header là null")
    void processSePayWebhook_HeaderNull_ShouldReturnFalse() {
        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .content("PW-1726000000000")
                .transferAmount(new BigDecimal("250000"))
                .build();

        boolean result = paymentService.processSePayWebhook(request, null);

        assertFalse(result, "Phải từ chối khi header null");
        verify(orderRepository, never()).findByOrderCode(anyString());
    }

    @Test
    @DisplayName("Bảo mật: Từ chối request khi Authorization Header sai API Key")
    void processSePayWebhook_WrongApiKey_ShouldReturnFalse() {
        SePayWebhookRequest request = SePayWebhookRequest.builder()
            .content("PW-1726000000000")
            .transferAmount(new BigDecimal("250000"))
            .build();

        boolean result = paymentService.processSePayWebhook(request, "Apikey wrong_hacker_key");

        assertFalse(result, "Phải từ chối khi API Key không khớp");
        verify(orderRepository, never()).findByOrderCode(anyString());
    }

    @Test
    @DisplayName("Bảo mật: Từ chối request khi Authorization Header chứa API Key dạng substring nối dài")
    void processSePayWebhook_SubstringApiKey_ShouldReturnFalse() {
        SePayWebhookRequest request = SePayWebhookRequest.builder()
            .content("PW-1726000000000")
            .transferAmount(new BigDecimal("250000"))
            .build();

        boolean result = paymentService.processSePayWebhook(request, "Apikey " + VALID_API_KEY + "_attacker_suffix");

        assertFalse(result, "Phải từ chối khi API Key bị nối thêm ký tự");
        verify(orderRepository, never()).findByOrderCode(anyString());
    }

    @Test
    @DisplayName("Bảo mật: Chấp nhận request khi Authorization Header sử dụng scheme Bearer chuẩn")
    void processSePayWebhook_BearerScheme_ShouldSucceed() {
        SePayWebhookRequest request = SePayWebhookRequest.builder()
            .content("PW-1726888888888")
            .transferAmount(new BigDecimal("500000"))
            .referenceCode("FT240999999")
            .build();

        Order order = Order.builder()
            .orderCode("PW-1726888888888")
            .status("TO_PAY")
            .totalAmount(new BigDecimal("500000"))
            .build();

        when(orderRepository.findByOrderCode("PW-1726888888888")).thenReturn(Optional.of(order));

        boolean result = paymentService.processSePayWebhook(request, "Bearer " + VALID_API_KEY);

        assertTrue(result, "Phải chấp nhận Bearer scheme hợp lệ");
        assertEquals("PROCESSING", order.getStatus());
        verify(orderRepository, times(1)).save(order);
    }

    // =========================================================================
    // TEST CASE 2: Regex - Nội dung chuyển khoản không hợp lệ
    // =========================================================================
    @Test
    @DisplayName("Regex: Từ chối khi nội dung chuyển khoản không chứa mã đơn dạng PW-\\d+")
    void processSePayWebhook_InvalidContent_ShouldReturnFalse() {
        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .content("Nguyen Van A chuyen tien mua hang khong ghi ma")
                .transferAmount(new BigDecimal("500000"))
                .build();

        boolean result = paymentService.processSePayWebhook(request, VALID_AUTH_HEADER);

        assertFalse(result, "Phải từ chối khi không tìm thấy mã PW-");
        verify(orderRepository, never()).findByOrderCode(anyString());
    }

    @Test
    @DisplayName("Regex: Từ chối an toàn khi cả content và description đều là null")
    void processSePayWebhook_NullContentAndDescription_ShouldReturnFalse() {
        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .content(null)
                .description(null)
                .transferAmount(new BigDecimal("500000"))
                .build();

        boolean result = paymentService.processSePayWebhook(request, VALID_AUTH_HEADER);

        assertFalse(result, "Phải từ chối khi content và description đều null");
        verify(orderRepository, never()).findByOrderCode(anyString());
    }

    // =========================================================================
    // TEST CASE 3: Tìm kiếm đơn hàng - Không tìm thấy trong CSDL
    // =========================================================================
    @Test
    @DisplayName("Đơn hàng: Trả về false khi không tìm thấy đơn hàng trong CSDL")
    void processSePayWebhook_OrderNotFound_ShouldReturnFalse() {
        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .content("Thanh toan don hang PW-9999999999")
                .transferAmount(new BigDecimal("500000"))
                .build();

        when(orderRepository.findByOrderCode("PW-9999999999")).thenReturn(Optional.empty());

        boolean result = paymentService.processSePayWebhook(request, VALID_AUTH_HEADER);

        assertFalse(result, "Phải trả về false khi đơn hàng không tồn tại");
        verify(orderRepository, never()).save(any(Order.class));
    }

    // =========================================================================
    // TEST CASE 4: Tính Bất biến (Idempotency) - Đơn hàng đã được thanh toán trước đó
    // =========================================================================
    @Test
    @DisplayName("Idempotency: Trả về true ngay khi đơn đã ở trạng thái PROCESSING/COMPLETED để tránh xử lý lặp")
    void processSePayWebhook_AlreadyPaid_ShouldReturnTrueWithoutUpdating() {
        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .content("PW-1726000000001")
                .transferAmount(new BigDecimal("300000"))
                .build();

        Order order = Order.builder()
                .orderCode("PW-1726000000001")
                .status("PROCESSING")
                .totalAmount(new BigDecimal("300000"))
                .build();

        when(orderRepository.findByOrderCode("PW-1726000000001")).thenReturn(Optional.of(order));

        boolean result = paymentService.processSePayWebhook(request, VALID_AUTH_HEADER);

        assertTrue(result, "Phải trả về true cho SePay để không gửi lại webhook nữa");
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Đơn hàng: Trả về false khi đơn hàng đã bị hủy (CANCELLED)")
    void processSePayWebhook_CancelledOrder_ShouldReturnFalse() {
        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .content("PW-1726000000002")
                .transferAmount(new BigDecimal("300000"))
                .build();

        Order order = Order.builder()
                .orderCode("PW-1726000000002")
                .status("CANCELLED")
                .totalAmount(new BigDecimal("300000"))
                .build();

        when(orderRepository.findByOrderCode("PW-1726000000002")).thenReturn(Optional.of(order));

        boolean result = paymentService.processSePayWebhook(request, VALID_AUTH_HEADER);

        assertFalse(result, "Phải trả về false khi đơn đã bị hủy để cảnh báo CSKH");
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Đơn hàng: Trả về false khi đơn hàng đã hết hạn (EXPIRED)")
    void processSePayWebhook_ExpiredOrder_ShouldReturnFalse() {
        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .content("PW-1726000000002")
                .transferAmount(new BigDecimal("300000"))
                .build();

        Order order = Order.builder()
                .orderCode("PW-1726000000002")
                .status("EXPIRED")
                .totalAmount(new BigDecimal("300000"))
                .build();

        when(orderRepository.findByOrderCode("PW-1726000000002")).thenReturn(Optional.of(order));

        boolean result = paymentService.processSePayWebhook(request, VALID_AUTH_HEADER);

        assertFalse(result, "Phải trả về false khi đơn đã hết hạn 15 phút");
        verify(orderRepository, never()).save(any(Order.class));
    }


    // =========================================================================
    // TEST CASE 5: Đối soát số tiền - Khách chuyển thiếu tiền
    // =========================================================================
    @Test
    @DisplayName("Đối soát: Từ chối khi số tiền chuyển khoản nhỏ hơn tổng tiền đơn hàng")
    void processSePayWebhook_Underpaid_ShouldReturnFalse() {
        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .content("Chuyen khoan don hang PW-1726000000003")
                .transferAmount(new BigDecimal("299000")) // Thiếu 1.000đ
                .build();

        Order order = Order.builder()
                .orderCode("PW-1726000000003")
                .status("TO_PAY")
                .totalAmount(new BigDecimal("300000"))
                .build();

        when(orderRepository.findByOrderCode("PW-1726000000003")).thenReturn(Optional.of(order));

        boolean result = paymentService.processSePayWebhook(request, VALID_AUTH_HEADER);

        assertFalse(result, "Phải từ chối khi số tiền chuyển vào không đủ");
        assertEquals("TO_PAY", order.getStatus(), "Trạng thái không được đổi sang PROCESSING");
        verify(orderRepository, never()).save(any(Order.class));
    }

    // =========================================================================
    // TEST CASE 6: Thành công - Khách chuyển đủ tiền, bóc tách đúng từ description
    // =========================================================================
    @Test
    @DisplayName("Thành công: Khách chuyển đủ tiền, cập nhật đơn sang PROCESSING và lưu vết giao dịch")
    void processSePayWebhook_Success_ShouldUpdateOrder() {
        // Mô phỏng nội dung ngân hàng thực tế có tiền tố và hậu tố
        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .content("MBVCB.0987654321.PW-1726888888888.PopWorld Store")
                .transferAmount(new BigDecimal("500000.00")) // Định dạng có scale .00
                .referenceCode("FT24091234567890")
                .build();

        Order order = Order.builder()
                .orderCode("PW-1726888888888")
                .status("TO_PAY")
                .totalAmount(new BigDecimal("500000"))
                .build();

        when(orderRepository.findByOrderCode("PW-1726888888888")).thenReturn(Optional.of(order));

        boolean result = paymentService.processSePayWebhook(request, VALID_AUTH_HEADER);

        assertTrue(result, "Xử lý webhook phải thành công");
        assertEquals("PROCESSING", order.getStatus(), "Trạng thái phải chuyển sang PROCESSING");
        assertNotNull(order.getPaidAt(), "Thời gian thanh toán không được null");
        assertEquals("FT24091234567890", order.getNote(), "Ghi chú phải lưu mã tham chiếu giao dịch");

        // Xác minh orderRepository.save() được gọi đúng 1 lần với đơn hàng đã cập nhật
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(1)).save(orderCaptor.capture());
        assertEquals("PROCESSING", orderCaptor.getValue().getStatus());
    }

    // =========================================================================
    // TEST CASE 7: Hardening Null Safety & API Key Bypass Prevention
    // =========================================================================
    @Test
    @DisplayName("Bảo mật: Từ chối khi webhookData payload là null")
    void processSePayWebhook_NullPayload_ShouldReturnFalse() {
        boolean result = paymentService.processSePayWebhook(null, VALID_AUTH_HEADER);

        assertFalse(result, "Phải từ chối an toàn khi payload null mà không gây NPE");
        verify(orderRepository, never()).findByOrderCode(anyString());
    }

    @Test
    @DisplayName("Bảo mật: Từ chối khi apiKey cấu hình rỗng hoặc null trong hệ thống")
    void processSePayWebhook_EmptyApiKeyConfig_ShouldReturnFalse() {
        ReflectionTestUtils.setField(paymentService, "apiKey", "   ");
        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .content("PW-1726000000000")
                .transferAmount(new BigDecimal("250000"))
                .build();

        boolean result = paymentService.processSePayWebhook(request, "Apikey any_key");

        assertFalse(result, "Phải từ chối khi apiKey cấu hình không hợp lệ");
        verify(orderRepository, never()).findByOrderCode(anyString());
    }

    @Test
    @DisplayName("Đối soát: Từ chối khi transferAmount trong webhook là null")
    void processSePayWebhook_NullTransferAmount_ShouldReturnFalse() {
        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .content("PW-1726000000003")
                .transferAmount(null)
                .build();

        Order order = Order.builder()
                .orderCode("PW-1726000000003")
                .status("TO_PAY")
                .totalAmount(new BigDecimal("300000"))
                .build();

        when(orderRepository.findByOrderCode("PW-1726000000003")).thenReturn(Optional.of(order));

        boolean result = paymentService.processSePayWebhook(request, VALID_AUTH_HEADER);

        assertFalse(result, "Phải từ chối khi transferAmount là null");
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Idempotency: Trả về true ngay khi đơn đã DELIVERED để tránh ghi đè trạng thái")
    void processSePayWebhook_AlreadyDelivered_ShouldReturnTrueWithoutUpdating() {
        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .content("PW-1726000000001")
                .transferAmount(new BigDecimal("300000"))
                .build();

        Order order = Order.builder()
                .orderCode("PW-1726000000001")
                .status("DELIVERED")
                .totalAmount(new BigDecimal("300000"))
                .build();

        when(orderRepository.findByOrderCode("PW-1726000000001")).thenReturn(Optional.of(order));

        boolean result = paymentService.processSePayWebhook(request, VALID_AUTH_HEADER);

        assertTrue(result, "Phải trả về true khi đơn đã DELIVERED");
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Trạng thái: Từ chối khi đơn hàng ở trạng thái không hợp lệ khác TO_PAY")
    void processSePayWebhook_InvalidStatus_ShouldReturnFalse() {
        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .content("PW-1726000000005")
                .transferAmount(new BigDecimal("300000"))
                .build();

        Order order = Order.builder()
                .orderCode("PW-1726000000005")
                .status("UNKNOWN_STATUS")
                .totalAmount(new BigDecimal("300000"))
                .build();

        when(orderRepository.findByOrderCode("PW-1726000000005")).thenReturn(Optional.of(order));

        boolean result = paymentService.processSePayWebhook(request, VALID_AUTH_HEADER);

        assertFalse(result, "Phải từ chối khi trạng thái đơn không phải TO_PAY");
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Thành công: Tự động fallback sang mã code hoặc id khi referenceCode null")
    void processSePayWebhook_FallbackReferenceCode_WhenRefCodeNull() {
        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .content("PW-1726999999999")
                .transferAmount(new BigDecimal("400000"))
                .referenceCode(null)
                .code("SEPAY_CODE_123")
                .id(999L)
                .build();

        Order order = Order.builder()
                .orderCode("PW-1726999999999")
                .status("TO_PAY")
                .totalAmount(new BigDecimal("400000"))
                .build();

        when(orderRepository.findByOrderCode("PW-1726999999999")).thenReturn(Optional.of(order));

        boolean result = paymentService.processSePayWebhook(request, VALID_AUTH_HEADER);

        assertTrue(result);
        assertEquals("SEPAY_CODE_123", order.getNote());
        verify(orderRepository, times(1)).save(order);
    }

    // =========================================================================
    // TEST CASE 6: POP NOW Webhook Integration & Error Handling
    // =========================================================================
    @Test
    @DisplayName("POP NOW Webhook: Thanh toán thành công -> markPurchased được gọi và Order chuyển PROCESSING")
    void processSePayWebhook_PopNow_Success_ShouldCallMarkPurchasedAndSetProcessing() {
        String orderCode = "PW-1726000000888";
        String resCode = "PN-TEST123456";

        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .content(orderCode)
                .transferAmount(new BigDecimal("300000"))
                .referenceCode("REF-POPNOW-OK")
                .build();

        Order order = Order.builder()
                .orderCode(orderCode)
                .status("TO_PAY")
                .totalAmount(new BigDecimal("300000"))
                .deliveryMethod("POP_NOW_CABINET")
                .build();

        BoxReservation reservation = BoxReservation.builder()
                .reservationCode(resCode)
                .status(ReservationStatus.RESERVED)
                .orderCode(orderCode)
                .build();

        when(orderRepository.findByOrderCode(orderCode)).thenReturn(Optional.of(order));
        when(boxReservationRepository.findByOrderCode(orderCode)).thenReturn(Optional.of(reservation));

        boolean result = paymentService.processSePayWebhook(request, VALID_AUTH_HEADER);

        assertTrue(result, "Webhook phải trả về true khi thành công");
        verify(popNowService, times(1)).markPurchased(resCode, orderCode);
        assertEquals("PROCESSING", order.getStatus());
        assertNotNull(order.getPaidAt());
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    @DisplayName("POP NOW Webhook: markPurchased thất bại do hết hạn -> Order không thành PROCESSING (chuyển EXPIRED), webhook trả false")
    void processSePayWebhook_PopNow_WhenMarkPurchasedFails_ShouldNotBeProcessingAndReturnFalse() {
        String orderCode = "PW-1726000000999";
        String resCode = "PN-EXPIRED999";

        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .content(orderCode)
                .transferAmount(new BigDecimal("300000"))
                .referenceCode("REF-POPNOW-FAIL")
                .build();

        Order order = Order.builder()
                .orderCode(orderCode)
                .status("TO_PAY")
                .totalAmount(new BigDecimal("300000"))
                .deliveryMethod("POP_NOW_CABINET")
                .build();

        BoxReservation reservation = BoxReservation.builder()
                .reservationCode(resCode)
                .status(ReservationStatus.EXPIRED)
                .orderCode(orderCode)
                .build();

        when(orderRepository.findByOrderCode(orderCode)).thenReturn(Optional.of(order));
        when(boxReservationRepository.findByOrderCode(orderCode)).thenReturn(Optional.of(reservation));
        doThrow(new BadRequestException("Phiếu giữ hộp đã hết hạn, không thể thanh toán!"))
                .when(popNowService).markPurchased(resCode, orderCode);

        boolean result = paymentService.processSePayWebhook(request, VALID_AUTH_HEADER);

        assertFalse(result, "Webhook phải trả về false khi phiếu giữ hộp không thể thanh toán");
        assertNotEquals("PROCESSING", order.getStatus(), "Order tuyệt đối không được chuyển sang PROCESSING");
        assertEquals("EXPIRED", order.getStatus(), "Order được đánh dấu EXPIRED để đồng bộ");
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    @DisplayName("POP NOW Webhook Idempotency: Webhook gọi lại khi Order đã PROCESSING -> Trả về true và gọi markPurchased an toàn")
    void processSePayWebhook_PopNow_IdempotentOnDuplicateCall() {
        String orderCode = "PW-1726000000777";
        String resCode = "PN-DUP777";

        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .content(orderCode)
                .transferAmount(new BigDecimal("300000"))
                .referenceCode("REF-POPNOW-DUP")
                .build();

        Order order = Order.builder()
                .orderCode(orderCode)
                .status("PROCESSING")
                .totalAmount(new BigDecimal("300000"))
                .deliveryMethod("POP_NOW_CABINET")
                .build();

        BoxReservation reservation = BoxReservation.builder()
                .reservationCode(resCode)
                .status(ReservationStatus.PURCHASED)
                .orderCode(orderCode)
                .build();

        when(orderRepository.findByOrderCode(orderCode)).thenReturn(Optional.of(order));
        when(boxReservationRepository.findByOrderCode(orderCode)).thenReturn(Optional.of(reservation));

        boolean result = paymentService.processSePayWebhook(request, VALID_AUTH_HEADER);

        assertTrue(result, "Lần gọi thứ 2 phải trả về true (Idempotent)");
        verify(popNowService, times(1)).markPurchased(resCode, orderCode);
        verify(orderRepository, never()).save(order);
    }

    @Test
    @DisplayName("POP NOW Webhook Idempotency: Webhook gọi lại khi Order đã PROCESSING và Reservation UNBOXED -> Trả về true an toàn")
    void processSePayWebhook_PopNow_IdempotentOnDuplicateCall_WhenUnboxed() {
        String orderCode = "PW-1726000000778";
        String resCode = "PN-UNBOX778";

        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .content(orderCode)
                .transferAmount(new BigDecimal("300000"))
                .referenceCode("REF-POPNOW-UNBOX")
                .build();

        Order order = Order.builder()
                .orderCode(orderCode)
                .status("PROCESSING")
                .totalAmount(new BigDecimal("300000"))
                .deliveryMethod("POP_NOW_CABINET")
                .build();

        BoxReservation reservation = BoxReservation.builder()
                .reservationCode(resCode)
                .status(ReservationStatus.UNBOXED)
                .orderCode(orderCode)
                .build();

        when(orderRepository.findByOrderCode(orderCode)).thenReturn(Optional.of(order));
        when(boxReservationRepository.findByOrderCode(orderCode)).thenReturn(Optional.of(reservation));

        boolean result = paymentService.processSePayWebhook(request, VALID_AUTH_HEADER);

        assertTrue(result, "Đơn đã PROCESSING và hộp đã UNBOXED phải trả về true");
        verify(popNowService, times(1)).markPurchased(resCode, orderCode);
        verify(orderRepository, never()).save(order);
    }

    @Test
    @DisplayName("POP NOW Webhook Idempotency Edge Case: Đơn đã PROCESSING nhưng phiếu giữ hộp EXPIRED -> Trả về false và GIỮ NGUYÊN trạng thái đơn")
    void processSePayWebhook_PopNow_IdempotentOnDuplicateCall_WhenExpired_ShouldReturnFalseAndNotChangeOrder() {
        String orderCode = "PW-1726000000779";
        String resCode = "PN-EXP779";

        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .content(orderCode)
                .transferAmount(new BigDecimal("300000"))
                .referenceCode("REF-POPNOW-DUP-EXP")
                .build();

        Order order = Order.builder()
                .orderCode(orderCode)
                .status("PROCESSING")
                .totalAmount(new BigDecimal("300000"))
                .deliveryMethod("POP_NOW_CABINET")
                .build();

        BoxReservation reservation = BoxReservation.builder()
                .reservationCode(resCode)
                .status(ReservationStatus.EXPIRED)
                .orderCode(orderCode)
                .build();

        when(orderRepository.findByOrderCode(orderCode)).thenReturn(Optional.of(order));
        when(boxReservationRepository.findByOrderCode(orderCode)).thenReturn(Optional.of(reservation));

        boolean result = paymentService.processSePayWebhook(request, VALID_AUTH_HEADER);

        assertFalse(result, "Phải trả về false khi phiếu giữ hộp đã hết hạn");
        assertEquals("PROCESSING", order.getStatus(), "Trạng thái đơn hàng thành công trước đó phải được bảo toàn, không đổi sang EXPIRED");
        verify(orderRepository, never()).save(order);
        verify(popNowService, never()).markPurchased(anyString(), anyString());
    }

    @Test
    @DisplayName("POP NOW Webhook Idempotency Edge Case: Đơn đã PROCESSING nhưng phiếu giữ hộp CANCELLED -> Trả về false và GIỮ NGUYÊN trạng thái đơn")
    void processSePayWebhook_PopNow_IdempotentOnDuplicateCall_WhenCancelled_ShouldReturnFalseAndNotChangeOrder() {
        String orderCode = "PW-1726000000780";
        String resCode = "PN-CANCEL780";

        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .content(orderCode)
                .transferAmount(new BigDecimal("300000"))
                .referenceCode("REF-POPNOW-DUP-CANCEL")
                .build();

        Order order = Order.builder()
                .orderCode(orderCode)
                .status("PROCESSING")
                .totalAmount(new BigDecimal("300000"))
                .deliveryMethod("POP_NOW_CABINET")
                .build();

        BoxReservation reservation = BoxReservation.builder()
                .reservationCode(resCode)
                .status(ReservationStatus.CANCELLED)
                .orderCode(orderCode)
                .build();

        when(orderRepository.findByOrderCode(orderCode)).thenReturn(Optional.of(order));
        when(boxReservationRepository.findByOrderCode(orderCode)).thenReturn(Optional.of(reservation));

        boolean result = paymentService.processSePayWebhook(request, VALID_AUTH_HEADER);

        assertFalse(result, "Phải trả về false khi phiếu giữ hộp đã bị hủy");
        assertEquals("PROCESSING", order.getStatus(), "Trạng thái đơn hàng thành công trước đó phải được bảo toàn, không đổi sang CANCELLED");
        verify(orderRepository, never()).save(order);
        verify(popNowService, never()).markPurchased(anyString(), anyString());
    }

    @Test
    @DisplayName("POP NOW Webhook Idempotency: Đơn đã PROCESSING nhưng markPurchased ném ngoại lệ -> Trả về false và không đổi trạng thái đơn")
    void processSePayWebhook_PopNow_IdempotentOnDuplicateCall_WhenMarkPurchasedThrows_ShouldReturnFalse() {
        String orderCode = "PW-1726000000781";
        String resCode = "PN-FAIL781";

        SePayWebhookRequest request = SePayWebhookRequest.builder()
                .content(orderCode)
                .transferAmount(new BigDecimal("300000"))
                .referenceCode("REF-POPNOW-DUP-FAIL")
                .build();

        Order order = Order.builder()
                .orderCode(orderCode)
                .status("PROCESSING")
                .totalAmount(new BigDecimal("300000"))
                .deliveryMethod("POP_NOW_CABINET")
                .build();

        BoxReservation reservation = BoxReservation.builder()
                .reservationCode(resCode)
                .status(ReservationStatus.RESERVED)
                .orderCode(orderCode)
                .build();

        when(orderRepository.findByOrderCode(orderCode)).thenReturn(Optional.of(order));
        when(boxReservationRepository.findByOrderCode(orderCode)).thenReturn(Optional.of(reservation));
        doThrow(new RuntimeException("Database timeout during markPurchased"))
                .when(popNowService).markPurchased(resCode, orderCode);

        boolean result = paymentService.processSePayWebhook(request, VALID_AUTH_HEADER);

        assertFalse(result, "Phải trả về false khi markPurchased gặp sự cố");
        assertEquals("PROCESSING", order.getStatus(), "Trạng thái đơn hàng phải được giữ nguyên");
        verify(orderRepository, never()).save(order);
    }
}
