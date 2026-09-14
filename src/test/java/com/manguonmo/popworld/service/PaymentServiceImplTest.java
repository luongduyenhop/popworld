package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.SePayWebhookRequest;
import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.repository.OrderRepository;
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
}
