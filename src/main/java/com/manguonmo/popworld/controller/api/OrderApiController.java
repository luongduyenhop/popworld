package com.manguonmo.popworld.controller.api;

import com.manguonmo.popworld.dto.response.ApiResponse;
import com.manguonmo.popworld.dto.response.OrderResponse;
import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.OrderItem;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.mapper.OrderMapper;
import com.manguonmo.popworld.repository.BoxReservationRepository;
import com.manguonmo.popworld.service.OrderService;
import com.manguonmo.popworld.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API Controller chuẩn hóa cho Đơn hàng (Order)
 */
@RestController
@RequestMapping("/api/orders")
public class OrderApiController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;
    private final UserService userService;
    private final BoxReservationRepository boxReservationRepository;

    public OrderApiController(OrderService orderService,
                              OrderMapper orderMapper,
                              UserService userService,
                              BoxReservationRepository boxReservationRepository) {
        this.orderService = orderService;
        this.orderMapper = orderMapper;
        this.userService = userService;
        this.boxReservationRepository = boxReservationRepository;
    }

    /**
     * Lấy chi tiết đơn hàng theo mã đơn (Bảo vệ IDOR: chỉ chủ sở hữu đơn hàng hoặc Admin)
     */
    @GetMapping("/{orderCode}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderDetail(@PathVariable String orderCode, Principal principal) {
        if (principal == null) {
            throw new BadRequestException("Vui lòng đăng nhập để xem thông tin đơn hàng.");
        }
        Order order = orderService.getOrderByCode(orderCode);
        if (order == null) {
            throw new ResourceNotFoundException("Không tìm thấy đơn hàng với mã: " + orderCode);
        }

        User user = userService.getUserByEmail(principal.getName());
        boolean isAdmin = user != null && ("ROLE_ADMIN".equals(user.getRole()) || "ADMIN".equals(user.getRole()));
        boolean isOwner = user != null && order.getUser() != null && order.getUser().getId().equals(user.getId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("Bạn không có quyền xem thông tin đơn hàng này.");
        }

        List<OrderItem> items = orderService.getOrderItems(order.getId());
        OrderResponse response = orderMapper.toResponse(order, items);

        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin đơn hàng thành công", response));
    }

    /**
     * Overload hỗ trợ các gọi nội bộ / unit test đơn giản không có context bảo mật
     */
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderDetail(String orderCode) {
        Order order = orderService.getOrderByCode(orderCode);
        if (order == null) {
            throw new ResourceNotFoundException("Không tìm thấy đơn hàng với mã: " + orderCode);
        }

        List<OrderItem> items = orderService.getOrderItems(order.getId());
        OrderResponse response = orderMapper.toResponse(order, items);

        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin đơn hàng thành công", response));
    }

    /**
     * API Polling trạng thái đơn hàng (phục vụ giao diện đếm ngược & thanh toán VietQR)
     * [BẢO MẬT]: Chỉ chủ sở hữu đơn hàng hoặc Admin mới được phép poll trạng thái,
     * tránh rò rỉ isPaid cho user khác qua orderCode enumeration.
     */
    @GetMapping("/{orderCode}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrderStatus(@PathVariable String orderCode,
                                                                           Principal principal) {
        Order order = orderService.getOrderByCode(orderCode);
        if (order == null) {
            throw new ResourceNotFoundException("Không tìm thấy đơn hàng với mã: " + orderCode);
        }

        if (principal == null) {
            throw new BadRequestException("Vui lòng đăng nhập để xem trạng thái đơn hàng.");
        }
        User user = userService.getUserByEmail(principal.getName());
        boolean isAdmin = user != null && ("ROLE_ADMIN".equals(user.getRole()) || "ADMIN".equals(user.getRole()));
        boolean isOwner = user != null && order.getUser() != null && order.getUser().getId().equals(user.getId());
        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("Bạn không có quyền xem thông tin đơn hàng này.");
        }

        boolean isPaid = !"TO_PAY".equalsIgnoreCase(order.getStatus()) && !"CANCELLED".equalsIgnoreCase(order.getStatus());

        Map<String, Object> statusData = new HashMap<>();
        statusData.put("orderCode", order.getOrderCode());
        statusData.put("status", order.getStatus());
        statusData.put("isPaid", isPaid);
        if (order.getExpiresAt() != null) {
            statusData.put("expiresAt", order.getExpiresAt().toString());
        }

        if ("POP_NOW_CABINET".equalsIgnoreCase(order.getDeliveryMethod())) {
            statusData.put("isPopNow", true);
        }

        if (boxReservationRepository != null) {
            boxReservationRepository.findByOrderCode(order.getOrderCode()).ifPresent(res -> {
                statusData.put("isPopNow", true);
                statusData.put("reservationCode", res.getReservationCode());
                statusData.put("reservationStatus", res.getStatus().name());
                if (res.getExpiresAt() != null) {
                    statusData.put("expiresAt", res.getExpiresAt().toString());
                }
            });
        }

        return ResponseEntity.ok(ApiResponse.success("Lấy trạng thái đơn hàng thành công", statusData));
    }


    /**
     * Lấy danh sách đơn hàng của người dùng hiện tại
     */
    @GetMapping("/my-orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(Principal principal) {
        if (principal == null) {
            throw new BadRequestException("Vui lòng đăng nhập để xem danh sách đơn hàng.");
        }
        User user = userService.getUserByEmail(principal.getName());
        List<Order> orders = orderService.getOrdersByUser(user.getId());

        List<OrderResponse> responseList = orders.stream()
                .map(order -> {
                    List<OrderItem> items = orderService.getOrderItems(order.getId());
                    return orderMapper.toResponse(order, items);
                })
                .toList();

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn hàng thành công", responseList));
    }

}
