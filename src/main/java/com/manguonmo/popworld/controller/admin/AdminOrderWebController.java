package com.manguonmo.popworld.controller.admin;

import com.manguonmo.popworld.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminOrderWebController {

    private final OrderService orderService;

    public AdminOrderWebController(OrderService orderService) {
        this.orderService = orderService;
    }
}
