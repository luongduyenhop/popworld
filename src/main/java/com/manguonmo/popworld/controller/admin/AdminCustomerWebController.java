package com.manguonmo.popworld.controller.admin;

import com.manguonmo.popworld.dto.response.CustomerStatsResponse;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin/customers")
@RequiredArgsConstructor
public class AdminCustomerWebController {

    private final UserService userService;

    @GetMapping
    public String listCustomers(Model model) {
        List<User> customers = userService.getAllCustomers();
        CustomerStatsResponse stats = userService.getCustomerStats();

        model.addAttribute("customers", customers);
        model.addAttribute("totalCustomers", stats.getTotalCustomers());
        model.addAttribute("vipCount", stats.getVipCount());
        model.addAttribute("memberCount", stats.getMemberCount());
        model.addAttribute("activeNav", "customers");

        return "admin/customers";
    }
}
