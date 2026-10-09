package com.manguonmo.popworld.controller.admin;

import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.repository.UserRepository;
import com.manguonmo.popworld.security.filter.Admin2faFilter;
import com.manguonmo.popworld.security.totp.TotpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Admin2faControllerTest {

    @Mock
    private TotpService totpService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private Admin2faController controller;

    private Authentication adminAuth;
    private User adminUser;

    @BeforeEach
    void setUp() {
        adminAuth = new UsernamePasswordAuthenticationToken(
                "admin@popworld.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        adminUser = User.builder()
                .id(1L)
                .email("admin@popworld.com")
                .role("ADMIN")
                .totpSecret("JBSWY3DPEHPK3PXP")
                .totpEnabled(true)
                .build();
    }

    @Test
    @DisplayName("2FA Controller: Hiển thị trang xác thực verify-2fa khi chưa xác nhận")
    void showVerify2faPage_NotVerified_ReturnsView() {
        MockHttpSession session = new MockHttpSession();
        Model model = new ConcurrentModel();

        String view = controller.showVerify2faPage(adminAuth, session, model);

        assertEquals("admin/verify-2fa", view);
        assertEquals("admin@popworld.com", model.getAttribute("adminEmail"));
    }

    @Test
    @DisplayName("2FA Controller: Đã xác thực 2FA trong session -> Redirect thẳng về dashboard")
    void showVerify2faPage_AlreadyVerified_RedirectsDashboard() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(Admin2faFilter.SESSION_2FA_VERIFIED, true);
        Model model = new ConcurrentModel();

        String view = controller.showVerify2faPage(adminAuth, session, model);

        assertEquals("redirect:/admin/dashboard", view);
    }

    @Test
    @DisplayName("2FA Controller: Nhập đúng mã OTP -> Set session verified và redirect dashboard")
    void verify2faCode_ValidCode_Success() {
        MockHttpSession session = new MockHttpSession();
        Model model = new ConcurrentModel();

        when(userRepository.findByEmail("admin@popworld.com")).thenReturn(Optional.of(adminUser));
        when(totpService.verifyCode("JBSWY3DPEHPK3PXP", "123456")).thenReturn(true);

        String view = controller.verify2faCode("123456", adminAuth, session, model);

        assertEquals("redirect:/admin/dashboard", view);
        assertEquals(Boolean.TRUE, session.getAttribute(Admin2faFilter.SESSION_2FA_VERIFIED));
    }

    @Test
    @DisplayName("2FA Controller: Nhập sai mã OTP -> Báo lỗi và ở lại trang verify-2fa")
    void verify2faCode_InvalidCode_Fails() {
        MockHttpSession session = new MockHttpSession();
        Model model = new ConcurrentModel();

        when(userRepository.findByEmail("admin@popworld.com")).thenReturn(Optional.of(adminUser));
        when(totpService.verifyCode("JBSWY3DPEHPK3PXP", "000000")).thenReturn(false);

        String view = controller.verify2faCode("000000", adminAuth, session, model);

        assertEquals("admin/verify-2fa", view);
        assertNotNull(model.getAttribute("error"));
        assertNull(session.getAttribute(Admin2faFilter.SESSION_2FA_VERIFIED));
    }

    @Test
    @DisplayName("2FA Setup: Bật 2FA thành công khi nhập đúng OTP xác nhận")
    void enable2fa_ValidCode_Succeeds() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("PENDING_2FA_SECRET", "NEWSECRETBASE32ABC");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        when(totpService.verifyCode("NEWSECRETBASE32ABC", "654321")).thenReturn(true);
        when(userRepository.findByEmail("admin@popworld.com")).thenReturn(Optional.of(adminUser));

        String view = controller.enable2fa("654321", adminAuth, session, redirectAttributes);

        assertEquals("redirect:/admin/2fa/setup", view);
        assertTrue(adminUser.getTotpEnabled());
        assertEquals("NEWSECRETBASE32ABC", adminUser.getTotpSecret());
        verify(userRepository, times(1)).save(adminUser);
        assertNotNull(redirectAttributes.getFlashAttributes().get("success"));
    }
}
