package com.manguonmo.popworld.repository;

import com.manguonmo.popworld.entity.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {
    List<UserCoupon> findByUserId(Long userId);
    Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT uc FROM UserCoupon uc WHERE uc.user.id = :userId AND uc.coupon.id = :couponId")
    Optional<UserCoupon> findByUserIdAndCouponIdForUpdate(@Param("userId") Long userId, @Param("couponId") Long couponId);

    boolean existsByUserIdAndCouponIdAndIsUsedTrue(Long userId, Long couponId);
}