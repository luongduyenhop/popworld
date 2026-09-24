package com.manguonmo.popworld.service.impl;

import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("loadUserByUsername: Thành công khi tài khoản tồn tại và đang hoạt động")
    void loadUserByUsername_WhenUserExistsAndActive_ShouldReturnUserDetails() {
        // Arrange
        User user = User.builder()
                .id(1L)
                .email("user@popworld.com")
                .password("encoded_pass_123")
                .role("ROLE_USER")
                .enabled(true)
                .build();

        when(userRepository.findByEmail("user@popworld.com")).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("user@popworld.com");

        // Assert
        assertNotNull(userDetails);
        assertEquals("user@popworld.com", userDetails.getUsername());
        assertEquals("encoded_pass_123", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));

        verify(userRepository, times(1)).findByEmail("user@popworld.com");
    }

    @Test
    @DisplayName("loadUserByUsername: Ném UsernameNotFoundException khi email không tồn tại")
    void loadUserByUsername_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@popworld.com")).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("nonexistent@popworld.com")
        );

        assertTrue(exception.getMessage().contains("Không tìm thấy"));
        verify(userRepository, times(1)).findByEmail("nonexistent@popworld.com");
    }

    @Test
    @DisplayName("loadUserByUsername: Ném DisabledException khi tài khoản bị khóa")
    void loadUserByUsername_WhenUserDisabled_ShouldThrowDisabledException() {
        // Arrange
        User disabledUser = User.builder()
                .id(2L)
                .email("blocked@popworld.com")
                .password("pass123")
                .role("ROLE_USER")
                .enabled(false)
                .build();

        when(userRepository.findByEmail("blocked@popworld.com")).thenReturn(Optional.of(disabledUser));

        // Act & Assert
        DisabledException exception = assertThrows(
                DisabledException.class,
                () -> customUserDetailsService.loadUserByUsername("blocked@popworld.com")
        );

        assertTrue(exception.getMessage().contains("vô hiệu hóa"));
        verify(userRepository, times(1)).findByEmail("blocked@popworld.com");
    }
}
