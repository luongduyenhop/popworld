package com.manguonmo.popworld.repository;

import com.manguonmo.popworld.entity.OwnedItem;
import com.manguonmo.popworld.entity.OwnedItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OwnedItemRepository extends JpaRepository<OwnedItem, Long> {

    List<OwnedItem> findByUserIdOrderByUnboxedAtDesc(Long userId);

    List<OwnedItem> findByUserIdAndStatusOrderByUnboxedAtDesc(Long userId, OwnedItemStatus status);

    boolean existsByReservationId(Long reservationId);

    Optional<OwnedItem> findByReservationId(Long reservationId);

    long countByUserId(Long userId);
}
