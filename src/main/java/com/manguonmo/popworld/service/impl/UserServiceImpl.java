package com.manguonmo.popworld.service.impl;

import com.manguonmo.popworld.dto.request.RegisterRequest;
import com.manguonmo.popworld.dto.response.CustomerStatsResponse;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.repository.UserRepository;
import com.manguonmo.popworld.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(
                () -> new  UsernameNotFoundException("Không tìm thấy username với email: "+ email)
        );
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

    @Override
    @Transactional
    public User updateProfile(Long userId, com.manguonmo.popworld.dto.request.ProfileUpdateRequest request) {
        if (userId == null) {
            throw new BadRequestException("Yêu cầu xác thực người dùng.");
        }
        User user = userRepository.findById(userId).orElseThrow(
                () -> new com.manguonmo.popworld.exception.ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId)
        );

        if (request.getFullName() == null || request.getFullName().trim().isBlank()) {
            throw new BadRequestException("Họ và tên không được để trống!");
        }

        user.setFullName(request.getFullName().trim());
        if (request.getPhone() != null && !request.getPhone().trim().isBlank()) {
            user.setPhone(request.getPhone().trim());
        }
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, com.manguonmo.popworld.dto.request.ChangePasswordRequest request) {
        if (userId == null) {
            throw new BadRequestException("Yêu cầu xác thực người dùng.");
        }
        User user = userRepository.findById(userId).orElseThrow(
                () -> new com.manguonmo.popworld.exception.ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId)
        );

        if (request.getCurrentPassword() == null || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu hiện tại không chính xác!");
        }

        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new BadRequestException("Mật khẩu mới phải có ít nhất 6 ký tự!");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu mới và mật khẩu xác nhận không khớp!");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu mới không được trùng với mật khẩu hiện tại!");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}

