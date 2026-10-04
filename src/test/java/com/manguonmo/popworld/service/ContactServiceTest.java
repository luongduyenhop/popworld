package com.manguonmo.popworld.service;

import com.manguonmo.popworld.entity.Contact;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.repository.ContactRepository;
import com.manguonmo.popworld.service.impl.ContactServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private ContactRepository contactRepository;

    @InjectMocks
    private ContactServiceImpl contactService;

    @Test
    @DisplayName("Gửi yêu cầu hỗ trợ thành công khi dữ liệu hợp lệ")
    void submitContact_validInput_createsContact() {
        Contact savedContact = Contact.builder()
                .id(1L)
                .senderName("Molly Collector")
                .email("molly@example.com")
                .phone("0912345678")
                .subject("Hỏi về đơn hàng")
                .category("DON_HANG")
                .message("Cần kiểm tra trạng thái đơn giao")
                .userId(10L)
                .isProcessed(false)
                .build();

        when(contactRepository.save(any(Contact.class))).thenReturn(savedContact);

        Contact result = contactService.submitContact("Molly Collector", "molly@example.com", "0912345678",
                "Hỏi về đơn hàng", "DON_HANG", "Cần kiểm tra trạng thái đơn giao", 10L);

        assertNotNull(result);
        assertEquals("Molly Collector", result.getSenderName());
        assertEquals("molly@example.com", result.getEmail());
        assertFalse(result.getIsProcessed());
        verify(contactRepository, times(1)).save(any(Contact.class));
    }

    @Test
    @DisplayName("Ném lỗi khi gửi yêu cầu thiếu tên, email hoặc nội dung")
    void submitContact_missingFields_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                contactService.submitContact("", "email@example.com", "123", "sub", "cat", "msg", null));

        assertThrows(IllegalArgumentException.class, () ->
                contactService.submitContact("Name", "", "123", "sub", "cat", "msg", null));

        assertThrows(IllegalArgumentException.class, () ->
                contactService.submitContact("Name", "email@example.com", "123", "sub", "cat", "   ", null));
    }

    @Test
    @DisplayName("Lọc danh sách yêu cầu theo trạng thái PENDING")
    void getContacts_withPendingStatus_filtersPending() {
        Contact c1 = Contact.builder().id(1L).isProcessed(false).build();
        when(contactRepository.findByIsProcessedOrderByCreatedAtDesc(false)).thenReturn(List.of(c1));

        List<Contact> results = contactService.getContacts("PENDING", null);

        assertEquals(1, results.size());
        assertFalse(results.get(0).getIsProcessed());
        verify(contactRepository).findByIsProcessedOrderByCreatedAtDesc(false);
    }

    @Test
    @DisplayName("Admin cập nhật trạng thái giải quyết thành công kèm ghi chú")
    void updateProcessStatus_updatesStatusAndNote() {
        Contact contact = Contact.builder().id(1L).isProcessed(false).build();
        when(contactRepository.findById(1L)).thenReturn(Optional.of(contact));
        when(contactRepository.save(any(Contact.class))).thenAnswer(inv -> inv.getArgument(0));

        Contact updated = contactService.updateProcessStatus(1L, true, "Đã gọi điện tư vấn", "admin_linh");

        assertTrue(updated.getIsProcessed());
        assertEquals("Đã gọi điện tư vấn", updated.getAdminNote());
        assertEquals("admin_linh", updated.getProcessedBy());
        assertNotNull(updated.getProcessedAt());
        verify(contactRepository).save(contact);
    }

    @Test
    @DisplayName("Cập nhật yêu cầu không tồn tại ném ResourceNotFoundException")
    void updateProcessStatus_notFound_throwsException() {
        when(contactRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                contactService.updateProcessStatus(999L, true, "note", "admin"));
    }

    @Test
    @DisplayName("Đếm số lượng yêu cầu hỗ trợ đang chờ xử lý")
    void countPendingContacts_returnsCorrectCount() {
        when(contactRepository.countByIsProcessed(false)).thenReturn(5L);

        long count = contactService.countPendingContacts();

        assertEquals(5L, count);
        verify(contactRepository).countByIsProcessed(false);
    }
}
