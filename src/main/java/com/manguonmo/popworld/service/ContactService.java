package com.manguonmo.popworld.service;

import com.manguonmo.popworld.entity.Contact;

import java.util.List;

public interface ContactService {

    Contact submitContact(String senderName, String email, String phone, String subject, String category, String message, Long userId);

    List<Contact> getContacts(String status, String keyword);

    Contact getContactById(Long id);

    Contact updateProcessStatus(Long id, boolean isProcessed, String adminNote, String adminUsername);

    long countPendingContacts();
}
