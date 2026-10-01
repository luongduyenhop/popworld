package com.manguonmo.popworld.service.impl;

import com.manguonmo.popworld.dto.request.ReviewCreateRequest;
import com.manguonmo.popworld.dto.response.ReviewResponse;
import com.manguonmo.popworld.dto.response.ReviewStatsResponse;
import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.entity.Review;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.repository.OrderItemRepository;
import com.manguonmo.popworld.repository.ProductRepository;
import com.manguonmo.popworld.repository.ReviewRepository;
import com.manguonmo.popworld.repository.UserRepository;
import com.manguonmo.popworld.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    public List<ReviewResponse> getAdminReviews(String status, String keyword) {
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        String cleanStatus = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : "ALL";

        List<Review> reviews;

        if (cleanKeyword != null) {
            if ("PENDING".equals(cleanStatus) || "UNAPPROVED".equals(cleanStatus)) {
                reviews = reviewRepository.searchReviewsByApprovedWithUserAndProduct(false, cleanKeyword);
            } else if ("APPROVED".equals(cleanStatus)) {
                reviews = reviewRepository.searchReviewsByApprovedWithUserAndProduct(true, cleanKeyword);
            } else {
                reviews = reviewRepository.searchReviewsWithUserAndProduct(cleanKeyword);
            }
        } else {
            if ("PENDING".equals(cleanStatus) || "UNAPPROVED".equals(cleanStatus)) {
                reviews = reviewRepository.findByApprovedWithUserAndProduct(false);
            } else if ("APPROVED".equals(cleanStatus)) {
                reviews = reviewRepository.findByApprovedWithUserAndProduct(true);
            } else {
                reviews = reviewRepository.findAllWithUserAndProduct();
            }
        }

        return reviews.stream().map(this::mapToResponse).toList();
    }

    @Override
    public ReviewStatsResponse getReviewStats() {
        long total = reviewRepository.count();
        long approved = reviewRepository.countByApprovedTrue();
        long pending = reviewRepository.countByApprovedFalse();

        Double avg = reviewRepository.calculateAverageRating();
        double avgRating = 0.0;
        if (avg != null && !avg.isNaN()) {
            avgRating = BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP).doubleValue();
        }

        return ReviewStatsResponse.builder()
                .totalReviews(total)
                .approvedReviews(approved)
                .pendingReviews(pending)
                .averageRating(avgRating)
                .build();
    }

    @Override
    public ReviewResponse getReviewById(Long id) {
        Review review = findReviewOrThrow(id);
        return mapToResponse(review);
    }

    @Override
    @Transactional
    public ReviewResponse approveReview(Long id) {
        Review review = findReviewOrThrow(id);
        review.setApproved(true);
        Review saved = reviewRepository.save(review);
        log.info("Admin đã duyệt đánh giá ID={}", id);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ReviewResponse unapproveReview(Long id) {
        Review review = findReviewOrThrow(id);
        review.setApproved(false);
        Review saved = reviewRepository.save(review);
        log.info("Admin đã bỏ duyệt (ẩn) đánh giá ID={}", id);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ReviewResponse toggleApproval(Long id) {
        Review review = findReviewOrThrow(id);
        boolean newState = !Boolean.TRUE.equals(review.getApproved());
        review.setApproved(newState);
        Review saved = reviewRepository.save(review);
        log.info("Admin đã chuyển trạng thái kiểm duyệt đánh giá ID={} sang approved={}", id, newState);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteReview(Long id) {
        Review review = findReviewOrThrow(id);
        reviewRepository.delete(review);
        log.info("Admin đã xóa đánh giá ID={} an toàn khỏi hệ thống (không ảnh hưởng lịch sử mua hàng)", id);
    }

    @Override
    public List<ReviewResponse> getApprovedReviewsByProductId(Long productId) {
        if (productId == null) {
            throw new BadRequestException("ID sản phẩm không được để trống!");
        }
        return reviewRepository.findByProductIdAndApprovedTrueOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public ReviewResponse createReview(Long userId, ReviewCreateRequest request) {
        if (userId == null) {
            throw new BadRequestException("Thông tin người dùng không hợp lệ!");
        }
        if (request == null) {
            throw new BadRequestException("Dữ liệu đánh giá không được để trống!");
        }
        if (request.getProductId() == null) {
            throw new BadRequestException("ID sản phẩm không được để trống!");
        }
        if (request.getRating() != null && (request.getRating() < 1 || request.getRating() > 5)) {
            throw new BadRequestException("Số sao đánh giá phải từ 1 đến 5 sao!");
        }
        int rating = request.getRating() != null ? request.getRating() : 5;

        User user = userRepository.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId)
        );

        Product product = productRepository.findById(request.getProductId()).orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + request.getProductId())
        );

        // Kiểm tra chống đánh giá trùng lặp
        if (reviewRepository.existsByUserIdAndProductId(userId, request.getProductId())) {
            throw new BadRequestException("Bạn đã gửi đánh giá cho sản phẩm này rồi! Mỗi sản phẩm chỉ có thể đánh giá một lần.");
        }

        // Kiểm tra điều kiện mua hàng: Khách hàng phải có đơn hàng đã giao (DELIVERED/COMPLETED) chứa sản phẩm này
        boolean isDelivered = orderItemRepository.hasUserPurchasedProductDelivered(userId, request.getProductId());
        if (!isDelivered) {
            throw new BadRequestException("Bạn chỉ có thể đánh giá những sản phẩm đã mua và nhận hàng thành công!");
        }

        String cleanComment = (request.getComment() != null && !request.getComment().trim().isEmpty())
                ? request.getComment().trim() : null;
        String cleanImageUrl = validateAndSanitizeImageUrl(request.getReviewImageUrl());

        // Mặc định luôn là approved = false để chờ admin kiểm duyệt
        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(rating)
                .comment(cleanComment)
                .reviewImageUrl(cleanImageUrl)
                .approved(false)
                .build();

        Review saved = reviewRepository.save(review);
        log.info("Khách hàng userId={} đã gửi đánh giá mới cho productId={}, chờ duyệt: reviewId={}",
                userId, product.getId(), saved.getId());

        return mapToResponse(saved);
    }

    @Override
    public boolean isUserEligibleToReview(Long userId, Long productId) {
        if (userId == null || productId == null) {
            return false;
        }
        // Đã đánh giá rồi thì không còn quyền gửi thêm
        if (reviewRepository.existsByUserIdAndProductId(userId, productId)) {
            return false;
        }
        return orderItemRepository.hasUserPurchasedProductDelivered(userId, productId);
    }

    @Override
    public ReviewResponse getUserReviewForProduct(Long userId, Long productId) {
        if (userId == null || productId == null) {
            return null;
        }
        return reviewRepository.findByUserIdAndProductId(userId, productId)
                .map(this::mapToResponse)
                .orElse(null);
    }

    @Override
    public List<ReviewResponse> getReviewsByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private Review findReviewOrThrow(Long id) {
        if (id == null || id <= 0) {
            throw new BadRequestException("ID đánh giá không hợp lệ: " + id);
        }
        return reviewRepository.findByIdWithUserAndProduct(id)
                .or(() -> reviewRepository.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá với ID: " + id));
    }

    private ReviewResponse mapToResponse(Review review) {
        if (review == null) return null;

        Product product = review.getProduct();
        User user = review.getUser();

        String productName = product != null ? product.getName() : "Sản phẩm không xác định";
        String productSlug = product != null ? product.getSlug() : null;
        String productImageUrl = product != null ? product.getMainImageUrl() : null;
        Long productId = product != null ? product.getId() : null;

        String userName = "Khách hàng ẩn danh";
        String userEmail = "";
        String userAvatarUrl = "https://cdn-global.popmart.com/images/default-avatar.png";
        Long userId = null;
        if (user != null) {
            userId = user.getId();
            userName = user.getFullName() != null && !user.getFullName().isBlank()
                    ? user.getFullName() : (user.getEmail() != null ? user.getEmail() : "Khách hàng");
            userEmail = user.getEmail() != null ? user.getEmail() : "";
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isBlank()) {
                userAvatarUrl = user.getAvatarUrl();
            }
        }

        return ReviewResponse.builder()
                .id(review.getId())
                .productId(productId)
                .productName(productName)
                .productSlug(productSlug)
                .productImageUrl(productImageUrl)
                .userId(userId)
                .userName(userName)
                .userEmail(userEmail)
                .userAvatarUrl(userAvatarUrl)
                .rating(review.getRating() != null ? review.getRating() : 5)
                .comment(review.getComment())
                .reviewImageUrl(review.getReviewImageUrl())
                .approved(Boolean.TRUE.equals(review.getApproved()))
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

    private String validateAndSanitizeImageUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            return null;
        }
        String trimmed = rawUrl.trim();
        String lower = trimmed.toLowerCase();

        // Ngăn chặn các scheme nguy hiểm (XSS/RFI/Local File): javascript:, data:, vbscript:, file:
        if (lower.startsWith("javascript:") || lower.startsWith("data:") || lower.startsWith("vbscript:") || lower.startsWith("file:")) {
            throw new BadRequestException("Đường dẫn ảnh chứa giao thức không an toàn!");
        }

        // Bắt buộc URL tuyệt đối với scheme http hoặc https
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            throw new BadRequestException("Đường dẫn ảnh unboxing không hợp lệ hoặc không an toàn! Chỉ chấp nhận URL tuyệt đối bắt đầu bằng http:// hoặc https://.");
        }

        try {
            java.net.URI uri = java.net.URI.create(trimmed);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                throw new BadRequestException("Giao thức URL không được phép: " + scheme);
            }
            if (uri.getHost() == null || uri.getHost().trim().isEmpty()) {
                throw new BadRequestException("Đường dẫn ảnh thiếu tên miền (host) hợp lệ!");
            }
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Định dạng đường dẫn ảnh không hợp lệ: " + e.getMessage());
        }

        return trimmed;
    }
}
