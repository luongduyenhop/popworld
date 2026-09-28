package com.manguonmo.popworld.repository;

import com.manguonmo.popworld.entity.OrderTimeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderTimelineRepository extends JpaRepository<OrderTimeline, Long> {
    List<OrderTimeline> findByOrderIdOrderByCreatedAtAsc(Long orderId);
    List<OrderTimeline> findByOrderOrderCodeOrderByCreatedAtAsc(String orderCode);
}
