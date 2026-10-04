package com.manguonmo.popworld.repository;

import com.manguonmo.popworld.entity.OrderItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @EntityGraph(attributePaths = {"product", "product.images"})
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId")
    List<OrderItem> findByOrderId(@Param("orderId") Long orderId);

    boolean existsByProductId(Long productId);

    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi " +
            "WHERE oi.order.user.id = :userId " +
            "  AND oi.product.id = :productId " +
            "  AND oi.order.status IN ('DELIVERED', 'COMPLETED')")
    boolean hasUserPurchasedProductDelivered(@Param("userId") Long userId,
                                            @Param("productId") Long productId);

    @EntityGraph(attributePaths = {"product", "product.images"})
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id IN :orderIds")
    List<OrderItem> findByOrderIdIn(@Param("orderIds") List<Long> orderIds);
}