package com.manguonmo.popworld.controller.admin;

import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.repository.UserRepository;
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

    private final UserRepository userRepository;

    @GetMapping
    public String listCustomers(Model model) {
        List<User> customers = userRepository.findAll();

        long totalCustomers = customers.size();
        long vipCount = customers.stream().filter(u -> "VIP".equalsIgnoreCase(u.getMembershipTier())).count();
        long memberCount = totalCustomers - vipCount;

        model.addAttribute("customers", customers);
        model.addAttribute("totalCustomers", totalCustomers);
        model.addAttribute("vipCount", vipCount);
        model.addAttribute("memberCount", memberCount);
        model.addAttribute("activeNav", "customers");

        return "admin/customers";
    }
}
