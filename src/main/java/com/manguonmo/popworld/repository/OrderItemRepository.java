package com.manguonmo.popworld.repository;

import com.manguonmo.popworld.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @org.springframework.data.jpa.repository.Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.product WHERE oi.order.id = :orderId")
    List<OrderItem> findByOrderId(@org.springframework.data.repository.query.Param("orderId") Long orderId);

    boolean existsByProductId(Long productId);
}