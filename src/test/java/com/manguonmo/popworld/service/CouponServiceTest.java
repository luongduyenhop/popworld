package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.response.CouponDiscountResponse;
import com.manguonmo.popworld.entity.Coupon;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.entity.UserCoupon;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.repository.CouponRepository;
import com.manguonmo.popworld.repository.UserCouponRepository;
import com.manguonmo.popworld.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CouponServiceImpl couponService;

    // Helper so sánh BigDecimal chính xác bỏ qua scale (200.00 vs 200)
    private void assertBigDecimalEquals(String expected, BigDecimal actual) {
        assertNotNull(actual, "Giá trị thực tế không được null");
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                String.format("Kỳ vọng: %s, nhưng thực tế: %s", expected, actual));
    }

    // =========================================================================
    // 1. NHÓM TEST: calculateDiscount (Tính toán & Kiểm tra điều kiện áp dụng)
    // =========================================================================
    @Nested
    @DisplayName("Kiểm thử calculateDiscount")
    class CalculateDiscountTests {

        @Test
        @DisplayName("Ném BadRequestException khi mã coupon rỗng hoặc null")
        void calculateDiscount_ThrowsException_WhenCouponCodeBlank() {
            assertThrows(BadRequestException.class,
                    () -> couponService.calculateDiscount(null, 1L, new BigDecimal("200000")));
            assertThrows(BadRequestException.class,
                    () -> couponService.calculateDiscount("   ", 1L, new BigDecimal("200000")));
        }

        @Test
        @DisplayName("Ném BadRequestException khi subtotal null hoặc <= 0")
        void calculateDiscount_ThrowsException_WhenSubtotalInvalid() {
            assertThrows(BadRequestException.class,
                    () -> couponService.calculateDiscount("POP10", 1L, null));
            assertThrows(BadRequestException.class,
                    () -> couponService.calculateDiscount("POP10", 1L, BigDecimal.ZERO));
            assertThrows(BadRequestException.class,
                    () -> couponService.calculateDiscount("POP10", 1L, new BigDecimal("-10000")));
        }

        @Test
        @DisplayName("Ném ResourceNotFoundException khi mã không tồn tại hoặc inactive")
        void calculateDiscount_ThrowsException_WhenCouponNotFound() {
            when(couponRepository.findByCodeAndActiveTrue("NOTFOUND"))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> couponService.calculateDiscount("NOTFOUND", 1L, new BigDecimal("200000")));
        }

        @Test
        @DisplayName("Ném BadRequestException khi mã chưa đến ngày bắt đầu có hiệu lực")
        void calculateDiscount_ThrowsException_WhenStartDateInFuture() {
            Coupon futureCoupon = Coupon.builder()
                    .id(1L)
                    .code("FUTURE")
                    .active(true)
                    .startDate(LocalDate.now().plusDays(2))
                    .build();

            when(couponRepository.findByCodeAndActiveTrue("FUTURE"))
                    .thenReturn(Optional.of(futureCoupon));

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> couponService.calculateDiscount("FUTURE", 1L, new BigDecimal("200000")));
            assertTrue(ex.getMessage().contains("chưa đến ngày có hiệu lực"));
        }

        @Test
        @DisplayName("Ném BadRequestException khi mã đã hết hạn sử dụng")
        void calculateDiscount_ThrowsException_WhenEndDateInPast() {
            Coupon expiredCoupon = Coupon.builder()
                    .id(2L)
                    .code("EXPIRED")
                    .active(true)
                    .endDate(LocalDate.now().minusDays(1))
                    .build();

            when(couponRepository.findByCodeAndActiveTrue("EXPIRED"))
                    .thenReturn(Optional.of(expiredCoupon));

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> couponService.calculateDiscount("EXPIRED", 1L, new BigDecimal("200000")));
            assertTrue(ex.getMessage().contains("đã hết hạn sử dụng"));
        }

        @Test
        @DisplayName("Ném BadRequestException khi subtotal chưa đạt minOrderAmount")
        void calculateDiscount_ThrowsException_WhenBelowMinOrderAmount() {
            Coupon minOrderCoupon = Coupon.builder()
                    .id(3L)
                    .code("MIN500K")
                    .active(true)
                    .minOrderAmount(new BigDecimal("500000"))
                    .build();

            when(couponRepository.findByCodeAndActiveTrue("MIN500K"))
                    .thenReturn(Optional.of(minOrderCoupon));

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> couponService.calculateDiscount("MIN500K", 1L, new BigDecimal("300000")));
            assertTrue(ex.getMessage().contains("chưa đạt giá trị tối thiểu"));
        }

        @Test
        @DisplayName("Ném BadRequestException khi mã đã hết lượt dùng tổng (usedCount >= usageLimit)")
        void calculateDiscount_ThrowsException_WhenUsageLimitExceeded() {
            Coupon limitCoupon = Coupon.builder()
                    .id(4L)
                    .code("LIMITED")
                    .active(true)
                    .usageLimit(100)
                    .usedCount(100)
                    .build();

            when(couponRepository.findByCodeAndActiveTrue("LIMITED"))
                    .thenReturn(Optional.of(limitCoupon));

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> couponService.calculateDiscount("LIMITED", 1L, new BigDecimal("300000")));
            assertTrue(ex.getMessage().contains("đã hết lượt sử dụng"));
        }

        @Test
        @DisplayName("Ném BadRequestException khi User đã từng sử dụng mã này rồi (UserCoupon.isUsed = true)")
        void calculateDiscount_ThrowsException_WhenUserAlreadyUsed() {
            Coupon coupon = Coupon.builder()
                    .id(5L)
                    .code("ONCE_PER_USER")
                    .active(true)
                    .build();

            UserCoupon usedUserCoupon = UserCoupon.builder()
                    .id(10L)
                    .isUsed(true)
                    .build();

            when(couponRepository.findByCodeAndActiveTrue("ONCE_PER_USER"))
                    .thenReturn(Optional.of(coupon));
            when(userCouponRepository.findByUserIdAndCouponId(1L, 5L))
                    .thenReturn(Optional.of(usedUserCoupon));

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> couponService.calculateDiscount("ONCE_PER_USER", 1L, new BigDecimal("300000")));
            assertTrue(ex.getMessage().contains("Bạn đã sử dụng mã giảm giá này rồi"));
        }

        @Test
        @DisplayName("Thành công: Tính giảm giá theo PERCENT và bị chặn trần maxDiscountAmount")
        void calculateDiscount_Success_PercentWithMaxCap() {
            Coupon percentCoupon = Coupon.builder()
                    .id(6L)
                    .code("SALE20")
                    .active(true)
                    .discountType("PERCENT")
                    .discountValue(new BigDecimal("20")) // 20%
                    .maxDiscountAmount(new BigDecimal("100000")) // Tối đa 100.000đ
                    .build();

            when(couponRepository.findByCodeAndActiveTrue("SALE20"))
                    .thenReturn(Optional.of(percentCoupon));

            // Đơn 1.000.000đ, 20% = 200.000đ > maxDiscountAmount 100.000đ -> giảm 100.000đ
            CouponDiscountResponse response = couponService.calculateDiscount("SALE20", null, new BigDecimal("1000000"));

            assertEquals("SALE20", response.getCouponCode());
            assertBigDecimalEquals("100000", response.getDiscountAmount());
            assertBigDecimalEquals("1000000", response.getSubtotal());
            assertBigDecimalEquals("900000", response.getNewTotal());
        }

        @Test
        @DisplayName("Thành công: Tính giảm giá theo PERCENT không bị trần")
        void calculateDiscount_Success_PercentWithoutCap() {
            Coupon percentCoupon = Coupon.builder()
                    .id(7L)
                    .code("SALE10")
                    .active(true)
                    .discountType("PERCENT")
                    .discountValue(new BigDecimal("10")) // 10%
                    .build();

            when(couponRepository.findByCodeAndActiveTrue("SALE10"))
                    .thenReturn(Optional.of(percentCoupon));

            // Đơn 500.000đ, 10% = 50.000đ -> giảm 50.000đ, còn 450.000đ
            CouponDiscountResponse response = couponService.calculateDiscount("SALE10", null, new BigDecimal("500000"));

            assertBigDecimalEquals("50000", response.getDiscountAmount());
            assertBigDecimalEquals("450000", response.getNewTotal());
        }

        @Test
        @DisplayName("Thành công: Tính giảm giá theo FIXED, không vượt quá subtotal")
        void calculateDiscount_Success_FixedAmount() {
            Coupon fixedCoupon = Coupon.builder()
                    .id(8L)
                    .code("GIAM50K")
                    .active(true)
                    .discountType("FIXED")
                    .discountValue(new BigDecimal("50000"))
                    .build();

            when(couponRepository.findByCodeAndActiveTrue("GIAM50K"))
                    .thenReturn(Optional.of(fixedCoupon));

            CouponDiscountResponse response = couponService.calculateDiscount("GIAM50K", null, new BigDecimal("200000"));

            assertBigDecimalEquals("50000", response.getDiscountAmount());
            assertBigDecimalEquals("150000", response.getNewTotal());
        }

        @Test
        @DisplayName("Thành công: Giảm giá FIXED lớn hơn subtotal thì giảm tối đa bằng subtotal (newTotal = 0)")
        void calculateDiscount_Success_FixedExceedsSubtotal() {
            Coupon fixedCoupon = Coupon.builder()
                    .id(9L)
                    .code("GIAM100K")
                    .active(true)
                    .discountType("FIXED")
                    .discountValue(new BigDecimal("100000"))
                    .build();

            when(couponRepository.findByCodeAndActiveTrue("GIAM100K"))
                    .thenReturn(Optional.of(fixedCoupon));

            // Đơn 80.000đ, mã 100.000đ -> giảm tối đa 80.000đ, newTotal = 0
            CouponDiscountResponse response = couponService.calculateDiscount("GIAM100K", null, new BigDecimal("80000"));

            assertBigDecimalEquals("80000", response.getDiscountAmount());
            assertBigDecimalEquals("0", response.getNewTotal());
        }
    }

    // =========================================================================
    // 2. NHÓM TEST: applyCoupon (Áp dụng mã nguyên tử & Lưu UserCoupon)
    // =========================================================================
    @Nested
    @DisplayName("Kiểm thử applyCoupon")
    class ApplyCouponTests {

        @Test
        @DisplayName("Ném BadRequestException khi cập nhật usedCount thất bại (vừa hết lượt dùng do tranh chấp)")
        void applyCoupon_ThrowsException_WhenAtomicUpdateFails() {
            Coupon coupon = Coupon.builder()
                    .id(1L)
                    .code("RACE_CODE")
                    .active(true)
                    .discountType("FIXED")
                    .discountValue(new BigDecimal("20000"))
                    .build();

            when(couponRepository.findByCodeAndActiveTrue("RACE_CODE"))
                    .thenReturn(Optional.of(coupon));
            // Giả lập tăng usedCount trả về 0 (đã bị luồng khác nhanh tay dùng hết)
            when(couponRepository.increaseUsedCount(1L)).thenReturn(0);

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> couponService.applyCoupon("RACE_CODE", 1L, new BigDecimal("200000")));
            assertTrue(ex.getMessage().contains("vừa hết lượt sử dụng"));
        }

        @Test
        @DisplayName("Thành công: Áp dụng mã cho User đã đăng nhập và chưa từng claim coupon (tạo mới UserCoupon)")
        void applyCoupon_Success_NewUserCouponCreated() {
            Coupon coupon = Coupon.builder()
                    .id(2L)
                    .code("WELCOME")
                    .active(true)
                    .discountType("FIXED")
                    .discountValue(new BigDecimal("30000"))
                    .build();

            User mockUser = User.builder().id(100L).fullName("Test User").email("test@example.com").build();

            when(couponRepository.findByCodeAndActiveTrue("WELCOME"))
                    .thenReturn(Optional.of(coupon));
            when(couponRepository.increaseUsedCount(2L)).thenReturn(1);
            when(userCouponRepository.findByUserIdAndCouponId(100L, 2L)).thenReturn(Optional.empty());
            when(userRepository.getReferenceById(100L)).thenReturn(mockUser);

            Coupon result = couponService.applyCoupon("WELCOME", 100L, new BigDecimal("200000"));

            assertEquals("WELCOME", result.getCode());
            verify(couponRepository, times(1)).increaseUsedCount(2L);

            ArgumentCaptor<UserCoupon> captor = ArgumentCaptor.forClass(UserCoupon.class);
            verify(userCouponRepository, times(1)).save(captor.capture());

            UserCoupon savedUserCoupon = captor.getValue();
            assertTrue(savedUserCoupon.getIsUsed());
            assertNotNull(savedUserCoupon.getUsedAt());
            assertEquals(mockUser, savedUserCoupon.getUser());
            assertEquals(coupon, savedUserCoupon.getCoupon());
        }

        @Test
        @DisplayName("Thành công: Áp dụng mã cho User đã có UserCoupon (cập nhật isUsed = true)")
        void applyCoupon_Success_ExistingUserCouponUpdated() {
            Coupon coupon = Coupon.builder()
                    .id(3L)
                    .code("CLAIMED_CODE")
                    .active(true)
                    .discountType("FIXED")
                    .discountValue(new BigDecimal("20000"))
                    .build();

            UserCoupon existingUserCoupon = UserCoupon.builder()
                    .id(55L)
                    .isUsed(false)
                    .claimedAt(LocalDateTime.now().minusDays(1))
                    .build();

            when(couponRepository.findByCodeAndActiveTrue("CLAIMED_CODE"))
                    .thenReturn(Optional.of(coupon));
            when(couponRepository.increaseUsedCount(3L)).thenReturn(1);
            when(userCouponRepository.findByUserIdAndCouponId(100L, 3L))
                    .thenReturn(Optional.of(existingUserCoupon));

            Coupon result = couponService.applyCoupon("CLAIMED_CODE", 100L, new BigDecimal("200000"));

            assertEquals("CLAIMED_CODE", result.getCode());
            assertTrue(existingUserCoupon.getIsUsed());
            assertNotNull(existingUserCoupon.getUsedAt());
            verify(userCouponRepository, times(1)).save(existingUserCoupon);
        }

        @Test
        @DisplayName("Thành công: Khách vãng lai (userId == null) chỉ tăng usedCount, không lưu UserCoupon")
        void applyCoupon_Success_GuestUser() {
            Coupon coupon = Coupon.builder()
                    .id(4L)
                    .code("GUEST_CODE")
                    .active(true)
                    .discountType("FIXED")
                    .discountValue(new BigDecimal("20000"))
                    .build();

            when(couponRepository.findByCodeAndActiveTrue("GUEST_CODE"))
                    .thenReturn(Optional.of(coupon));
            when(couponRepository.increaseUsedCount(4L)).thenReturn(1);

            Coupon result = couponService.applyCoupon("GUEST_CODE", null, new BigDecimal("200000"));

            assertEquals("GUEST_CODE", result.getCode());
            verify(couponRepository, times(1)).increaseUsedCount(4L);
            verify(userCouponRepository, never()).save(any(UserCoupon.class));
        }
    }

    // =========================================================================
    // 3. NHÓM TEST: releaseCoupon (Hoàn lại lượt dùng khi hủy đơn hàng)
    // =========================================================================
    @Nested
    @DisplayName("Kiểm thử releaseCoupon")
    class ReleaseCouponTests {

        @Test
        @DisplayName("releaseCoupon với couponId null thì không làm gì cả")
        void releaseCoupon_NullCouponId_DoesNothing() {
            couponService.releaseCoupon(null, 1L);

            verify(couponRepository, never()).decreaseUsedCount(anyLong());
            verify(userCouponRepository, never()).findByUserIdAndCouponId(anyLong(), anyLong());
        }

        @Test
        @DisplayName("Thành công: Hoàn lại lượt dùng cho cả Coupon và UserCoupon khi hủy đơn")
        void releaseCoupon_Success_WithUserId() {
            UserCoupon usedUserCoupon = UserCoupon.builder()
                    .id(88L)
                    .isUsed(true)
                    .usedAt(LocalDateTime.now().minusMinutes(15))
                    .build();

            when(userCouponRepository.findByUserIdAndCouponId(1L, 10L))
                    .thenReturn(Optional.of(usedUserCoupon));

            couponService.releaseCoupon(10L, 1L);

            verify(couponRepository, times(1)).decreaseUsedCount(10L);
            assertFalse(usedUserCoupon.getIsUsed());
            assertNull(usedUserCoupon.getUsedAt());
            verify(userCouponRepository, times(1)).save(usedUserCoupon);
        }

        @Test
        @DisplayName("Thành công: Hoàn lại lượt dùng cho Coupon khi userId null (khách vãng lai)")
        void releaseCoupon_Success_WithoutUserId() {
            couponService.releaseCoupon(10L, null);

            verify(couponRepository, times(1)).decreaseUsedCount(10L);
            verify(userCouponRepository, never()).findByUserIdAndCouponId(anyLong(), anyLong());
        }
    }
}
