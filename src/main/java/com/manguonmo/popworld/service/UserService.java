package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.request.ChangePasswordRequest;
import com.manguonmo.popworld.dto.request.ProfileUpdateRequest;
import com.manguonmo.popworld.dto.request.RegisterRequest;
import com.manguonmo.popworld.dto.response.CustomerStatsResponse;
import com.manguonmo.popworld.entity.User;

import java.util.List;

public interface UserService {
    List<User> getAllCustomers();
    CustomerStatsResponse getCustomerStats();
    User register(RegisterRequest request);
    User getUserByEmail(String email);
    User updateProfile(Long userId, ProfileUpdateRequest request);
    void changePassword(Long userId, ChangePasswordRequest request);
    com.manguonmo.popworld.entity.RewardRedemption redeemReward(Long userId, String rewardTitle, int pointsCost);
    List<com.manguonmo.popworld.dto.response.PointTransactionResponse> getPointHistory(Long userId);
    List<com.manguonmo.popworld.entity.RewardRedemption> getRedeemedRewards(Long userId);
    User getUserById(Long userId);
    User toggleUserStatus(Long userId);
    User adjustUserPoints(Long userId, Integer rewardPointsDelta, Integer luckyPointsDelta, Integer hintCardsDelta);
}
