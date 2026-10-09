package com.manguonmo.popworld.repository;

import com.manguonmo.popworld.entity.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {

    List<InventoryLog> findTop50ByProductIdOrderByCreatedAtDesc(Long productId);

    List<InventoryLog> findAllByOrderByCreatedAtDesc();
}
