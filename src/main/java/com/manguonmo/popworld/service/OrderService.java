package com.manguonmo.popworld.service;

import com.manguonmo.popworld.entity.Order;
import org.aspectj.weaver.ast.Or;

import java.util.List;

public interface OrderService {
    Order createOrder(Long userId, String recipientName, String recipientPhone,
                      String provinceCity, String district, String ward,
                      String detailedAddress, String paymentMethod, String couponCode);

    Order getOrderByCode(String orderCode);
    List<Order> getOrdersByUser(Long userId);
    Order cancelOrder(Long userId, String orderCode, String reason);
    // 1. Lấy toàn bộ đơn hàng (truyền null nếu muốn lấy hết, hoặc truyền "PROCESSING", "SHIPPED",... để lọc)
    List<Order> getAllOrders(String status);
    // 2. Chuyển trạng thái đơn sang ĐANG GIAO (PROCESSING -> SHIPPED)
    Order shipOrder(String orderCode);
    // 3. Chuyển trạng thái đơn sang HOÀN TẤT (SHIPPED -> COMPLETED)
    Order completeOrder(String orderCode);
}
