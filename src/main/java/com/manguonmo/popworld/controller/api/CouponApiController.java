package com.manguonmo.popworld.controller.api;

import com.manguonmo.popworld.dto.request.CouponValidateRequest;
import com.manguonmo.popworld.dto.response.ApiResponse;
import com.manguonmo.popworld.dto.response.CouponDiscountResponse;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.service.CartService;
import com.manguonmo.popworld.service.CouponService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/coupons")
public class CouponApiController {
    private final CouponService couponService;
    private final CartService cartService;

    public CouponApiController(CouponService couponService, CartService cartService) {
        this.couponService = couponService;
        this.cartService = cartService;
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<CouponDiscountResponse>> validateCoupon(
            @Valid @RequestBody CouponValidateRequest request
            ){
        User currentUser = cartService.getDefaultUser();
        Long userId = (currentUser != null) ? currentUser.getId():null;


        CouponDiscountResponse response =couponService.calculateDiscount(request.getCouponCode(),userId,request.getSubtotal());
        return ResponseEntity.ok(ApiResponse.success("Áp dụng mã giảm giá thành công",response));

    }
}
