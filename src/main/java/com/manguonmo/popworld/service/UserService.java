package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.request.RegisterRequest;
import com.manguonmo.popworld.dto.response.CustomerStatsResponse;
import com.manguonmo.popworld.entity.User;

import java.util.List;

public interface UserService {
    List<User> getAllCustomers();
    CustomerStatsResponse getCustomerStats();
    User register(RegisterRequest request);
    User getUserByEmail(String email);
}
