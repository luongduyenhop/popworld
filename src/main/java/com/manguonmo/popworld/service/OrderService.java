package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.response.OrderResponse;
import com.manguonmo.popworld.dto.response.OrderStatusCountResponse;
import com.manguonmo.popworld.dto.response.OrderTimelineResponse;
import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.OrderItem;

import java.util.List;
import java.util.Map;

public interface OrderService {
    Map<Long, List<OrderItem>> getOrderItemsByOrderIds(List<Long> orderIds);
    default Order createOrder(Long userId, String recipientName, String recipientPhone,
                      String provinceCity, String district, String ward,
                      String detailedAddress, String paymentMethod, String couponCode) {
        return createOrder(userId, recipientName, recipientPhone, provinceCity, district, ward, detailedAddress, paymentMethod, couponCode, 0);
    }

    Order createOrder(Long userId, String recipientName, String recipientPhone,
                      String provinceCity, String district, String ward,
                      String detailedAddress, String paymentMethod, String couponCode,
                      Integer pointsToUse);

    Order createOrderForReservation(Long userId, String reservationCode, String paymentMethod);


    Order getOrderByCode(String orderCode);
    List<Order> getOrdersByUser(Long userId);
    Order cancelOrder(Long userId, String orderCode, String reason);
    Order adminCancelOrder(String orderCode, String reason);
    Order adminCancelOrder(String orderCode, String reason, String adminUsername);

    // 1. Lấy toàn bộ đơn hàng dạng DTO OrderResponse (kèm items), có hỗ trợ lọc theo status
    List<OrderResponse> getAllOrders(String status);

    // 2. Chuyển trạng thái đơn sang ĐÃ ĐÓNG GÓI (PROCESSING -> PACKED)
    Order packOrder(String orderCode, String adminUsername, String note);
    default Order packOrder(String orderCode) {
        return packOrder(orderCode, "Quản trị viên", null);
    }

    // 3. Chuyển trạng thái đơn sang ĐANG GIAO (PACKED / PROCESSING -> SHIPPING) kèm thông tin vận đơn
    Order shipOrder(String orderCode);
    Order shipOrder(String orderCode, String carrier, String trackingNumber, String adminUsername, String note);

    // 4. Chuyển trạng thái đơn sang ĐÃ GIAO HÀNG / HOÀN TẤT (SHIPPING -> DELIVERED)
    Order completeOrder(String orderCode);
    Order completeOrder(String orderCode, String adminUsername, String note);

    // 5. Lấy danh sách sản phẩm theo mã định danh đơn hàng
    List<OrderItem> getOrderItems(Long orderId);

    // 6. Thống kê số lượng đơn hàng theo từng trạng thái bằng DTO
    OrderStatusCountResponse getOrderStatusCounts();

    // 7. Tìm kiếm đơn hàng theo từ khóa (mã, tên người nhận, SĐT) dạng DTO
    List<OrderResponse> searchOrders(String keyword);

    // 8. Lấy toàn bộ lịch sử hành trình đơn hàng (Timeline)
    List<OrderTimelineResponse> getOrderTimelines(String orderCode);

    // 9. Ghi nhận sự kiện hành trình đơn hàng (Internal Timeline Logger)
    void recordTimeline(Order order, String fromStatus, String toStatus, String action, String actor, String note);
}
