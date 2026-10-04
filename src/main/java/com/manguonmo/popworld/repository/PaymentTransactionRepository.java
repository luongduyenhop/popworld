package com.manguonmo.popworld.repository;

import com.manguonmo.popworld.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    boolean existsByTransactionCode(String transactionCode);

    Optional<PaymentTransaction> findByTransactionCode(String transactionCode);

    List<PaymentTransaction> findByOrderIdOrderByCreatedAtDesc(Long orderId);
}
