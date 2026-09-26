package com.manguonmo.popworld.controller.api;

import com.manguonmo.popworld.dto.response.ApiResponse;
import com.manguonmo.popworld.dto.response.OrderResponse;
import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.mapper.OrderMapper;
import com.manguonmo.popworld.service.OrderService;
import com.manguonmo.popworld.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderApiControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private OrderApiController orderApiController;

    @Test
    @DisplayName("getOrderDetail trả về 200 kèm OrderResponse khi tìm thấy đơn")
    public void getOrderDetail_shouldReturnOrderResponse_whenOrderExists() {
        Order order = Order.builder().id(1L).orderCode("PW-123456").build();
        OrderResponse expectedResponse = OrderResponse.builder().orderCode("PW-123456").build();

        when(orderService.getOrderByCode("PW-123456")).thenReturn(order);
        when(orderService.getOrderItems(1L)).thenReturn(List.of());
        when(orderMapper.toResponse(order, List.of())).thenReturn(expectedResponse);

        ResponseEntity<ApiResponse<OrderResponse>> response = orderApiController.getOrderDetail("PW-123456");

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        assertEquals("PW-123456", response.getBody().getData().getOrderCode());
    }

    @Test
    @DisplayName("getOrderDetail ném ResourceNotFoundException khi không thấy đơn")
    public void getOrderDetail_shouldThrowException_whenOrderNotFound() {
        when(orderService.getOrderByCode("PW-UNKNOWN")).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> {
            orderApiController.getOrderDetail("PW-UNKNOWN");
        });
    }

    @Test
    @DisplayName("getOrderStatus trả về trạng thái thanh toán đúng khi là chủ đơn hàng")
    public void getOrderStatus_shouldReturnPaymentStatus() {
        User owner = User.builder().id(5L).email("owner@popworld.com").role("ROLE_USER").build();
        Order order = Order.builder()
                .orderCode("PW-789")
                .status("PROCESSING")
                .user(owner)
                .build();
        Principal principal = () -> "owner@popworld.com";

        when(orderService.getOrderByCode("PW-789")).thenReturn(order);
        when(userService.getUserByEmail("owner@popworld.com")).thenReturn(owner);

        ResponseEntity<ApiResponse<Map<String, Object>>> response = orderApiController.getOrderStatus("PW-789", principal);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> data = response.getBody().getData();
        assertEquals("PW-789", data.get("orderCode"));
        assertEquals("PROCESSING", data.get("status"));
        assertEquals(true, data.get("isPaid"));
    }

    @Test
    @DisplayName("getOrderStatus ném BadRequestException khi user khác cố poll trạng thái đơn (IDOR)")
    public void getOrderStatus_shouldThrowBadRequest_whenOtherUserPolls() {
        User owner = User.builder().id(5L).email("owner@popworld.com").role("ROLE_USER").build();
        User attacker = User.builder().id(99L).email("attacker@popworld.com").role("ROLE_USER").build();
        Order order = Order.builder()
                .orderCode("PW-789")
                .status("TO_PAY")
                .user(owner)
                .build();
        Principal principal = () -> "attacker@popworld.com";

        when(orderService.getOrderByCode("PW-789")).thenReturn(order);
        when(userService.getUserByEmail("attacker@popworld.com")).thenReturn(attacker);

        assertThrows(BadRequestException.class, () ->
                orderApiController.getOrderStatus("PW-789", principal));
    }

    @Test
    @DisplayName("getOrderStatus ném BadRequestException khi Principal null")
    public void getOrderStatus_shouldThrowBadRequest_whenPrincipalNull() {
        User owner = User.builder().id(5L).email("owner@popworld.com").role("ROLE_USER").build();
        Order order = Order.builder()
                .orderCode("PW-789")
                .status("TO_PAY")
                .user(owner)
                .build();

        when(orderService.getOrderByCode("PW-789")).thenReturn(order);

        assertThrows(BadRequestException.class, () ->
                orderApiController.getOrderStatus("PW-789", null));
    }


    @Test
    @DisplayName("getMyOrders trả về danh sách đơn của người dùng khi có Principal")
    public void getMyOrders_shouldReturnOrderList() {
        User user = User.builder().id(10L).build();
        Order order = Order.builder().id(1L).orderCode("PW-1").build();
        OrderResponse orderRes = OrderResponse.builder().orderCode("PW-1").build();
        Principal principal = () -> "test@popworld.com";

        when(userService.getUserByEmail("test@popworld.com")).thenReturn(user);
        when(orderService.getOrdersByUser(10L)).thenReturn(List.of(order));
        when(orderService.getOrderItems(1L)).thenReturn(List.of());
        when(orderMapper.toResponse(order, List.of())).thenReturn(orderRes);

        ResponseEntity<ApiResponse<List<OrderResponse>>> response = orderApiController.getMyOrders(principal);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
        assertEquals("PW-1", response.getBody().getData().get(0).getOrderCode());
    }

    @Test
    @DisplayName("getMyOrders ném BadRequestException khi Principal null")
    public void getMyOrders_shouldThrowBadRequest_whenPrincipalNull() {
        assertThrows(BadRequestException.class, () -> orderApiController.getMyOrders(null));
        verifyNoInteractions(userService);
        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("getOrderDetail(orderCode, principal) thành công khi là chủ đơn hàng")
    public void getOrderDetail_withPrincipal_shouldReturnOrder_whenOwner() {
        User user = User.builder().id(5L).email("owner@popworld.com").role("ROLE_USER").build();
        Order order = Order.builder().id(20L).orderCode("PW-OWNER").user(user).build();
        OrderResponse expectedResponse = OrderResponse.builder().orderCode("PW-OWNER").build();
        Principal principal = () -> "owner@popworld.com";

        when(orderService.getOrderByCode("PW-OWNER")).thenReturn(order);
        when(userService.getUserByEmail("owner@popworld.com")).thenReturn(user);
        when(orderService.getOrderItems(20L)).thenReturn(List.of());
        when(orderMapper.toResponse(order, List.of())).thenReturn(expectedResponse);

        ResponseEntity<ApiResponse<OrderResponse>> response = orderApiController.getOrderDetail("PW-OWNER", principal);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("PW-OWNER", response.getBody().getData().getOrderCode());
    }

    @Test
    @DisplayName("getOrderDetail(orderCode, principal) thành công khi là Admin xem đơn người khác")
    public void getOrderDetail_withPrincipal_shouldReturnOrder_whenAdmin() {
        User owner = User.builder().id(5L).email("owner@popworld.com").role("ROLE_USER").build();
        User admin = User.builder().id(1L).email("admin@popworld.com").role("ROLE_ADMIN").build();
        Order order = Order.builder().id(20L).orderCode("PW-OWNER").user(owner).build();
        OrderResponse expectedResponse = OrderResponse.builder().orderCode("PW-OWNER").build();
        Principal principal = () -> "admin@popworld.com";

        when(orderService.getOrderByCode("PW-OWNER")).thenReturn(order);
        when(userService.getUserByEmail("admin@popworld.com")).thenReturn(admin);
        when(orderService.getOrderItems(20L)).thenReturn(List.of());
        when(orderMapper.toResponse(order, List.of())).thenReturn(expectedResponse);

        ResponseEntity<ApiResponse<OrderResponse>> response = orderApiController.getOrderDetail("PW-OWNER", principal);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("PW-OWNER", response.getBody().getData().getOrderCode());
    }

    @Test
    @DisplayName("getOrderDetail(orderCode, principal) ném BadRequestException khi truy cập đơn của người khác (IDOR)")
    public void getOrderDetail_withPrincipal_shouldThrowBadRequest_whenOtherUser() {
        User owner = User.builder().id(5L).email("owner@popworld.com").role("ROLE_USER").build();
        User attacker = User.builder().id(99L).email("attacker@popworld.com").role("ROLE_USER").build();
        Order order = Order.builder().id(20L).orderCode("PW-OWNER").user(owner).build();
        Principal principal = () -> "attacker@popworld.com";

        when(orderService.getOrderByCode("PW-OWNER")).thenReturn(order);
        when(userService.getUserByEmail("attacker@popworld.com")).thenReturn(attacker);

        assertThrows(BadRequestException.class, () -> {
            orderApiController.getOrderDetail("PW-OWNER", principal);
        });
    }

    @Test
    @DisplayName("getOrderDetail(orderCode, principal) ném BadRequestException khi Principal null")
    public void getOrderDetail_withPrincipal_shouldThrowBadRequest_whenPrincipalNull() {
        assertThrows(BadRequestException.class, () -> {
            orderApiController.getOrderDetail("PW-123456", null);
        });
    }
}
