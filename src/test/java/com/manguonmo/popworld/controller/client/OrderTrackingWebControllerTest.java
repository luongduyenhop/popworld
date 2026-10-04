package com.manguonmo.popworld.controller.client;

import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.OrderItem;
import com.manguonmo.popworld.entity.OrderTimeline;
import com.manguonmo.popworld.service.CategoryService;
import com.manguonmo.popworld.service.CharacterIpService;
import com.manguonmo.popworld.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderTrackingWebControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private CharacterIpService characterIpService;

    @Mock
    private Model model;

    @InjectMocks
    private OrderTrackingWebController controller;

    @Test
    @DisplayName("showTrackingPage: Hiển thị form tra cứu khi không có tham số")
    void showTrackingPage_WithoutParams_ReturnsView() {
        String view = controller.showTrackingPage(null, null, model);
        assertEquals("order-tracking", view);
        verify(model).addAttribute("orderCode", null);
        verify(model).addAttribute("phone", null);
    }

    @Test
    @DisplayName("lookupOrder: Tra cứu thành công khi mã đơn và SĐT khớp")
    void lookupOrder_Success_WhenPhoneMatches() {
        Order order = Order.builder()
                .id(1L)
                .orderCode("PW-100")
                .recipientPhone("0987654321")
                .status("PROCESSING")
                .build();

        when(orderService.getOrderByCode("PW-100")).thenReturn(order);
        when(orderService.getOrderItems(1L)).thenReturn(List.of());
        when(orderService.getOrderTimelines("PW-100")).thenReturn(List.of());

        String view = controller.lookupOrder("PW-100", "0987654321", model);

        assertEquals("order-tracking", view);
        verify(model).addAttribute("order", order);
        verify(model).addAttribute("searchSuccess", true);
    }

    @Test
    @DisplayName("lookupOrder: Báo lỗi khi không tìm thấy mã đơn hàng")
    void lookupOrder_OrderNotFound_ShowsError() {
        when(orderService.getOrderByCode("PW-UNKNOWN")).thenReturn(null);

        String view = controller.lookupOrder("PW-UNKNOWN", "0987654321", model);

        assertEquals("order-tracking", view);
        verify(model).addAttribute(eq("errorMessage"), contains("Không tìm thấy đơn hàng"));
    }

    @Test
    @DisplayName("lookupOrder: Báo lỗi khi số điện thoại không khớp")
    void lookupOrder_PhoneMismatch_ShowsError() {
        Order order = Order.builder()
                .id(1L)
                .orderCode("PW-100")
                .recipientPhone("0987654321")
                .build();

        when(orderService.getOrderByCode("PW-100")).thenReturn(order);

        String view = controller.lookupOrder("PW-100", "0911111111", model);

        assertEquals("order-tracking", view);
        verify(model).addAttribute(eq("errorMessage"), contains("Số điện thoại"));
    }
}
