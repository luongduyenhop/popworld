package com.manguonmo.popworld.controller.client;

import com.manguonmo.popworld.dto.request.RegisterRequest;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthWebController.class)
class AuthWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("GET /register: Hiển thị form đăng ký thành công")
    void showRegisterForm_ShouldReturnRegisterView() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("registerRequest"));
    }

    @Test
    @DisplayName("POST /register: Thất bại khi dữ liệu validation không hợp lệ")
    void handleRegister_WhenValidationFails_ShouldReturnRegisterView() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "")
                        .param("email", "invalid-email")
                        .param("password", "123")
                        .param("confirmPassword", "123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().hasErrors());

        verify(userService, never()).register(any());
    }

    @Test
    @DisplayName("POST /register: Thất bại khi Service quăng lỗi BadRequestException (trùng email)")
    void handleRegister_WhenServiceThrowsBadRequest_ShouldReturnRegisterViewWithErrorMessage() throws Exception {
        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(new BadRequestException("Email đã được đăng ký"));

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Nguyễn Văn A")
                        .param("email", "existing@example.com")
                        .param("phone", "0912345678")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("errorMessage", "Email đã được đăng ký"));

        verify(userService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /register: Đăng ký thành công, redirect sang /login kèm flash attribute")
    void handleRegister_WhenSuccess_ShouldRedirectToLogin() throws Exception {
        User mockUser = User.builder().id(1L).email("newuser@example.com").build();
        when(userService.register(any(RegisterRequest.class))).thenReturn(mockUser);

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Nguyễn Văn A")
                        .param("email", "newuser@example.com")
                        .param("phone", "0912345678")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(userService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("GET /login: Hiển thị form đăng nhập thành công")
    void showLoginForm_ShouldReturnLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    @DisplayName("GET /403: Hiển thị trang từ chối truy cập 403 thành công")
    void showAccessDeniedPage_ShouldReturn403View() throws Exception {
        mockMvc.perform(get("/403"))
                .andExpect(status().isOk())
                .andExpect(view().name("403"));
    }
}
