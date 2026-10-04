package com.manguonmo.popworld.controller.admin;

import com.manguonmo.popworld.dto.response.ReviewResponse;
import com.manguonmo.popworld.dto.response.ReviewStatsResponse;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminReviewWebController {

    private final ReviewService reviewService;

    @GetMapping
    public String listReviews(@RequestParam(value = "status", required = false, defaultValue = "ALL") String status,
                              @RequestParam(value = "keyword", required = false) String keyword,
                              Model model) {
        String cleanStatus = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : "ALL";
        List<ReviewResponse> reviews = reviewService.getAdminReviews(cleanStatus, keyword);
        ReviewStatsResponse stats = reviewService.getReviewStats();

        model.addAttribute("reviews", reviews);
        model.addAttribute("stats", stats);
        model.addAttribute("currentStatus", cleanStatus);
        model.addAttribute("keyword", keyword);
        model.addAttribute("activeItem", "reviews");
        model.addAttribute("pageTitle", "Quản Lý Đánh Giá & Kiểm Duyệt");

        return "admin/reviews";
    }

    @PostMapping("/{id}/approve")
    public String approveReview(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            reviewService.approveReview(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã duyệt và cho phép hiển thị công khai đánh giá #" + id + "!");
        } catch (ResourceNotFoundException | BadRequestException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi khi duyệt đánh giá ID={}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể duyệt đánh giá: " + e.getMessage());
        }
        return "redirect:/admin/reviews";
    }

    @PostMapping("/{id}/unapprove")
    public String unapproveReview(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            reviewService.unapproveReview(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy duyệt và ẩn đánh giá #" + id + " khỏi trang chủ!");
        } catch (ResourceNotFoundException | BadRequestException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi khi hủy duyệt đánh giá ID={}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể hủy duyệt đánh giá: " + e.getMessage());
        }
        return "redirect:/admin/reviews";
    }

    @PostMapping("/{id}/toggle")
    public String toggleApproval(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            ReviewResponse updated = reviewService.toggleApproval(id);
            String stateText = Boolean.TRUE.equals(updated.getApproved()) ? "Đã duyệt hiển thị" : "Đã chuyển về Chờ duyệt (ẩn)";
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật đánh giá #" + id + ": " + stateText + "!");
        } catch (ResourceNotFoundException | BadRequestException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi khi đổi trạng thái kiểm duyệt đánh giá ID={}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
        }
        return "redirect:/admin/reviews";
    }

    @PostMapping("/{id}/delete")
    public String deleteReview(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            reviewService.deleteReview(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa vĩnh viễn đánh giá #" + id + " thành công!");
        } catch (ResourceNotFoundException | BadRequestException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi khi xóa đánh giá ID={}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa đánh giá: " + e.getMessage());
        }
        return "redirect:/admin/reviews";
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public String handleTypeMismatch(MethodArgumentTypeMismatchException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "Mã định danh đánh giá không hợp lệ: " + ex.getValue());
        return "redirect:/admin/reviews";
    }
}
