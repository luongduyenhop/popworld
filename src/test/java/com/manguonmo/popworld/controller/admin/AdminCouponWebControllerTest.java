package com.manguonmo.popworld.controller.admin;

import com.manguonmo.popworld.entity.Coupon;
import com.manguonmo.popworld.service.CouponService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminCouponWebControllerTest {

    @Mock
    private CouponService couponService;

    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private AdminCouponWebController controller;

    @Test
    @DisplayName("listCoupons: Hiển thị danh sách và các chỉ số thống kê")
    void listCoupons_ReturnsViewAndModel() {
        Coupon c1 = Coupon.builder().id(1L).code("POP10").active(true).endDate(LocalDate.now().plusDays(10)).build();
        Coupon c2 = Coupon.builder().id(2L).code("EXP").active(false).endDate(LocalDate.now().minusDays(5)).build();

        when(couponService.getAllCoupons()).thenReturn(List.of(c1, c2));

        String view = controller.listCoupons(model);

        assertEquals("admin/coupons", view);
        verify(model).addAttribute("coupons", List.of(c1, c2));
        verify(model).addAttribute("totalCount", 2L);
        verify(model).addAttribute("activeCount", 1L);
        verify(model).addAttribute("expiredCount", 1L);
        verify(model).addAttribute("activeItem", "coupons");
    }

    @Test
    @DisplayName("createCoupon: Thành công và chuyển hướng về danh sách")
    void createCoupon_Success() {
        Coupon c = Coupon.builder().code("NEWCODE").discountValue(new BigDecimal("10")).build();
        when(couponService.createCoupon(any(Coupon.class))).thenReturn(c);

        String view = controller.createCoupon(c, redirectAttributes);

        assertEquals("redirect:/admin/coupons", view);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
    }

    @Test
    @DisplayName("toggleCoupon: Bật/Tắt trạng thái coupon")
    void toggleCoupon_Success() {
        Coupon c = Coupon.builder().id(1L).code("TEST").active(true).build();
        when(couponService.toggleCouponActive(1L)).thenReturn(c);

        String view = controller.toggleCoupon(1L, redirectAttributes);

        assertEquals("redirect:/admin/coupons", view);
        verify(couponService).toggleCouponActive(1L);
    }

    @Test
    @DisplayName("deleteCoupon: Xóa mã voucher")
    void deleteCoupon_Success() {
        doNothing().when(couponService).deleteCoupon(1L);

        String view = controller.deleteCoupon(1L, redirectAttributes);

        assertEquals("redirect:/admin/coupons", view);
        verify(couponService).deleteCoupon(1L);
    }
}
