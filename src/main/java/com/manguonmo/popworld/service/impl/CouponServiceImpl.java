package com.manguonmo.popworld.service.impl;

import com.manguonmo.popworld.service.CouponService;
import com.manguonmo.popworld.dto.response.CouponDiscountResponse;
import com.manguonmo.popworld.entity.Coupon;
import com.manguonmo.popworld.entity.UserCoupon;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.repository.CouponRepository;
import com.manguonmo.popworld.repository.UserCouponRepository;
import com.manguonmo.popworld.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final UserRepository userRepository;

    public CouponServiceImpl(CouponRepository couponRepository, UserCouponRepository userCouponRepository, UserRepository userRepository) {
        this.couponRepository = couponRepository;
        this.userCouponRepository = userCouponRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CouponDiscountResponse calculateDiscount(String couponCode, Long userId, BigDecimal subtotal) {
        Coupon coupon = validateAndGetCoupon(couponCode, userId, subtotal);
        BigDecimal discountAmount = calculateDiscountAmount(coupon, subtotal);
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
    @Transactional
    public Coupon applyCoupon(String couponCode, Long userId, BigDecimal subtotal) {
        Coupon coupon = validateAndGetCoupon(couponCode, userId, subtotal);

        int updateRows = couponRepository.increaseUsedCount(coupon.getId());
        if (updateRows == 0) {
            throw new BadRequestException("Mã giảm giá vừa hết lượt sử dụng!");
        }

        if (userId != null) {
            UserCoupon userCoupon = userCouponRepository.findByUserIdAndCouponId(userId, coupon.getId())
                    .orElseGet(() -> UserCoupon.builder()
                            .coupon(coupon)
                            .user(userRepository.getReferenceById(userId))
                            .claimedAt(LocalDateTime.now())
                            .build());

            userCoupon.setIsUsed(true);
            userCoupon.setUsedAt(LocalDateTime.now());
            userCouponRepository.save(userCoupon);
        }

        return coupon;
    }

    @Override
    @Transactional
    public void releaseCoupon(Long couponId, Long userId) {
        if (couponId == null) {
            return;
        }
        couponRepository.decreaseUsedCount(couponId);
        if (userId != null) {
            userCouponRepository.findByUserIdAndCouponId(userId, couponId).ifPresent(userCoupon -> {
                userCoupon.setIsUsed(false);
                userCoupon.setUsedAt(null);
                userCouponRepository.save(userCoupon);
            });
        }
    }

    // =========================================================================
    // HELPER METHODS: Tách bạch trách nhiệm (SRP & DRY)
    // =========================================================================

    /**
     * Xác thực tính hợp lệ của mã giảm giá qua 6 tầng phòng thủ nghiêm ngặt
     */
    private Coupon validateAndGetCoupon(String couponCode, Long userId, BigDecimal subtotal) {
        // Tầng 1: Kiểm tra mã rỗng & tổng tiền hợp lệ
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

        return coupon;
    }

    /**
     * Tính toán số tiền được giảm theo loại PERCENT hoặc FIXED
     */
    private BigDecimal calculateDiscountAmount(Coupon coupon, BigDecimal subtotal) {
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

        return discountAmount;
    }
}
