package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.response.CouponDiscountResponse;
import com.manguonmo.popworld.entity.Coupon;

import java.math.BigDecimal;

public interface CouponService {

    // 1. Kiểm tra mã và tính trước tiền giảm (phục vụ nút "Kiểm Tra" trên giao diện & OrderService)

    CouponDiscountResponse calculateDiscount(String couponCode, Long userId, BigDecimal subtotal);

    // 2. Thực sự áp dụng mã khi đặt hàng (Tăng usedCount nguyên tử + Đánh dấu UserCoupon)

    Coupon applyCoupon(String couponCode, Long userId, BigDecimal subtotal);

    // 3. Hoàn lại lượt dùng khi hủy đơn hàng quá 15 phút (Giảm usedCount + Mở lại UserCoupon)

    void releaseCoupon(Long couponId, Long userId);
}
