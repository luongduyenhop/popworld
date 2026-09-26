package com.manguonmo.popworld.repository;

import com.manguonmo.popworld.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    List<UserAddress> findByUserId(Long userId);
    List<UserAddress> findByUserIdOrderByIsDefaultDescIdDesc(Long userId);
    Optional<UserAddress> findByUserIdAndIsDefaultTrue(Long userId);
    Optional<UserAddress> findByIdAndUserId(Long id, Long userId);
    long countByUserId(Long userId);
}