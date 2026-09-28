package com.manguonmo.popworld.repository;

import com.manguonmo.popworld.entity.OwnedItem;
import com.manguonmo.popworld.entity.OwnedItemStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OwnedItemRepository extends JpaRepository<OwnedItem, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OwnedItem o WHERE o.id = :id")
    Optional<OwnedItem> findByIdForUpdate(@Param("id") Long id);

    List<OwnedItem> findByUserIdOrderByUnboxedAtDesc(Long userId);

    List<OwnedItem> findByUserIdAndStatusOrderByUnboxedAtDesc(Long userId, OwnedItemStatus status);

    boolean existsByReservationId(Long reservationId);

    Optional<OwnedItem> findByReservationId(Long reservationId);

    long countByUserId(Long userId);

    boolean existsByBlindBoxItemId(Long blindBoxItemId);

    long countByBlindBoxItemId(Long blindBoxItemId);
}
