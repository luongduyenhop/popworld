package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.response.RefundResponse;
import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.Refund;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.repository.OrderRepository;
import com.manguonmo.popworld.repository.RefundRepository;
import com.manguonmo.popworld.service.impl.RefundServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private RefundServiceImpl refundService;

    private Order paidOrder;

    @BeforeEach
    void setUp() {
        paidOrder = Order.builder()
                .id(1L)
                .orderCode("PW-100")
                .totalAmount(BigDecimal.valueOf(250000))
                .status("PROCESSING")
                .paidAt(LocalDateTime.now().minusHours(2))
                .paymentMethod("VNPAY")
                .build();
    }

    @Test
    @DisplayName("processManualRefund: Ném lỗi nếu mã đơn hàng rỗng")
    void processManualRefund_EmptyOrderCode_ThrowsBadRequest() {
        assertThrows(BadRequestException.class, () ->
                refundService.processManualRefund("", BigDecimal.valueOf(50000), "Lý do", "admin"));
    }

    @Test
    @DisplayName("processManualRefund: Ném lỗi ResourceNotFoundException nếu không tìm thấy đơn")
    void processManualRefund_OrderNotFound_ThrowsResourceNotFoundException() {
        when(orderRepository.findByOrderCode("PW-999")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                refundService.processManualRefund("PW-999", BigDecimal.valueOf(50000), "Lý do", "admin"));
    }

    @Test
    @DisplayName("processManualRefund: Ném lỗi nếu đơn hàng chưa có paidAt (chưa thanh toán)")
    void processManualRefund_UnpaidOrder_ThrowsBadRequestException() {
        paidOrder.setPaidAt(null);
        when(orderRepository.findByOrderCode("PW-100")).thenReturn(Optional.of(paidOrder));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                refundService.processManualRefund("PW-100", BigDecimal.valueOf(50000), "Lý do", "admin"));

        assertTrue(ex.getMessage().contains("chưa được thanh toán"));
        verifyNoInteractions(refundRepository);
    }

    @Test
    @DisplayName("processManualRefund: Ném lỗi nếu đơn hàng ở trạng thái TO_PAY")
    void processManualRefund_ToPayStatus_ThrowsBadRequestException() {
        paidOrder.setStatus("TO_PAY");
        when(orderRepository.findByOrderCode("PW-100")).thenReturn(Optional.of(paidOrder));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                refundService.processManualRefund("PW-100", BigDecimal.valueOf(50000), "Lý do", "admin"));

        assertTrue(ex.getMessage().contains("TO_PAY"));
    }

    @Test
    @DisplayName("processManualRefund: Ném lỗi nếu đơn hàng ở trạng thái EXPIRED")
    void processManualRefund_ExpiredStatus_ThrowsBadRequestException() {
        paidOrder.setStatus("EXPIRED");
        when(orderRepository.findByOrderCode("PW-100")).thenReturn(Optional.of(paidOrder));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                refundService.processManualRefund("PW-100", BigDecimal.valueOf(50000), "Lý do", "admin"));

        assertTrue(ex.getMessage().contains("EXPIRED"));
    }

    @Test
    @DisplayName("processManualRefund: Ném lỗi nếu số tiền hoàn <= 0 hoặc null")
    void processManualRefund_InvalidAmount_ThrowsBadRequestException() {
        when(orderRepository.findByOrderCode("PW-100")).thenReturn(Optional.of(paidOrder));

        assertThrows(BadRequestException.class, () ->
                refundService.processManualRefund("PW-100", BigDecimal.ZERO, "Lý do", "admin"));

        assertThrows(BadRequestException.class, () ->
                refundService.processManualRefund("PW-100", BigDecimal.valueOf(-5000), "Lý do", "admin"));

        assertThrows(BadRequestException.class, () ->
                refundService.processManualRefund("PW-100", null, "Lý do", "admin"));
    }

    @Test
    @DisplayName("processManualRefund: Ném lỗi nếu đơn hàng đã được hoàn tiền đầy đủ (100%)")
    void processManualRefund_AlreadyFullyRefunded_ThrowsBadRequestException() {
        when(orderRepository.findByOrderCode("PW-100")).thenReturn(Optional.of(paidOrder));
        when(refundRepository.sumCompletedRefundAmountByOrderId(1L)).thenReturn(BigDecimal.valueOf(250000));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                refundService.processManualRefund("PW-100", BigDecimal.valueOf(10000), "Lý do", "admin"));

        assertTrue(ex.getMessage().contains("đã được hoàn tiền đầy đủ"));
    }

    @Test
    @DisplayName("processManualRefund: Ném lỗi nếu số tiền hoàn vượt quá số tiền còn lại (Over-refund)")
    void processManualRefund_OverRefund_ThrowsBadRequestException() {
        when(orderRepository.findByOrderCode("PW-100")).thenReturn(Optional.of(paidOrder));
        when(refundRepository.sumCompletedRefundAmountByOrderId(1L)).thenReturn(BigDecimal.valueOf(200000)); // Còn 50,000

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                refundService.processManualRefund("PW-100", BigDecimal.valueOf(60000), "Hoàn quá số còn lại", "admin"));

        assertTrue(ex.getMessage().contains("vượt quá số tiền còn lại"));
    }

    @Test
    @DisplayName("processManualRefund: Hoàn tiền toàn phần thành công và lưu bản ghi COMPLETED")
    void processManualRefund_SuccessfulFullRefund() {
        when(orderRepository.findByOrderCode("PW-100")).thenReturn(Optional.of(paidOrder));
        when(refundRepository.sumCompletedRefundAmountByOrderId(1L)).thenReturn(BigDecimal.ZERO);
        when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Refund refund = refundService.processManualRefund("PW-100", BigDecimal.valueOf(250000), "Hàng lỗi", "admin@popworld.com");

        assertNotNull(refund);
        assertEquals(BigDecimal.valueOf(250000), refund.getAmount());
        assertEquals("COMPLETED", refund.getStatus());
        assertEquals("admin@popworld.com", refund.getProcessedBy());
        assertEquals("MANUAL_ONLINE", refund.getRefundMethod());
        assertEquals("Hàng lỗi", refund.getReason());
        assertNotNull(refund.getRefundCode());
        assertTrue(refund.getRefundCode().startsWith("RF-"));

        // Không làm thay đổi trạng thái giao hàng hay xóa/sửa Order
        assertEquals("PROCESSING", paidOrder.getStatus());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("processManualRefund: Đơn hàng COD ghi nhận phương thức MANUAL_OFFLINE")
    void processManualRefund_CodOrder_SetsManualOfflineMethod() {
        paidOrder.setPaymentMethod("COD");
        paidOrder.setStatus("DELIVERED");
        when(orderRepository.findByOrderCode("PW-100")).thenReturn(Optional.of(paidOrder));
        when(refundRepository.sumCompletedRefundAmountByOrderId(1L)).thenReturn(BigDecimal.ZERO);
        when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Refund refund = refundService.processManualRefund("PW-100", BigDecimal.valueOf(100000), "Khách trả tại quầy", "admin_store");

        assertEquals("MANUAL_OFFLINE", refund.getRefundMethod());
        assertEquals("DELIVERED", paidOrder.getStatus());
    }

    @Test
    @DisplayName("processManualRefund: Hoàn tiền từng phần (Partial) rồi hoàn nốt phần còn lại")
    void processManualRefund_PartialThenRemainingRefund() {
        when(orderRepository.findByOrderCode("PW-100")).thenReturn(Optional.of(paidOrder));
        // Đợt 1: Đã hoàn 100k
        when(refundRepository.sumCompletedRefundAmountByOrderId(1L)).thenReturn(BigDecimal.valueOf(100000));
        when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Hoàn nốt 150k (đúng bằng 250k - 100k)
        Refund secondRefund = refundService.processManualRefund("PW-100", BigDecimal.valueOf(150000), "Đợt 2", "admin");

        assertNotNull(secondRefund);
        assertEquals(BigDecimal.valueOf(150000), secondRefund.getAmount());
        assertEquals("COMPLETED", secondRefund.getStatus());
    }

    @Test
    @DisplayName("getRefundsByOrderCode: Trả về danh sách RefundResponse đúng định dạng")
    void getRefundsByOrderCode_ReturnsMappedDTOs() {
        Refund mockRefund = Refund.builder()
                .id(10L)
                .refundCode("RF-12345")
                .order(paidOrder)
                .amount(BigDecimal.valueOf(50000))
                .reason("Test")
                .processedBy("admin")
                .processedAt(LocalDateTime.now())
                .status("COMPLETED")
                .refundMethod("MANUAL_ONLINE")
                .build();

        when(refundRepository.findByOrderOrderCodeOrderByProcessedAtDesc("PW-100"))
                .thenReturn(List.of(mockRefund));

        List<RefundResponse> responses = refundService.getRefundsByOrderCode("PW-100");

        assertEquals(1, responses.size());
        assertEquals("RF-12345", responses.get(0).getRefundCode());
        assertEquals(BigDecimal.valueOf(50000), responses.get(0).getAmount());
        assertEquals("PW-100", responses.get(0).getOrderCode());
    }

    @Test
    @DisplayName("getRemainingRefundableAmount & getTotalRefundedAmount: Tính toán chính xác")
    void refundCalculations_Accurate() {
        when(orderRepository.findByOrderCode("PW-100")).thenReturn(Optional.of(paidOrder));
        when(refundRepository.sumCompletedRefundAmountByOrderCode("PW-100")).thenReturn(BigDecimal.valueOf(80000));

        BigDecimal totalRefunded = refundService.getTotalRefundedAmount("PW-100");
        BigDecimal remaining = refundService.getRemainingRefundableAmount("PW-100");

        assertEquals(BigDecimal.valueOf(80000), totalRefunded);
        assertEquals(BigDecimal.valueOf(170000), remaining); // 250000 - 80000
    }

    @Test
    @DisplayName("isEligibleForRefund: Đơn đã thanh toán và còn tiền có thể hoàn -> true, ngược lại -> false")
    void isEligibleForRefund_Verification() {
        when(orderRepository.findByOrderCode("PW-100")).thenReturn(Optional.of(paidOrder));
        when(refundRepository.sumCompletedRefundAmountByOrderCode("PW-100")).thenReturn(BigDecimal.valueOf(50000));

        assertTrue(refundService.isEligibleForRefund("PW-100"));

        // Khi đã hoàn hết (250k)
        when(refundRepository.sumCompletedRefundAmountByOrderCode("PW-100")).thenReturn(BigDecimal.valueOf(250000));
        assertFalse(refundService.isEligibleForRefund("PW-100"));

        // Khi đơn chưa thanh toán (paidAt null)
        paidOrder.setPaidAt(null);
        assertFalse(refundService.isEligibleForRefund("PW-100"));
    }
}
