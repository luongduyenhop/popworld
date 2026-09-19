package com.manguonmo.popworld.service;

import com.manguonmo.popworld.entity.Order;

import java.util.List;

public interface OrderService {
    Order createOrder(Long userId, String recipientName, String recipientPhone,
                      String provinceCity, String district, String ward,
                      String detailedAddress, String paymentMethod, String couponCode);

    Order getOrderByCode(String orderCode);
    List<Order> getOrdersByUser(Long userId);
    Order cancelOrder(Long userId, String orderCode, String reason);
}
