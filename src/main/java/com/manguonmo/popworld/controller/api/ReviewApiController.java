package com.manguonmo.popworld.controller.api;

import com.manguonmo.popworld.dto.request.ReviewCreateRequest;
import com.manguonmo.popworld.dto.response.ApiResponse;
import com.manguonmo.popworld.dto.response.ReviewResponse;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.service.ReviewService;
import com.manguonmo.popworld.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewApiController {

    private final ReviewService reviewService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> submitReview(@Valid @RequestBody ReviewCreateRequest request,
                                                                    Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Vui lòng đăng nhập để gửi đánh giá!"));
        }

        User user = userService.getUserByEmail(principal.getName());
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Tài khoản không hợp lệ hoặc đã bị khóa!"));
        }

        ReviewResponse response = reviewService.createReview(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Gửi đánh giá thành công! Đánh giá đang chờ ban quản trị kiểm duyệt.", response));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getApprovedReviewsByProduct(@PathVariable Long productId) {
        List<ReviewResponse> reviews = reviewService.getApprovedReviewsByProductId(productId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đánh giá thành công", reviews));
    }
}
