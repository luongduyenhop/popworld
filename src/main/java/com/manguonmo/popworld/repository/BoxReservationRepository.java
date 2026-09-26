package com.manguonmo.popworld.repository;

import com.manguonmo.popworld.entity.BoxReservation;
import com.manguonmo.popworld.entity.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BoxReservationRepository extends JpaRepository<BoxReservation, Long> {

    Optional<BoxReservation> findByReservationCode(String reservationCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM BoxReservation r WHERE r.reservationCode = :reservationCode")
    Optional<BoxReservation> findByReservationCodeForUpdate(@Param("reservationCode") String reservationCode);

    Optional<BoxReservation> findByReservationCodeAndUserId(String reservationCode, Long userId);

    List<BoxReservation> findByProductIdAndStatus(Long productId, ReservationStatus status);

    List<BoxReservation> findByUserIdAndStatus(Long userId, ReservationStatus status);

    List<BoxReservation> findByStatusAndExpiresAtBefore(ReservationStatus status, LocalDateTime now);

    boolean existsByProductIdAndBoxIndexAndStatusIn(Long productId, Integer boxIndex, Collection<ReservationStatus> statuses);

    long countByProductIdAndStatusIn(Long productId, Collection<ReservationStatus> statuses);
}
