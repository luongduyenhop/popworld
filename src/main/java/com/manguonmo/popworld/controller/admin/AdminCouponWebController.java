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
    public String listCoupons(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            Model model) {
        List<Coupon> allCoupons = couponService.getAllCoupons();
        boolean hasFilter = (keyword != null && !keyword.isBlank()) ||
                            (status != null && !"ALL".equalsIgnoreCase(status));
        List<Coupon> filteredCoupons = hasFilter 
                ? couponService.searchCoupons(keyword, status) 
                : allCoupons;

        long totalCount = allCoupons.size();
        LocalDate today = LocalDate.now();
        long activeCount = allCoupons.stream().filter(c -> Boolean.TRUE.equals(c.getActive()) && (c.getEndDate() == null || !c.getEndDate().isBefore(today))).count();
        long expiredCount = allCoupons.stream()
                .filter(c -> c.getEndDate() != null && c.getEndDate().isBefore(today))
                .count();

        model.addAttribute("coupons", filteredCoupons);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentStatus", status != null ? status.toUpperCase() : "ALL");
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("expiredCount", expiredCount);
        model.addAttribute("activeItem", "coupons");

        return "admin/coupons";
    }

    public String listCoupons(Model model) {
        return listCoupons(null, "ALL", model);
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
