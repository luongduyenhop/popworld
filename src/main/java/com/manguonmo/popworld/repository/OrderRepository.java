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

    // Lấy danh sách đơn hàng mới nhất cho Dashboard
    List<Order> findTop8ByOrderByCreatedAtDesc();

    // Tìm kiếm đơn hàng theo mã, tên người nhận hoặc số điện thoại
    @org.springframework.data.jpa.repository.Query("SELECT o FROM Order o WHERE " +
            "LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(o.recipientName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "o.recipientPhone LIKE CONCAT('%', :keyword, '%') " +
            "ORDER BY o.createdAt DESC")
    List<Order> searchOrders(@org.springframework.data.repository.query.Param("keyword") String keyword);

    // Tính tổng doanh thu từ các đơn hàng thành công
    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status IN ('DELIVERED', 'SHIPPING', 'PROCESSING', 'COMPLETED', 'SHIPPED')")
    java.math.BigDecimal calculateTotalRevenue();
}