package com.manguonmo.popworld.service.impl;

import com.manguonmo.popworld.dto.response.CustomerStatsResponse;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.repository.UserRepository;
import com.manguonmo.popworld.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

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
