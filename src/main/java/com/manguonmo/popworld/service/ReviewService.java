package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.request.ReviewCreateRequest;
import com.manguonmo.popworld.dto.response.ReviewResponse;
import com.manguonmo.popworld.dto.response.ReviewStatsResponse;

import java.util.List;

public interface ReviewService {

    List<ReviewResponse> getAdminReviews(String status, String keyword);

    ReviewStatsResponse getReviewStats();

    ReviewResponse getReviewById(Long id);

    ReviewResponse approveReview(Long id);

    ReviewResponse unapproveReview(Long id);

    ReviewResponse toggleApproval(Long id);

    void deleteReview(Long id);

    List<ReviewResponse> getApprovedReviewsByProductId(Long productId);

    // Customer methods
    ReviewResponse createReview(Long userId, ReviewCreateRequest request);

    boolean isUserEligibleToReview(Long userId, Long productId);

    ReviewResponse getUserReviewForProduct(Long userId, Long productId);

    List<ReviewResponse> getReviewsByUserId(Long userId);
}
