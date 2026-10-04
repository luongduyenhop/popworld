package com.manguonmo.popworld.controller.admin;

import com.manguonmo.popworld.entity.Coupon;
import com.manguonmo.popworld.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin/coupons")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCouponWebController {

    private final CouponService couponService;

    @GetMapping
    public String listCoupons(Model model) {
        List<Coupon> coupons = couponService.getAllCoupons();

        long totalCount = coupons.size();
        long activeCount = coupons.stream().filter(c -> Boolean.TRUE.equals(c.getActive())).count();
        LocalDate today = LocalDate.now();
        long expiredCount = coupons.stream()
                .filter(c -> c.getEndDate() != null && c.getEndDate().isBefore(today))
                .count();

        model.addAttribute("coupons", coupons);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("expiredCount", expiredCount);
        model.addAttribute("activeItem", "coupons");

        return "admin/coupons";
    }

    @PostMapping
    public String createCoupon(@ModelAttribute Coupon coupon, RedirectAttributes redirectAttributes) {
        try {
            Coupon created = couponService.createCoupon(coupon);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã tạo thành công mã giảm giá: " + created.getCode());
        } catch (Exception e) {
            log.error("Lỗi khi tạo mã giảm giá: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tạo mã giảm giá: " + e.getMessage());
        }
        return "redirect:/admin/coupons";
    }

    @PostMapping("/{id}/toggle")
    public String toggleCoupon(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Coupon updated = couponService.toggleCouponActive(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã " + (Boolean.TRUE.equals(updated.getActive()) ? "kích hoạt" : "tạm dừng") + " mã giảm giá: " + updated.getCode());
        } catch (Exception e) {
            log.error("Lỗi khi chuyển trạng thái coupon #{}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/coupons";
    }

    @PostMapping("/{id}/delete")
    public String deleteCoupon(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            couponService.deleteCoupon(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa / tạm dừng mã giảm giá thành công!");
        } catch (Exception e) {
            log.error("Lỗi khi xóa coupon #{}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/coupons";
    }
}
