package com.manguonmo.popworld.repository;

import com.manguonmo.popworld.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findByOrderIdOrderByProcessedAtDesc(Long orderId);

    List<Refund> findByOrderOrderCodeOrderByProcessedAtDesc(String orderCode);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.order.id = :orderId AND r.status = 'COMPLETED'")
    BigDecimal sumCompletedRefundAmountByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.order.orderCode = :orderCode AND r.status = 'COMPLETED'")
    BigDecimal sumCompletedRefundAmountByOrderCode(@Param("orderCode") String orderCode);
}
