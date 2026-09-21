package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.response.CustomerStatsResponse;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.repository.UserRepository;
import com.manguonmo.popworld.service.impl.UserServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

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
}
