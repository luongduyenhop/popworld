package com.manguonmo.popworld.repository;

import com.manguonmo.popworld.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Order> findByOrderCode(String orderCode);
    List<Order> findByStatusOrderByCreatedAtDesc(String status);
    long countByStatus(String status);
    
    // Tìm các đơn hàng quá hạn 15 phút chưa thanh toán để tự động giải phóng tồn kho
    List<Order> findByExpiresAtBeforeAndStatus(LocalDateTime now, String status);
}