package com.manguonmo.popworld.service.impl;

import com.manguonmo.popworld.dto.request.ChangePasswordRequest;
import com.manguonmo.popworld.dto.request.ProfileUpdateRequest;
import com.manguonmo.popworld.dto.request.RegisterRequest;
import com.manguonmo.popworld.dto.response.CustomerStatsResponse;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.repository.UserRepository;
import com.manguonmo.popworld.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import com.manguonmo.popworld.dto.response.PointTransactionResponse;
import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.RewardRedemption;
import com.manguonmo.popworld.repository.OrderRepository;
import com.manguonmo.popworld.repository.RewardRedemptionRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RewardRedemptionRepository rewardRedemptionRepository;
    private final OrderRepository orderRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public User register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String password = request.getPassword();
        String confirmPassword = request.getConfirmPassword();

        if (!password.equals(confirmPassword)) {
            throw new BadRequestException("Mật khẩu xác nhận không khớp");
        }
        if (userRepository.existsByEmail(email)) {
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
                () -> new UsernameNotFoundException("Không tìm thấy username với email: " + email)
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
    public User updateProfile(Long userId, ProfileUpdateRequest request) {
        if (userId == null) {
            throw new BadRequestException("Yêu cầu xác thực người dùng.");
        }
        User user = userRepository.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId)
        );

        if (request.getFullName() == null || request.getFullName().trim().isBlank()) {
            throw new BadRequestException("Họ và tên không được để trống!");
        }

        user.setFullName(request.getFullName().trim());
        if (request.getPhone() != null && !request.getPhone().trim().isBlank()) {
            user.setPhone(request.getPhone().trim());
        }
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname().trim());
        }
        if (request.getAvatarUrl() != null && !request.getAvatarUrl().trim().isBlank()) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }
        if (request.getGender() != null && !request.getGender().trim().isBlank()) {
            user.setGender(request.getGender().trim());
        }

        // Quy tắc bất biến POP MART: Ngày sinh một khi đã lưu thì KHÓA CỐ ĐỊNH, không cho sửa lại
        if (request.getBirthday() != null && user.getBirthday() == null) {
            user.setBirthday(request.getBirthday());
        }

        if (user.getMemberCode() == null || user.getMemberCode().isBlank()) {
            int randomNum = 100000000 + secureRandom.nextInt(900000000);
            user.setMemberCode("PM-" + randomNum);
        }

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        if (userId == null) {
            throw new BadRequestException("Yêu cầu xác thực người dùng.");
        }
        User user = userRepository.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId)
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

    @Override
    @Transactional
    public RewardRedemption redeemReward(Long userId, String rewardTitle, int pointsCost) {
        if (userId == null) {
            throw new BadRequestException("Yêu cầu đăng nhập để đổi quà.");
        }
        if (rewardTitle == null || rewardTitle.isBlank()) {
            throw new BadRequestException("Tên phần quà không hợp lệ.");
        }
        if (pointsCost <= 0) {
            throw new BadRequestException("Số điểm quy đổi không hợp lệ.");
        }

        User user = userRepository.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId)
        );

        int currentPoints = user.getRewardPoints() != null ? user.getRewardPoints() : 0;
        if (currentPoints < pointsCost) {
            throw new BadRequestException("Bạn không đủ điểm thưởng POP POINTS! Cần " + pointsCost + " điểm (Hiện có: " + currentPoints + " điểm).");
        }

        user.setRewardPoints(currentPoints - pointsCost);
        userRepository.save(user);

        String code = "RWD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        RewardRedemption redemption = RewardRedemption.builder()
                .user(user)
                .rewardTitle(rewardTitle.trim())
                .pointsCost(pointsCost)
                .redemptionCode(code)
                .status("CLAIMED")
                .notes("Đổi quà thành công từ POP POINTS")
                .build();

        return rewardRedemptionRepository.save(redemption);
    }

    @Override
    public List<PointTransactionResponse> getPointHistory(Long userId) {
        if (userId == null) return List.of();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        List<PointTransactionResponse> list = new ArrayList<>();

        // 1. Điểm từ đơn hàng
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (Order order : orders) {
            if ("DELIVERED".equalsIgnoreCase(order.getStatus()) && order.getPointsEarned() != null && order.getPointsEarned() > 0) {
                LocalDateTime time = order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getCreatedAt();
                list.add(PointTransactionResponse.builder()
                        .timestamp(time)
                        .timeFormatted(time != null ? time.format(dtf) : "-")
                        .remarks("Tích điểm hoàn tất đơn hàng #" + order.getOrderCode())
                        .pointsChange(order.getPointsEarned())
                        .isPositive(true)
                        .transactionType("ORDER_EARNED")
                        .build());
            }
            if (order.getPointsUsed() != null && order.getPointsUsed() > 0) {
                LocalDateTime time = order.getCreatedAt();
                list.add(PointTransactionResponse.builder()
                        .timestamp(time)
                        .timeFormatted(time != null ? time.format(dtf) : "-")
                        .remarks("Tiêu điểm giảm giá đơn hàng #" + order.getOrderCode())
                        .pointsChange(-order.getPointsUsed())
                        .isPositive(false)
                        .transactionType("ORDER_SPENT")
                        .build());
            }
        }

        // 2. Điểm từ đổi quà Member Rewards
        List<RewardRedemption> redemptions = rewardRedemptionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (RewardRedemption r : redemptions) {
            LocalDateTime time = r.getCreatedAt();
            list.add(PointTransactionResponse.builder()
                    .timestamp(time)
                    .timeFormatted(time != null ? time.format(dtf) : "-")
                    .remarks("Đổi quà: " + r.getRewardTitle())
                    .pointsChange(-r.getPointsCost())
                    .isPositive(false)
                    .transactionType("REWARD_REDEEMED")
                    .build());
        }

        list.sort(Comparator.comparing(PointTransactionResponse::getTimestamp, Comparator.nullsLast(Comparator.reverseOrder())));
        return list;
    }

    @Override
    public List<RewardRedemption> getRedeemedRewards(Long userId) {
        if (userId == null) return List.of();
        return rewardRedemptionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
