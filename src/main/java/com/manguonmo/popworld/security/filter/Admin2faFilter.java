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
 * Filter kiểm soát bảo mật hai yếu tố (2FA) cho mọi route nhạy cảm của Quản trị viên.
 *
 * Phiên bản nâng cấp: Không chỉ bảo vệ /admin/** mà bảo vệ TẤT CẢ route cần xác thực
 * (bao gồm /account, /my-orders, /cart, /checkout, /api/orders, v.v.) để tránh Admin
 * bypass 2FA bằng cách truy cập phần user-facing của ứng dụng.
 *
 * Logic hoạt động:
 * 1. Static resource và public path (trang catalog, trang chủ) → bỏ qua.
 * 2. Trang /admin/verify-2fa và /logout → bỏ qua (tránh vòng lặp redirect).
 * 3. Người dùng chưa đăng nhập hoặc không phải Admin → bỏ qua.
 * 4. Admin đã xác thực OTP trong session → bỏ qua.
 * 5. Admin có totp_enabled = true nhưng chưa xác thực OTP → chặn, redirect verify-2fa.
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

        // Static resource, public path, trang OTP và logout → bỏ qua hoàn toàn
        if (isExcludedPath(relativePath)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Kiểm tra xem người dùng hiện tại có phải Admin đã xác thực Spring Security không
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            // Người dùng thường (ROLE_USER) không bao giờ bị chặn bởi filter này
            filterChain.doFilter(request, response);
            return;
        }

        // Đây là Admin đã đăng nhập → kiểm tra trạng thái 2FA
        HttpSession session = request.getSession(true);
        Boolean isVerified = (Boolean) session.getAttribute(SESSION_2FA_VERIFIED);
        if (Boolean.TRUE.equals(isVerified)) {
            // Đã xác thực 2FA thành công trong phiên hiện tại -> Cho phép truy cập
            filterChain.doFilter(request, response);
            return;
        }

        // Tải cờ totpEnabled từ DB (cache vào session để tránh truy vấn lặp)
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
            log.info("Admin '{}' truy cập '{}' khi chưa hoàn tất 2FA TOTP. Chuyển hướng tới /admin/verify-2fa",
                    auth.getName(), relativePath);
            response.sendRedirect(contextPath + "/admin/verify-2fa");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Kiểm tra xem đường dẫn có được miễn kiểm tra 2FA không.
     * Bao gồm: trang verify OTP, /logout, static resource, và public path.
     */
    private boolean isExcludedPath(String path) {
        return isVerify2faPath(path) || path.equals("/logout")
                || isStaticResource(path) || isPublicPath(path);
    }

    /**
     * Kiểm tra xem đường dẫn có phải trang xác thực OTP 2FA không (tránh vòng lặp redirect).
     */
    private boolean isVerify2faPath(String path) {
        return path.equals("/admin/verify-2fa")
                || path.equals("/admin/verify-2fa/submit")
                || path.startsWith("/admin/verify-2fa/");
    }

    /**
     * Kiểm tra xem đường dẫn có phải static resource không (CSS, JS, ảnh, font...).
     */
    private boolean isStaticResource(String path) {
        return path.endsWith(".css") || path.endsWith(".js")
                || path.endsWith(".png") || path.endsWith(".jpg")
                || path.endsWith(".jpeg") || path.endsWith(".gif")
                || path.endsWith(".svg") || path.endsWith(".ico")
                || path.endsWith(".woff") || path.endsWith(".woff2")
                || path.endsWith(".ttf") || path.endsWith(".eot")
                || path.startsWith("/images/") || path.startsWith("/css/")
                || path.startsWith("/js/") || path.startsWith("/fonts/")
                || path.startsWith("/webjars/");
    }

    /**
     * Kiểm tra xem đường dẫn có phải public path không cần 2FA không.
     * Đây là các trang catalog/thông tin công khai mà Admin có thể xem mà không cần xác thực OTP.
     */
    private boolean isPublicPath(String path) {
        if (path.equals("/") || path.equals("/login") || path.equals("/register")
                || path.startsWith("/register/")
                || path.startsWith("/products") || path.startsWith("/product/")
                || path.startsWith("/search") || path.startsWith("/category")
                || path.startsWith("/character-ip")
                || path.startsWith("/order-tracking")
                || path.startsWith("/403") || path.startsWith("/404")
                || path.startsWith("/error")
                || path.startsWith("/api/health")
                || path.startsWith("/api/payment/sepay/")) {
            return true;
        }
        // /popnow (trang list) là public nhưng các sub-path mở hộp/checkout là private
        if (path.startsWith("/popnow")) {
            return !path.startsWith("/popnow/pick/")
                    && !path.equals("/popnow/cabinet")
                    && !path.startsWith("/popnow/reveal/")
                    && !path.startsWith("/popnow/checkout/");
        }
        return false;
    }
}
