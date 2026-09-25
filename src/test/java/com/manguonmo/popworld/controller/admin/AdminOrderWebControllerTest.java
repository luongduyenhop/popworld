package com.manguonmo.popworld.controller.admin;

import com.manguonmo.popworld.dto.response.OrderResponse;
import com.manguonmo.popworld.dto.response.OrderStatusCountResponse;
import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.OrderItem;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminOrderWebControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private AdminOrderWebController adminOrderWebController;

    @Test
    @DisplayName("listOrders: Hiển thị danh sách đơn hàng và thống kê số lượng")
    void listOrders_ShouldAddAttributesAndReturnView() {
        List<OrderResponse> mockOrders = List.of(OrderResponse.builder().orderCode("PW-001").build());
        OrderStatusCountResponse mockCounts = OrderStatusCountResponse.builder()
                .all(10L).toPay(2L).processing(3L).shipping(2L).delivered(2L).cancelled(1L).expired(0L).build();

        when(orderService.getAllOrders("ALL")).thenReturn(mockOrders);
        when(orderService.getOrderStatusCounts()).thenReturn(mockCounts);

        String view = adminOrderWebController.listOrders("ALL", null, model);

        assertEquals("admin/orders", view);
        verify(model).addAttribute("orders", mockOrders);
        verify(model).addAttribute("currentStatus", "ALL");
        verify(model).addAttribute("countAll", 10L);
        verify(model).addAttribute("countShipping", 2L);
        verify(model).addAttribute("countDelivered", 2L);
        verify(model).addAttribute("countExpired", 0L);
    }

    @Test
    @DisplayName("viewOrderDetail: Hiển thị chi tiết đơn khi tìm thấy")
    void viewOrderDetail_Found_ShouldReturnDetailView() {
        Order mockOrder = Order.builder().id(1L).orderCode("PW-001").build();
        List<OrderItem> mockItems = List.of(OrderItem.builder().id(10L).build());

        when(orderService.getOrderByCode("PW-001")).thenReturn(mockOrder);
        when(orderService.getOrderItems(1L)).thenReturn(mockItems);

        String view = adminOrderWebController.viewOrderDetail("PW-001", model, redirectAttributes);

        assertEquals("admin/order-detail", view);
        verify(model).addAttribute("order", mockOrder);
        verify(model).addAttribute("orderItems", mockItems);
    }

    @Test
    @DisplayName("viewOrderDetail: Không tìm thấy đơn -> redirect về danh sách và báo lỗi")
    void viewOrderDetail_NotFound_ShouldRedirectToList() {
        when(orderService.getOrderByCode("PW-UNKNOWN")).thenReturn(null);

        String view = adminOrderWebController.viewOrderDetail("PW-UNKNOWN", model, redirectAttributes);

        assertEquals("redirect:/admin/orders", view);
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), contains("Không tìm thấy đơn hàng"));
    }

    @Test
    @DisplayName("shipOrder: Chuyển sang SHIPPED thành công và redirect")
    void shipOrder_Success() {
        String view = adminOrderWebController.shipOrder("PW-001", "list", redirectAttributes);

        assertEquals("redirect:/admin/orders", view);
        verify(orderService).shipOrder("PW-001");
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), contains("Đã bàn giao đơn hàng"));
    }

    @Test
    @DisplayName("completeOrder: Chuyển sang COMPLETED thành công và redirect về detail")
    void completeOrder_Success_RedirectDetail() {
        String view = adminOrderWebController.completeOrder("PW-001", "detail", redirectAttributes);

        assertEquals("redirect:/admin/orders/PW-001", view);
        verify(orderService).completeOrder("PW-001");
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), contains("Đã xác nhận giao thành công"));
    }

    @Test
    @DisplayName("cancelOrder: Admin hủy đơn thành công và redirect về list")
    void cancelOrder_Success_RedirectList() {
        String view = adminOrderWebController.cancelOrder("PW-001", "Khách đổi ý", "list", redirectAttributes);

        assertEquals("redirect:/admin/orders", view);
        verify(orderService).adminCancelOrder("PW-001", "Khách đổi ý");
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), contains("Đã hủy đơn hàng"));
    }

    @Test
    @DisplayName("cancelOrder: Admin hủy đơn thành công và redirect về detail")
    void cancelOrder_Success_RedirectDetail() {
        String view = adminOrderWebController.cancelOrder("PW-001", "Lỗi tồn kho", "detail", redirectAttributes);

        assertEquals("redirect:/admin/orders/PW-001", view);
        verify(orderService).adminCancelOrder("PW-001", "Lỗi tồn kho");
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), contains("Đã hủy đơn hàng"));
    }

    @Test
    @DisplayName("cancelOrder: Thất bại do vi phạm state (ví dụ đơn SHIPPED) -> flash errorMessage")
    void cancelOrder_Fail_ShouldSetErrorMessage() {
        doThrow(new BadRequestException("Đơn hàng đang giao hàng, không thể hủy"))
                .when(orderService).adminCancelOrder("PW-001", "Hủy ngang");

        String view = adminOrderWebController.cancelOrder("PW-001", "Hủy ngang", "detail", redirectAttributes);

        assertEquals("redirect:/admin/orders/PW-001", view);
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), contains("Hủy đơn thất bại"));
    }
}
