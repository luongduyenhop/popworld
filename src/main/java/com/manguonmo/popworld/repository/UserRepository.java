package com.manguonmo.popworld.repository;

import com.manguonmo.popworld.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findById(Long id);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " (u.phone IS NOT NULL AND u.phone LIKE CONCAT('%', :keyword, '%')) OR " +
           " (u.memberCode IS NOT NULL AND LOWER(u.memberCode) LIKE LOWER(CONCAT('%', :keyword, '%')))) AND " +
           "(:tier IS NULL OR :tier = '' OR :tier = 'ALL' OR UPPER(u.membershipTier) = UPPER(:tier)) AND " +
           "(:enabled IS NULL OR u.enabled = :enabled) " +
           "ORDER BY u.id DESC")
    List<User> searchCustomers(@Param("keyword") String keyword,
                              @Param("tier") String tier,
                              @Param("enabled") Boolean enabled);
}