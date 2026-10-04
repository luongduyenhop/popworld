package com.manguonmo.popworld.controller.client;

import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.service.ContactService;
import com.manguonmo.popworld.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ContactWebController {

    private final ContactService contactService;
    private final UserService userService;

    @PostMapping("/support/submit")
    public String handleSupportSubmit(
            @RequestParam(required = false) String senderName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String message,
            @RequestParam(required = false, defaultValue = "/") String redirectUrl,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            Long userId = null;
            if (principal != null) {
                User user = userService.getUserByEmail(principal.getName());
                if (user != null) {
                    userId = user.getId();
                    if (senderName == null || senderName.isBlank()) {
                        senderName = user.getFullName() != null ? user.getFullName() : user.getEmail();
                    }
                    if (email == null || email.isBlank()) {
                        email = user.getEmail();
                    }
                    if ((phone == null || phone.isBlank()) && user.getPhone() != null) {
                        phone = user.getPhone();
                    }
                }
            }

            if (senderName == null || senderName.isBlank()) {
                redirectAttributes.addFlashAttribute("supportError", "Vui lòng nhập họ và tên của bạn.");
                return "redirect:" + (redirectUrl.startsWith("/") ? redirectUrl : "/");
            }
            if (email == null || email.isBlank()) {
                redirectAttributes.addFlashAttribute("supportError", "Vui lòng cung cấp email liên hệ hợp lệ.");
                return "redirect:" + (redirectUrl.startsWith("/") ? redirectUrl : "/");
            }
            if (message == null || message.isBlank()) {
                redirectAttributes.addFlashAttribute("supportError", "Vui lòng nhập nội dung cần hỗ trợ.");
                return "redirect:" + (redirectUrl.startsWith("/") ? redirectUrl : "/");
            }

            contactService.submitContact(senderName, email, phone, subject, category, message, userId);
            redirectAttributes.addFlashAttribute("supportSuccess", "Cảm ơn bạn! Yêu cầu hỗ trợ đã được tiếp nhận. Đội ngũ POP MART sẽ phản hồi sớm nhất qua email hoặc điện thoại.");
        } catch (Exception e) {
            log.error("Lỗi khi gửi yêu cầu hỗ trợ: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("supportError", "Có lỗi xảy ra khi gửi yêu cầu: " + e.getMessage());
        }

        return "redirect:" + (redirectUrl.startsWith("/") ? redirectUrl : "/");
    }
}
