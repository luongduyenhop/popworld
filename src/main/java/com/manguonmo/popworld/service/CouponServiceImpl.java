package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.response.CouponDiscountResponse;
import com.manguonmo.popworld.entity.Coupon;
import com.manguonmo.popworld.entity.UserCoupon;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.repository.CouponRepository;
import com.manguonmo.popworld.repository.UserCouponRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    public CouponServiceImpl(CouponRepository couponRepository, UserCouponRepository userCouponRepository) {
        this.couponRepository = couponRepository;
        this.userCouponRepository = userCouponRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CouponDiscountResponse calculateDiscount(String couponCode, Long userId, BigDecimal subtotal) {
        // Tầng 1: Kiểm tra mã rỗng
        if (couponCode == null || couponCode.trim().isEmpty()) {
            throw new BadRequestException("Vui lòng nhập mã giảm giá!");
        }

        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Giá trị đơn hàng không hợp lệ để tính giảm giá!");
        }

        // Tầng 2: Tìm mã tồn tại và còn active
        Coupon coupon = couponRepository.findByCodeAndActiveTrue(couponCode.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Mã giảm giá '" + couponCode.trim().toUpperCase() + "' không tồn tại hoặc đã hết hiệu lực!"));

        // Tầng 3: Kiểm tra ngày bắt đầu và ngày kết thúc
        LocalDate now = LocalDate.now();
        if (coupon.getStartDate() != null && now.isBefore(coupon.getStartDate())) {
            throw new BadRequestException("Mã giảm giá chưa đến ngày có hiệu lực (bắt đầu từ " + coupon.getStartDate() + ")!");
        }
        if (coupon.getEndDate() != null && now.isAfter(coupon.getEndDate())) {
            throw new BadRequestException("Mã giảm giá đã hết hạn sử dụng (hết hạn vào " + coupon.getEndDate() + ")!");
        }

        // Tầng 4: Kiểm tra giá trị đơn tối thiểu
        BigDecimal minOrderAmount = coupon.getMinOrderAmount();
        if (minOrderAmount != null && subtotal.compareTo(minOrderAmount) < 0) {
            throw new BadRequestException("Đơn hàng chưa đạt giá trị tối thiểu " + minOrderAmount.longValue() + " đ để dùng mã này!");
        }

        // Tầng 5: Kiểm tra lượt dùng tổng
        Integer usedCount = coupon.getUsedCount() != null ? coupon.getUsedCount() : 0;
        Integer usageLimit = coupon.getUsageLimit();
        if (usageLimit != null && usedCount >= usageLimit) {
            throw new BadRequestException("Mã giảm giá đã hết lượt sử dụng!");
        }

        // Tầng 6: Chống gian lận - Mỗi User chỉ dùng 1 lần (nếu đã login)
        if (userId != null) {
            Optional<UserCoupon> userCoupon = userCouponRepository.findByUserIdAndCouponId(userId, coupon.getId());
            if (userCoupon.isPresent() && Boolean.TRUE.equals(userCoupon.get().getIsUsed())) {
                throw new BadRequestException("Bạn đã sử dụng mã giảm giá này rồi! Mỗi tài khoản chỉ được áp dụng 1 lần.");
            }
        }

        // Tầng 7: Tính toán số tiền được giảm
        BigDecimal discountAmount;
        if ("PERCENT".equalsIgnoreCase(coupon.getDiscountType()) || "PERCENTAGE".equalsIgnoreCase(coupon.getDiscountType())) {
            BigDecimal rate = coupon.getDiscountValue().compareTo(BigDecimal.ONE) > 0
                    ? coupon.getDiscountValue().divide(BigDecimal.valueOf(100))
                    : coupon.getDiscountValue();
            discountAmount = subtotal.multiply(rate);

            // Áp trần số tiền giảm tối đa nếu có
            if (coupon.getMaxDiscountAmount() != null && discountAmount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                discountAmount = coupon.getMaxDiscountAmount();
            }
        } else {
            // FIXED hoặc FIXED_AMOUNT
            discountAmount = coupon.getDiscountValue();
        }

        // Tiền giảm không bao giờ được lớn hơn tổng tiền hàng
        if (discountAmount.compareTo(subtotal) > 0) {
            discountAmount = subtotal;
        }

        BigDecimal newTotal = subtotal.subtract(discountAmount);

        return CouponDiscountResponse.builder()
                .couponCode(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .discountAmount(discountAmount)
                .subtotal(subtotal)
                .newTotal(newTotal)
                .message("Áp dụng mã giảm giá '" + coupon.getCode() + "' thành công!")
                .build();
    }

    @Override
    public Coupon applyCoupon(String couponCode, Long userId, BigDecimal subtotal) {
        return null;
    }

    @Override
    public void releaseCoupon(Long couponId, Long userId) {

    }
}
