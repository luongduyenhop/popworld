package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.response.OrderResponse;
import com.manguonmo.popworld.dto.response.OrderStatusCountResponse;
import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.OrderItem;

import java.util.List;

public interface OrderService {
    Order createOrder(Long userId, String recipientName, String recipientPhone,
                      String provinceCity, String district, String ward,
                      String detailedAddress, String paymentMethod, String couponCode);

    Order getOrderByCode(String orderCode);
    List<Order> getOrdersByUser(Long userId);
    Order cancelOrder(Long userId, String orderCode, String reason);

    // 1. Lấy toàn bộ đơn hàng dạng DTO OrderResponse (kèm items), có hỗ trợ lọc theo status
    List<OrderResponse> getAllOrders(String status);

    // 2. Chuyển trạng thái đơn sang ĐANG GIAO (PROCESSING -> SHIPPED)
    Order shipOrder(String orderCode);

    // 3. Chuyển trạng thái đơn sang HOÀN TẤT (SHIPPED -> COMPLETED)
    Order completeOrder(String orderCode);

    // 4. Lấy danh sách sản phẩm theo mã định danh đơn hàng
    List<OrderItem> getOrderItems(Long orderId);

    // 5. Thống kê số lượng đơn hàng theo từng trạng thái bằng DTO
    OrderStatusCountResponse getOrderStatusCounts();

    // 6. Tìm kiếm đơn hàng theo từ khóa (mã, tên người nhận, SĐT) dạng DTO
    List<OrderResponse> searchOrders(String keyword);
}
