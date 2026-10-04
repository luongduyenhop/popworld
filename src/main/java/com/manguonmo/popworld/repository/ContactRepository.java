package com.manguonmo.popworld.repository;

import com.manguonmo.popworld.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {
    List<Contact> findAllByOrderByCreatedAtDesc();

    List<Contact> findByIsProcessedOrderByCreatedAtDesc(Boolean isProcessed);

    long countByIsProcessed(Boolean isProcessed);

    @Query("SELECT c FROM Contact c WHERE " +
           "LOWER(c.subject) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.senderName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "c.phone LIKE CONCAT('%', :keyword, '%') OR " +
           "LOWER(c.message) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY c.createdAt DESC")
    List<Contact> searchContacts(@Param("keyword") String keyword);
}