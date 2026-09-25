package com.manguonmo.popworld.controller.api;

import com.manguonmo.popworld.dto.request.CouponValidateRequest;
import com.manguonmo.popworld.dto.response.ApiResponse;
import com.manguonmo.popworld.dto.response.CouponDiscountResponse;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.service.CouponService;
import com.manguonmo.popworld.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;

@RestController
@RequestMapping("/api/coupons")
public class CouponApiController {
    private final CouponService couponService;
    private final UserService userService;

    public CouponApiController(CouponService couponService, UserService userService) {
        this.couponService = couponService;
        this.userService = userService;
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<CouponDiscountResponse>> validateCoupon(
            @Valid @RequestBody CouponValidateRequest request,
            Principal principal
    ) {
        Long userId = null;
        if (principal != null) {
            try {
                User currentUser = userService.getUserByEmail(principal.getName());
                if (currentUser != null) {
                    userId = currentUser.getId();
                }
            } catch (Exception ignored) {
                // Anonymous or user not found
            }
        }

        CouponDiscountResponse response = couponService.calculateDiscount(request.getCouponCode(), userId, request.getSubtotal());
        return ResponseEntity.ok(ApiResponse.success("Áp dụng mã giảm giá thành công", response));
    }
}
