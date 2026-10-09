package com.manguonmo.popworld.security.filter;

import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Filter kiểm soát bảo mật hai yếu tố (2FA) cho phân hệ Quản trị viên (/admin/**).
 * 
 * Nếu Admin đã đăng nhập bằng mật khẩu nhưng tài khoản có kích hoạt 2FA (totp_enabled = true)
 * và phiên làm việc chưa xác thực OTP (ADMIN_2FA_VERIFIED != true), Filter sẽ chặn và
 * chuyển hướng tới trang nhập mã xác thực OTP /admin/verify-2fa.
 */
@Slf4j
public class Admin2faFilter extends OncePerRequestFilter {

    public static final String SESSION_2FA_VERIFIED = "ADMIN_2FA_VERIFIED";
    public static final String SESSION_TOTP_ENABLED = "USER_TOTP_ENABLED";

    private final UserRepository userRepository;

    public Admin2faFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        String relativePath = (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath))
                ? path.substring(contextPath.length())
                : path;

        // Chỉ kiểm tra các yêu cầu truy cập vào phân hệ /admin/**
        if (relativePath.startsWith("/admin")) {
            // Danh sách các URL ngoại lệ không bị chuyển hướng vòng lặp
            if (isExcludedPath(relativePath)) {
                filterChain.doFilter(request, response);
                return;
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
                boolean isAdmin = auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

                if (isAdmin) {
                    HttpSession session = request.getSession(true);
                    Boolean isVerified = (Boolean) session.getAttribute(SESSION_2FA_VERIFIED);

                    if (Boolean.TRUE.equals(isVerified)) {
                        // Đã xác thực 2FA thành công trong phiên hiện tại -> Cho phép truy cập
                        filterChain.doFilter(request, response);
                        return;
                    }

                    // Kiểm tra cờ totp_enabled của user
                    Boolean totpEnabled = (Boolean) session.getAttribute(SESSION_TOTP_ENABLED);
                    if (totpEnabled == null && userRepository != null) {
                        Optional<User> userOpt = userRepository.findByEmail(auth.getName());
                        if (userOpt.isPresent()) {
                            totpEnabled = Boolean.TRUE.equals(userOpt.get().getTotpEnabled());
                            session.setAttribute(SESSION_TOTP_ENABLED, totpEnabled);
                        } else {
                            totpEnabled = false;
                        }
                    }

                    if (Boolean.TRUE.equals(totpEnabled)) {
                        log.info("Admin '{}' truy cập {} khi chưa hoàn tất 2FA. Chuyển hướng tới /admin/verify-2fa",
                                auth.getName(), relativePath);
                        response.sendRedirect(contextPath + "/admin/verify-2fa");
                        return;
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isExcludedPath(String path) {
        return path.equals("/admin/verify-2fa")
                || path.equals("/admin/verify-2fa/submit")
                || path.startsWith("/admin/verify-2fa/")
                || path.endsWith(".css")
                || path.endsWith(".js")
                || path.endsWith(".png")
                || path.endsWith(".jpg")
                || path.endsWith(".jpeg")
                || path.endsWith(".svg")
                || path.endsWith(".ico")
                || path.endsWith(".woff")
                || path.endsWith(".woff2");
    }
}
