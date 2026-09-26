package com.manguonmo.popworld.repository;

import com.manguonmo.popworld.entity.BlindBoxSlot;
import com.manguonmo.popworld.entity.SlotStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlindBoxSlotRepository extends JpaRepository<BlindBoxSlot, Long> {

    Optional<BlindBoxSlot> findByProductIdAndSlotIndex(Long productId, Integer slotIndex);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM BlindBoxSlot s WHERE s.product.id = :productId AND s.slotIndex = :slotIndex")
    Optional<BlindBoxSlot> findByProductIdAndSlotIndexForUpdate(@Param("productId") Long productId, @Param("slotIndex") Integer slotIndex);

    Optional<BlindBoxSlot> findByCurrentReservationId(Long reservationId);

    List<BlindBoxSlot> findByProductIdOrderBySlotIndexAsc(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM BlindBoxSlot s WHERE s.product.id = :productId AND s.status = :status ORDER BY s.slotIndex ASC")
    List<BlindBoxSlot> findAvailableSlotsForUpdate(@Param("productId") Long productId, @Param("status") SlotStatus status);
}
