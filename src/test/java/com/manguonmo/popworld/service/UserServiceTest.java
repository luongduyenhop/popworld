package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.request.RegisterRequest;
import com.manguonmo.popworld.dto.response.CustomerStatsResponse;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.repository.UserRepository;
import com.manguonmo.popworld.service.impl.UserServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("getAllCustomers: Trả về danh sách tất cả khách hàng")
    void getAllCustomers_ShouldReturnList() {
        User u1 = User.builder().id(1L).fullName("Alice").build();
        User u2 = User.builder().id(2L).fullName("Bob").build();
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        List<User> result = userService.getAllCustomers();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getCustomerStats: Tính toán số lượng tổng, VIP và Member chuẩn xác")
    void getCustomerStats_ShouldCalculateCorrectly() {
        User u1 = User.builder().id(1L).membershipTier("VIP").build();
        User u2 = User.builder().id(2L).membershipTier("VIP").build();
        User u3 = User.builder().id(3L).membershipTier("MEMBER").build();
        when(userRepository.findAll()).thenReturn(List.of(u1, u2, u3));

        CustomerStatsResponse stats = userService.getCustomerStats();

        assertNotNull(stats);
        assertEquals(3, stats.getTotalCustomers());
        assertEquals(2, stats.getVipCount());
        assertEquals(1, stats.getMemberCount());
    }

    @Test
    @DisplayName("register: Đăng ký thành công khi thông tin hợp lệ")
    void register_WhenValidRequest_ShouldSaveAndReturnUser() {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Nguyễn Văn A")
                .email("  TestUser@PopWorld.Com  ")
                .phone("0912345678")
                .password("password123")
                .confirmPassword("password123")
                .build();

        String normalizedEmail = "testuser@popworld.com";
        when(userRepository.existsByEmail(normalizedEmail)).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = userService.register(request);

        // Assert
        assertNotNull(result);
        assertEquals("Nguyễn Văn A", result.getFullName());
        assertEquals(normalizedEmail, result.getEmail());
        assertEquals("hashedPassword123", result.getPassword());
        assertEquals("0912345678", result.getPhone());
        assertEquals("ROLE_USER", result.getRole());
        assertEquals("MEMBER", result.getMembershipTier());
        assertTrue(result.getEnabled());

        verify(userRepository, times(1)).existsByEmail(normalizedEmail);
        verify(passwordEncoder, times(1)).encode("password123");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("register: Ném lỗi khi mật khẩu xác nhận không khớp")
    void register_WhenPasswordsDoNotMatch_ShouldThrowBadRequestException() {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Nguyễn Văn A")
                .email("test@popworld.com")
                .phone("0912345678")
                .password("password123")
                .confirmPassword("wrongPassword")
                .build();

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> userService.register(request));

        assertEquals("Mật khẩu xác nhận không khớp", exception.getMessage());
        verify(userRepository, never()).existsByEmail(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("register: Ném lỗi khi email đã tồn tại trong hệ thống")
    void register_WhenEmailAlreadyExists_ShouldThrowBadRequestException() {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Nguyễn Văn A")
                .email("duplicate@popworld.com")
                .phone("0912345678")
                .password("password123")
                .confirmPassword("password123")
                .build();

        when(userRepository.existsByEmail("duplicate@popworld.com")).thenReturn(true);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> userService.register(request));

        assertTrue(exception.getMessage().contains("Email đã được đăng ký"));
        verify(userRepository, times(1)).existsByEmail("duplicate@popworld.com");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("updateProfile: Cập nhật họ tên và số điện thoại thành công")
    void updateProfile_Success() {
        User user = User.builder().id(1L).fullName("Old Name").phone("0900000000").build();
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        com.manguonmo.popworld.dto.request.ProfileUpdateRequest req = com.manguonmo.popworld.dto.request.ProfileUpdateRequest.builder()
                .fullName("New Name")
                .phone("0988888888")
                .build();

        User updated = userService.updateProfile(1L, req);
        assertEquals("New Name", updated.getFullName());
        assertEquals("0988888888", updated.getPhone());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("updateProfile: Ném lỗi khi fullName bị rỗng")
    void updateProfile_BlankFullName_ThrowsBadRequest() {
        User user = User.builder().id(1L).fullName("Old Name").build();
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));

        com.manguonmo.popworld.dto.request.ProfileUpdateRequest req = com.manguonmo.popworld.dto.request.ProfileUpdateRequest.builder()
                .fullName("   ")
                .phone("0988888888")
                .build();

        assertThrows(BadRequestException.class, () -> userService.updateProfile(1L, req));
    }

    @Test
    @DisplayName("changePassword: Đổi mật khẩu thành công khi thông tin hợp lệ")
    void changePassword_Success() {
        User user = User.builder().id(1L).password("oldHash").build();
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("currentPass", "oldHash")).thenReturn(true);
        when(passwordEncoder.matches("newPass123", "oldHash")).thenReturn(false);
        when(passwordEncoder.encode("newPass123")).thenReturn("newHash");

        com.manguonmo.popworld.dto.request.ChangePasswordRequest req = com.manguonmo.popworld.dto.request.ChangePasswordRequest.builder()
                .currentPassword("currentPass")
                .newPassword("newPass123")
                .confirmPassword("newPass123")
                .build();

        userService.changePassword(1L, req);
        assertEquals("newHash", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("changePassword: Ném lỗi khi mật khẩu hiện tại không khớp")
    void changePassword_WrongCurrentPassword_ThrowsBadRequest() {
        User user = User.builder().id(1L).password("oldHash").build();
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("wrongPass", "oldHash")).thenReturn(false);

        com.manguonmo.popworld.dto.request.ChangePasswordRequest req = com.manguonmo.popworld.dto.request.ChangePasswordRequest.builder()
                .currentPassword("wrongPass")
                .newPassword("newPass123")
                .confirmPassword("newPass123")
                .build();

        assertThrows(BadRequestException.class, () -> userService.changePassword(1L, req));
    }

    @Test
    @DisplayName("changePassword: Ném lỗi khi mật khẩu xác nhận không trùng với mật khẩu mới")
    void changePassword_ConfirmPasswordMismatch_ThrowsBadRequest() {
        User user = User.builder().id(1L).password("oldHash").build();
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("currentPass", "oldHash")).thenReturn(true);

        com.manguonmo.popworld.dto.request.ChangePasswordRequest req = com.manguonmo.popworld.dto.request.ChangePasswordRequest.builder()
                .currentPassword("currentPass")
                .newPassword("newPass123")
                .confirmPassword("differentPass")
                .build();

        assertThrows(BadRequestException.class, () -> userService.changePassword(1L, req));
    }
}

