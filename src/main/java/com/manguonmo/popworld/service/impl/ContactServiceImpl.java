package com.manguonmo.popworld.service.impl;

import com.manguonmo.popworld.entity.Contact;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.repository.ContactRepository;
import com.manguonmo.popworld.service.ContactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;

    @Override
    @Transactional
    public Contact submitContact(String senderName, String email, String phone, String subject, String category, String message, Long userId) {
        if (senderName == null || senderName.trim().isEmpty()) {
            throw new IllegalArgumentException("Họ và tên không được để trống");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email liên hệ không được để trống");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung yêu cầu không được để trống");
        }

        Contact contact = Contact.builder()
                .senderName(senderName.trim())
                .email(email.trim().toLowerCase())
                .phone(phone != null ? phone.trim() : null)
                .subject(subject != null && !subject.trim().isEmpty() ? subject.trim() : "Yêu cầu hỗ trợ khách hàng")
                .category(category != null && !category.trim().isEmpty() ? category.trim() : "CHUNG")
                .message(message.trim())
                .userId(userId)
                .isProcessed(false)
                .build();

        Contact saved = contactRepository.save(contact);
        log.info("Khách hàng {} ({}) đã gửi yêu cầu hỗ trợ mới ID={}", saved.getSenderName(), saved.getEmail(), saved.getId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Contact> getContacts(String status, String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return contactRepository.searchContacts(keyword.trim());
        }

        if ("PENDING".equalsIgnoreCase(status)) {
            return contactRepository.findByIsProcessedOrderByCreatedAtDesc(false);
        } else if ("RESOLVED".equalsIgnoreCase(status) || "PROCESSED".equalsIgnoreCase(status)) {
            return contactRepository.findByIsProcessedOrderByCreatedAtDesc(true);
        }

        return contactRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public Contact getContactById(Long id) {
        return contactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu hỗ trợ ID=" + id));
    }

    @Override
    @Transactional
    public Contact updateProcessStatus(Long id, boolean isProcessed, String adminNote, String adminUsername) {
        Contact contact = getContactById(id);
        contact.setIsProcessed(isProcessed);
        contact.setAdminNote(adminNote);
        contact.setProcessedBy(adminUsername != null ? adminUsername : "admin");
        contact.setProcessedAt(LocalDateTime.now());
        
        Contact updated = contactRepository.save(contact);
        log.info("Admin {} đã cập nhật trạng thái yêu cầu hỗ trợ ID={} sang isProcessed={}", 
                contact.getProcessedBy(), updated.getId(), isProcessed);
        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingContacts() {
        return contactRepository.countByIsProcessed(false);
    }
}
