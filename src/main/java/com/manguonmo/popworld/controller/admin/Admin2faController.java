package com.manguonmo.popworld.controller.admin;

import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.repository.UserRepository;
import com.manguonmo.popworld.security.filter.Admin2faFilter;
import com.manguonmo.popworld.security.totp.TotpService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * Controller xử lý quy trình Xác thực 2 yếu tố (2FA / TOTP) cho Quản trị viên:
 * 1. Màn hình nhập mã xác thực OTP khi đăng nhập vào phân hệ Admin
 * 2. Màn hình kích hoạt / thiết lập mã QR 2FA trong trang quản trị cá nhân
 */
@Slf4j
@Controller
@RequestMapping("/admin")
public class Admin2faController {

    private final TotpService totpService;
    private final UserRepository userRepository;

    public Admin2faController(TotpService totpService, UserRepository userRepository) {
        this.totpService = totpService;
        this.userRepository = userRepository;
    }

    /**
     * Màn hình nhập mã OTP 6 số để hoàn tất đăng nhập Admin
     */
    @GetMapping("/verify-2fa")
    public String showVerify2faPage(Authentication authentication, HttpSession session, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        Boolean isVerified = (Boolean) session.getAttribute(Admin2faFilter.SESSION_2FA_VERIFIED);
        if (Boolean.TRUE.equals(isVerified)) {
            return "redirect:/admin/dashboard";
        }

        model.addAttribute("adminEmail", authentication.getName());
        return "admin/verify-2fa";
    }

    /**
     * Xử lý xác thực mã OTP gửi lên
     */
    @PostMapping("/verify-2fa/submit")
    public String verify2faCode(@RequestParam("code") String code,
                                Authentication authentication,
                                HttpSession session,
                                Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty() || userOpt.get().getTotpSecret() == null) {
            model.addAttribute("error", "Tài khoản chưa thiết lập khóa bí mật 2FA!");
            model.addAttribute("adminEmail", email);
            return "admin/verify-2fa";
        }

        User user = userOpt.get();
        boolean isValid = totpService.verifyCode(user.getTotpSecret(), code);

        if (isValid) {
            log.info("Admin '{}' xác thực 2FA TOTP thành công. Cấp quyền truy cập /admin", email);
            session.setAttribute(Admin2faFilter.SESSION_2FA_VERIFIED, true);
            session.setAttribute(Admin2faFilter.SESSION_TOTP_ENABLED, true);
            return "redirect:/admin/dashboard";
        } else {
            log.warn("Admin '{}' nhập sai mã 2FA TOTP: {}", email, code);
            model.addAttribute("error", "Mã xác thực 6 số không chính xác hoặc đã hết hạn. Vui lòng kiểm tra lại ứng dụng Authenticator!");
            model.addAttribute("adminEmail", email);
            return "admin/verify-2fa";
        }
    }

    /**
     * Màn hình thiết lập bật 2FA cho tài khoản Admin
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/2fa/setup")
    public String showSetup2faPage(Authentication authentication, HttpSession session, Model model) {
        String email = authentication.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        User user = userOpt.get();
        String secret = user.getTotpSecret();
        if (secret == null || secret.isBlank()) {
            secret = totpService.generateSecretKey();
            session.setAttribute("PENDING_2FA_SECRET", secret);
        } else {
            session.setAttribute("PENDING_2FA_SECRET", secret);
        }

        String qrUri = totpService.generateTotpUri(secret, email, "PopWorld");
        model.addAttribute("user", user);
        model.addAttribute("secretKey", secret);
        model.addAttribute("qrUri", qrUri);
        model.addAttribute("totpEnabled", Boolean.TRUE.equals(user.getTotpEnabled()));

        return "admin/setup-2fa";
    }

    /**
     * Kích hoạt 2FA sau khi Admin nhập mã kiểm tra thành công
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/2fa/enable")
    public String enable2fa(@RequestParam("code") String code,
                            Authentication authentication,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        String email = authentication.getName();
        String pendingSecret = (String) session.getAttribute("PENDING_2FA_SECRET");

        if (pendingSecret == null || pendingSecret.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Phiên thiết lập đã hết hạn, vui lòng thử lại.");
            return "redirect:/admin/2fa/setup";
        }

        boolean isValid = totpService.verifyCode(pendingSecret, code);
        if (!isValid) {
            redirectAttributes.addFlashAttribute("error", "Mã xác thực không chính xác! Không thể kích hoạt 2FA.");
            return "redirect:/admin/2fa/setup";
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setTotpSecret(pendingSecret);
            user.setTotpEnabled(true);
            userRepository.save(user);

            session.setAttribute(Admin2faFilter.SESSION_2FA_VERIFIED, true);
            session.setAttribute(Admin2faFilter.SESSION_TOTP_ENABLED, true);
            session.removeAttribute("PENDING_2FA_SECRET");

            log.info("Admin '{}' đã kích hoạt thành công xác thực 2 yếu tố (2FA).", email);
            redirectAttributes.addFlashAttribute("success", "Kích hoạt xác thực 2 lớp (2FA) thành công! Tài khoản của bạn đã được bảo vệ tối đa.");
        }

        return "redirect:/admin/2fa/setup";
    }

    /**
     * Hủy kích hoạt 2FA
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/2fa/disable")
    public String disable2fa(@RequestParam("code") String code,
                             Authentication authentication,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        String email = authentication.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (Boolean.TRUE.equals(user.getTotpEnabled())) {
                boolean isValid = totpService.verifyCode(user.getTotpSecret(), code);
                if (!isValid) {
                    redirectAttributes.addFlashAttribute("error", "Mã xác thực không đúng. Không thể hủy 2FA!");
                    return "redirect:/admin/2fa/setup";
                }

                user.setTotpEnabled(false);
                user.setTotpSecret(null);
                userRepository.save(user);

                session.removeAttribute(Admin2faFilter.SESSION_2FA_VERIFIED);
                session.setAttribute(Admin2faFilter.SESSION_TOTP_ENABLED, false);

                log.info("Admin '{}' đã tắt xác thực 2 yếu tố.", email);
                redirectAttributes.addFlashAttribute("success", "Đã tắt xác thực 2 lớp thành công.");
            }
        }

        return "redirect:/admin/2fa/setup";
    }
}
