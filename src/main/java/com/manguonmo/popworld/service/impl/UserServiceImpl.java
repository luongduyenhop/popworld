package com.manguonmo.popworld.service.impl;

import com.manguonmo.popworld.dto.request.RegisterRequest;
import com.manguonmo.popworld.dto.response.CustomerStatsResponse;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.repository.UserRepository;
import com.manguonmo.popworld.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String password = request.getPassword();
        String comfirmPassword = request.getConfirmPassword();

        if(!password.equals(comfirmPassword)){
            throw new BadRequestException("Mật khẩu xác nhận không khớp");
        }
        if(userRepository.existsByEmail(email)){
            throw new BadRequestException("Email đã được đăng ký, vui lòng chọn email khác hoặc đăng nhập");
        }
        password = passwordEncoder.encode(password);
        User user = User.builder()
                .fullName(request.getFullName())
                .email(email)
                .role("ROLE_USER")
                .password(password)
                .phone(request.getPhone())
                .enabled(true)
                .membershipTier("MEMBER")
                .build();
       return userRepository.save(user);

    }

    @Override
    public List<User> getAllCustomers() {
        return userRepository.findAll();
    }

    @Override
    public CustomerStatsResponse getCustomerStats() {
        List<User> customers = userRepository.findAll();
        long totalCustomers = customers.size();
        long vipCount = customers.stream().filter(u -> "VIP".equalsIgnoreCase(u.getMembershipTier())).count();
        long memberCount = totalCustomers - vipCount;

        return CustomerStatsResponse.builder()
                .totalCustomers(totalCustomers)
                .vipCount(vipCount)
                .memberCount(memberCount)
                .build();
    }
}
