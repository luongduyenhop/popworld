package com.manguonmo.popworld.controller.admin;

import com.manguonmo.popworld.entity.Contact;
import com.manguonmo.popworld.service.ContactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/admin/support")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminSupportWebController {

    private final ContactService contactService;

    @GetMapping
    public String listSupportTickets(
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false) String keyword,
            Model model) {

        List<Contact> contacts = contactService.getContacts(status, keyword);
        long pendingCount = contactService.countPendingContacts();
        List<Contact> allContacts = contactService.getContacts("ALL", null);
        long totalCount = allContacts.size();
        long resolvedCount = Math.max(0, totalCount - pendingCount);

        model.addAttribute("contacts", contacts);
        model.addAttribute("currentStatus", status.toUpperCase());
        model.addAttribute("keyword", keyword);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("resolvedCount", resolvedCount);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("activeItem", "support");

        return "admin/support";
    }

    @PostMapping("/{id}/process")
    public String updateTicketStatus(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean isProcessed,
            @RequestParam(required = false) String adminNote,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            String adminUsername = principal != null ? principal.getName() : "admin";
            contactService.updateProcessStatus(id, isProcessed, adminNote, adminUsername);
            redirectAttributes.addFlashAttribute("successMessage", 
                    isProcessed ? "Đã đánh dấu giải quyết thành công yêu cầu #" + id : "Đã chuyển yêu cầu #" + id + " về trạng thái Chờ xử lý");
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật trạng thái yêu cầu #{}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật yêu cầu: " + e.getMessage());
        }

        return "redirect:/admin/support";
    }
}
