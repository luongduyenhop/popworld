package com.manguonmo.popworld.repository;

import com.manguonmo.popworld.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCodeAndActiveTrue(String code);
    boolean existsByCode(String code);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Coupon c SET c.usedCount = c.usedCount + 1 WHERE c.id = :couponId AND (c.usageLimit IS NULL OR c.usedCount < c.usageLimit)")
    int increaseUsedCount( @Param("couponId") Long couponId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Coupon c SET c.usedCount = c.usedCount - 1 WHERE c.id = :couponId AND c.usedCount > 0")
    int decreaseUsedCount(@Param("couponId") Long couponId);




}