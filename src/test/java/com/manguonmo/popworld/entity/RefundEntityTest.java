package com.manguonmo.popworld.entity;

import com.manguonmo.popworld.dto.response.RefundResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RefundEntityTest {

    @Test
    @DisplayName("Refund Entity: Builder và default values thiết lập chính xác")
    void refundBuilder_DefaultValues() {
        Order order = Order.builder().id(10L).orderCode("PW-999").build();
        LocalDateTime now = LocalDateTime.now();

        Refund refund = Refund.builder()
                .id(1L)
                .refundCode("RF-001")
                .order(order)
                .amount(BigDecimal.valueOf(150000))
                .reason("Khách đổi ý")
                .processedBy("admin_user")
                .processedAt(now)
                .build();

        assertEquals(1L, refund.getId());
        assertEquals("RF-001", refund.getRefundCode());
        assertEquals(order, refund.getOrder());
        assertEquals(BigDecimal.valueOf(150000), refund.getAmount());
        assertEquals("Khách đổi ý", refund.getReason());
        assertEquals("admin_user", refund.getProcessedBy());
        assertEquals(now, refund.getProcessedAt());
        assertEquals("COMPLETED", refund.getStatus());
        assertEquals("MANUAL", refund.getRefundMethod());
    }

    @Test
    @DisplayName("RefundResponse.fromEntity: Chuyển đổi DTO an toàn bao gồm cả null handling")
    void refundResponse_FromEntity() {
        assertNull(RefundResponse.fromEntity(null));

        Order order = Order.builder().id(5L).orderCode("PW-555").build();
        LocalDateTime now = LocalDateTime.now();

        Refund refund = Refund.builder()
                .id(2L)
                .refundCode("RF-002")
                .order(order)
                .amount(BigDecimal.valueOf(75000))
                .reason("Lỗi bao bì")
                .processedBy("admin_support")
                .processedAt(now)
                .status("COMPLETED")
                .refundMethod("MANUAL_ONLINE")
                .build();

        RefundResponse response = RefundResponse.fromEntity(refund);

        assertNotNull(response);
        assertEquals(2L, response.getId());
        assertEquals("RF-002", response.getRefundCode());
        assertEquals(5L, response.getOrderId());
        assertEquals("PW-555", response.getOrderCode());
        assertEquals(BigDecimal.valueOf(75000), response.getAmount());
        assertEquals("Lỗi bao bì", response.getReason());
        assertEquals("admin_support", response.getProcessedBy());
        assertEquals(now, response.getProcessedAt());
        assertEquals("COMPLETED", response.getStatus());
        assertEquals("MANUAL_ONLINE", response.getRefundMethod());
    }
}
