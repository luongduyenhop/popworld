package com.manguonmo.popworld.repository;

import com.manguonmo.popworld.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {
    List<Contact> findByIsProcessedOrderByCreatedAtDesc(Boolean isProcessed);
}