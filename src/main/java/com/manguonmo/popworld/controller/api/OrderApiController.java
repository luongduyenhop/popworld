package com.manguonmo.popworld.controller.api;

import com.manguonmo.popworld.dto.response.ApiResponse;
import com.manguonmo.popworld.dto.response.OrderResponse;
import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.OrderItem;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.mapper.OrderMapper;
import com.manguonmo.popworld.repository.OrderItemRepository;
import com.manguonmo.popworld.service.CartService;
import com.manguonmo.popworld.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API Controller chuẩn hóa cho Đơn hàng (Order)
 */
@RestController
@RequestMapping("/api/orders")
public class OrderApiController {

    private final OrderService orderService;
    private final OrderItemRepository orderItemRepository;
    private final OrderMapper orderMapper;
    private final CartService cartService;

    public OrderApiController(OrderService orderService,
                              OrderItemRepository orderItemRepository,
                              OrderMapper orderMapper,
                              CartService cartService) {
        this.orderService = orderService;
        this.orderItemRepository = orderItemRepository;
        this.orderMapper = orderMapper;
        this.cartService = cartService;
    }

    /**
     * Lấy chi tiết đơn hàng theo mã đơn
     */
    @GetMapping("/{orderCode}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderDetail(@PathVariable String orderCode) {
        Order order = orderService.getOrderByCode(orderCode);
        if (order == null) {
            throw new ResourceNotFoundException("Không tìm thấy đơn hàng với mã: " + orderCode);
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        OrderResponse response = orderMapper.toResponse(order, items);

        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin đơn hàng thành công", response));
    }

    /**
     * API Polling trạng thái đơn hàng (phục vụ giao diện đếm ngược & thanh toán VietQR)
     */
    @GetMapping("/{orderCode}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrderStatus(@PathVariable String orderCode) {
        Order order = orderService.getOrderByCode(orderCode);
        if (order == null) {
            throw new ResourceNotFoundException("Không tìm thấy đơn hàng với mã: " + orderCode);
        }

        boolean isPaid = !"TO_PAY".equalsIgnoreCase(order.getStatus()) && !"CANCELLED".equalsIgnoreCase(order.getStatus());

        Map<String, Object> statusData = Map.of(
                "orderCode", order.getOrderCode(),
                "status", order.getStatus(),
                "isPaid", isPaid
        );

        return ResponseEntity.ok(ApiResponse.success("Lấy trạng thái đơn hàng thành công", statusData));
    }

    /**
     * Lấy danh sách đơn hàng của người dùng hiện tại
     */
    @GetMapping("/my-orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders() {
        User user = cartService.getDefaultUser();
        List<Order> orders = orderService.getOrdersByUser(user.getId());

        List<OrderResponse> responseList = orders.stream()
                .map(order -> {
                    List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
                    return orderMapper.toResponse(order, items);
                })
                .toList();

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn hàng thành công", responseList));
    }
}
