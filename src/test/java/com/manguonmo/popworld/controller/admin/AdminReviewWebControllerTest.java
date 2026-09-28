package com.manguonmo.popworld.controller.admin;

import com.manguonmo.popworld.dto.response.ReviewResponse;
import com.manguonmo.popworld.dto.response.ReviewStatsResponse;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminReviewWebControllerTest {

    @Mock
    private ReviewService reviewService;

    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private AdminReviewWebController controller;

    private ReviewStatsResponse sampleStats;

    @BeforeEach
    void setUp() {
        sampleStats = ReviewStatsResponse.builder()
                .totalReviews(5L)
                .approvedReviews(3L)
                .pendingReviews(2L)
                .averageRating(4.5)
                .build();
    }

    @Test
    @DisplayName("listReviews: Hiển thị danh sách đánh giá và thông số thống kê")
    void listReviews_ShouldPopulateModelAndReturnView() {
        when(reviewService.getAdminReviews("ALL", null)).thenReturn(Collections.emptyList());
        when(reviewService.getReviewStats()).thenReturn(sampleStats);

        String view = controller.listReviews("ALL", null, model);

        assertEquals("admin/reviews", view);
        verify(model).addAttribute(eq("reviews"), anyList());
        verify(model).addAttribute(eq("stats"), eq(sampleStats));
        verify(model).addAttribute("currentStatus", "ALL");
        verify(model).addAttribute("activeItem", "reviews");
        verify(model).addAttribute("pageTitle", "Quản Lý Đánh Giá & Kiểm Duyệt");
    }

    @Test
    @DisplayName("approveReview: Phê duyệt thành công -> Gửi flash success và chuyển hướng")
    void approveReview_Success_RedirectsWithFlashMessage() {
        when(reviewService.approveReview(10L)).thenReturn(ReviewResponse.builder().id(10L).approved(true).build());

        String view = controller.approveReview(10L, redirectAttributes);

        assertEquals("redirect:/admin/reviews", view);
        verify(reviewService).approveReview(10L);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), contains("Đã duyệt và cho phép hiển thị"));
    }

    @Test
    @DisplayName("approveReview: Không tìm thấy đánh giá -> Gửi flash error")
    void approveReview_NotFound_RedirectsWithErrorFlash() {
        when(reviewService.approveReview(999L)).thenThrow(new ResourceNotFoundException("Không tìm thấy đánh giá"));

        String view = controller.approveReview(999L, redirectAttributes);

        assertEquals("redirect:/admin/reviews", view);
        verify(redirectAttributes).addFlashAttribute("errorMessage", "Không tìm thấy đánh giá");
    }

    @Test
    @DisplayName("unapproveReview: Bỏ duyệt thành công -> Gửi flash success")
    void unapproveReview_Success_RedirectsWithFlashMessage() {
        when(reviewService.unapproveReview(10L)).thenReturn(ReviewResponse.builder().id(10L).approved(false).build());

        String view = controller.unapproveReview(10L, redirectAttributes);

        assertEquals("redirect:/admin/reviews", view);
        verify(reviewService).unapproveReview(10L);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), contains("Đã hủy duyệt và ẩn"));
    }

    @Test
    @DisplayName("toggleApproval: Đổi trạng thái kiểm duyệt thành công")
    void toggleApproval_Success_RedirectsWithFlashMessage() {
        when(reviewService.toggleApproval(10L)).thenReturn(ReviewResponse.builder().id(10L).approved(true).build());

        String view = controller.toggleApproval(10L, redirectAttributes);

        assertEquals("redirect:/admin/reviews", view);
        verify(reviewService).toggleApproval(10L);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), contains("Đã duyệt hiển thị"));
    }

    @Test
    @DisplayName("deleteReview: Xóa thành công -> Gửi flash success")
    void deleteReview_Success_RedirectsWithFlashMessage() {
        doNothing().when(reviewService).deleteReview(10L);

        String view = controller.deleteReview(10L, redirectAttributes);

        assertEquals("redirect:/admin/reviews", view);
        verify(reviewService).deleteReview(10L);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), contains("Đã xóa vĩnh viễn"));
    }

    @Test
    @DisplayName("deleteReview: Gặp lỗi hệ thống -> Gửi flash error")
    void deleteReview_Exception_RedirectsWithErrorFlash() {
        doThrow(new RuntimeException("DB error")).when(reviewService).deleteReview(10L);

        String view = controller.deleteReview(10L, redirectAttributes);

        assertEquals("redirect:/admin/reviews", view);
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), contains("Không thể xóa đánh giá"));
    }

    @Test
    @DisplayName("handleTypeMismatch: Xử lý ngoại lệ tham số sai kiểu dữ liệu")
    void handleTypeMismatch_ShouldRedirectWithErrorMessage() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getValue()).thenReturn("invalid-id");

        String view = controller.handleTypeMismatch(ex, redirectAttributes);

        assertEquals("redirect:/admin/reviews", view);
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), contains("invalid-id"));
    }
}
