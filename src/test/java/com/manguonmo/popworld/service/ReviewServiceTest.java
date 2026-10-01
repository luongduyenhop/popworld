package com.manguonmo.popworld.service;

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
import com.manguonmo.popworld.service.impl.ReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User sampleUser;
    private Product sampleProduct;
    private Review pendingReview;
    private Review approvedReview;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .fullName("Lê Hoàng")
                .email("lehoang@example.com")
                .build();

        sampleProduct = Product.builder()
                .id(10L)
                .name("Labubu The Monsters Tasty Macarons")
                .slug("labubu-tasty-macarons")
                .build();

        pendingReview = Review.builder()
                .id(100L)
                .user(sampleUser)
                .product(sampleProduct)
                .rating(5)
                .comment("Hộp rất đẹp, bóc ra được màu hồng mê ly!")
                .reviewImageUrl("https://image.com/unbox1.png")
                .approved(false)
                .build();
        pendingReview.setCreatedAt(LocalDateTime.now());

        approvedReview = Review.builder()
                .id(101L)
                .user(sampleUser)
                .product(sampleProduct)
                .rating(4)
                .comment("Chất lượng hoàn thiện tốt, giao hàng nhanh.")
                .reviewImageUrl(null)
                .approved(true)
                .build();
        approvedReview.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("getAdminReviews: Trạng thái ALL không keyword -> Gọi findAllWithUserAndProduct")
    void getAdminReviews_AllStatus_NoKeyword() {
        when(reviewRepository.findAllWithUserAndProduct()).thenReturn(List.of(pendingReview, approvedReview));

        List<ReviewResponse> result = reviewService.getAdminReviews("ALL", null);

        assertEquals(2, result.size());
        assertEquals("Labubu The Monsters Tasty Macarons", result.get(0).getProductName());
        verify(reviewRepository).findAllWithUserAndProduct();
    }

    @Test
    @DisplayName("getAdminReviews: Trạng thái PENDING không keyword -> Gọi findByApprovedWithUserAndProduct(false)")
    void getAdminReviews_PendingStatus_NoKeyword() {
        when(reviewRepository.findByApprovedWithUserAndProduct(false)).thenReturn(List.of(pendingReview));

        List<ReviewResponse> result = reviewService.getAdminReviews("PENDING", null);

        assertEquals(1, result.size());
        assertFalse(result.get(0).getApproved());
        verify(reviewRepository).findByApprovedWithUserAndProduct(false);
    }

    @Test
    @DisplayName("getAdminReviews: Trạng thái APPROVED không keyword -> Gọi findByApprovedWithUserAndProduct(true)")
    void getAdminReviews_ApprovedStatus_NoKeyword() {
        when(reviewRepository.findByApprovedWithUserAndProduct(true)).thenReturn(List.of(approvedReview));

        List<ReviewResponse> result = reviewService.getAdminReviews("APPROVED", null);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getApproved());
        verify(reviewRepository).findByApprovedWithUserAndProduct(true);
    }

    @Test
    @DisplayName("getAdminReviews: Có từ khóa tìm kiếm khi status ALL -> Gọi searchReviewsWithUserAndProduct")
    void getAdminReviews_WithKeyword_AllStatus() {
        when(reviewRepository.searchReviewsWithUserAndProduct("Labubu")).thenReturn(List.of(pendingReview));

        List<ReviewResponse> result = reviewService.getAdminReviews("ALL", "Labubu");

        assertEquals(1, result.size());
        verify(reviewRepository).searchReviewsWithUserAndProduct("Labubu");
    }

    @Test
    @DisplayName("getAdminReviews: Có từ khóa tìm kiếm khi status PENDING -> Gọi searchReviewsByApprovedWithUserAndProduct(false, kw)")
    void getAdminReviews_WithKeyword_PendingStatus() {
        when(reviewRepository.searchReviewsByApprovedWithUserAndProduct(false, "Labubu")).thenReturn(List.of(pendingReview));

        List<ReviewResponse> result = reviewService.getAdminReviews("PENDING", "Labubu");

        assertEquals(1, result.size());
        verify(reviewRepository).searchReviewsByApprovedWithUserAndProduct(false, "Labubu");
    }

    @Test
    @DisplayName("getReviewStats: Thống kê số lượng và điểm trung bình chính xác")
    void getReviewStats_Success() {
        when(reviewRepository.count()).thenReturn(10L);
        when(reviewRepository.countByApprovedTrue()).thenReturn(7L);
        when(reviewRepository.countByApprovedFalse()).thenReturn(3L);
        when(reviewRepository.calculateAverageRating()).thenReturn(4.66666);

        ReviewStatsResponse stats = reviewService.getReviewStats();

        assertEquals(10L, stats.getTotalReviews());
        assertEquals(7L, stats.getApprovedReviews());
        assertEquals(3L, stats.getPendingReviews());
        assertEquals(4.7, stats.getAverageRating());
    }

    @Test
    @DisplayName("getReviewStats: Khi chưa có đánh giá nào -> avgRating trả về 0.0 an toàn")
    void getReviewStats_EmptyReviews_ReturnsZeroAvg() {
        when(reviewRepository.count()).thenReturn(0L);
        when(reviewRepository.countByApprovedTrue()).thenReturn(0L);
        when(reviewRepository.countByApprovedFalse()).thenReturn(0L);
        when(reviewRepository.calculateAverageRating()).thenReturn(null);

        ReviewStatsResponse stats = reviewService.getReviewStats();

        assertEquals(0L, stats.getTotalReviews());
        assertEquals(0.0, stats.getAverageRating());
    }

    @Test
    @DisplayName("getReviewById: Tìm thấy đánh giá -> Trả về DTO")
    void getReviewById_Found_ReturnsResponse() {
        when(reviewRepository.findByIdWithUserAndProduct(100L)).thenReturn(Optional.of(pendingReview));

        ReviewResponse response = reviewService.getReviewById(100L);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("Lê Hoàng", response.getUserName());
        assertEquals(5, response.getRating());
    }

    @Test
    @DisplayName("getReviewById: Không tìm thấy -> Ném ResourceNotFoundException")
    void getReviewById_NotFound_ThrowsException() {
        when(reviewRepository.findByIdWithUserAndProduct(999L)).thenReturn(Optional.empty());
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> reviewService.getReviewById(999L));
    }

    @Test
    @DisplayName("getReviewById: ID không hợp lệ (null hoặc âm) -> Ném BadRequestException")
    void getReviewById_InvalidId_ThrowsException() {
        assertThrows(BadRequestException.class, () -> reviewService.getReviewById(null));
        assertThrows(BadRequestException.class, () -> reviewService.getReviewById(-1L));
    }

    @Test
    @DisplayName("approveReview: Phê duyệt đánh giá -> Chuyển approved thành true và lưu")
    void approveReview_Success() {
        when(reviewRepository.findByIdWithUserAndProduct(100L)).thenReturn(Optional.of(pendingReview));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewResponse response = reviewService.approveReview(100L);

        assertTrue(response.getApproved());
        verify(reviewRepository).save(pendingReview);
    }

    @Test
    @DisplayName("unapproveReview: Hủy duyệt đánh giá -> Chuyển approved thành false và lưu")
    void unapproveReview_Success() {
        when(reviewRepository.findByIdWithUserAndProduct(101L)).thenReturn(Optional.of(approvedReview));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewResponse response = reviewService.unapproveReview(101L);

        assertFalse(response.getApproved());
        verify(reviewRepository).save(approvedReview);
    }

    @Test
    @DisplayName("toggleApproval: Đổi từ false sang true thành công")
    void toggleApproval_FalseToTrue() {
        when(reviewRepository.findByIdWithUserAndProduct(100L)).thenReturn(Optional.of(pendingReview));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewResponse response = reviewService.toggleApproval(100L);

        assertTrue(response.getApproved());
        verify(reviewRepository).save(pendingReview);
    }

    @Test
    @DisplayName("toggleApproval: Đổi từ true sang false thành công")
    void toggleApproval_TrueToFalse() {
        when(reviewRepository.findByIdWithUserAndProduct(101L)).thenReturn(Optional.of(approvedReview));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewResponse response = reviewService.toggleApproval(101L);

        assertFalse(response.getApproved());
        verify(reviewRepository).save(approvedReview);
    }

    @Test
    @DisplayName("deleteReview: Xóa bản ghi đánh giá thành công")
    void deleteReview_Success() {
        when(reviewRepository.findByIdWithUserAndProduct(100L)).thenReturn(Optional.of(pendingReview));

        reviewService.deleteReview(100L);

        verify(reviewRepository).delete(pendingReview);
    }

    @Test
    @DisplayName("getApprovedReviewsByProductId: Lấy danh sách đánh giá đã duyệt theo sản phẩm")
    void getApprovedReviewsByProductId_Success() {
        when(reviewRepository.findByProductIdAndApprovedTrueOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(approvedReview));

        List<ReviewResponse> result = reviewService.getApprovedReviewsByProductId(10L);

        assertEquals(1, result.size());
        assertEquals(101L, result.get(0).getId());
    }

    @Test
    @DisplayName("getApprovedReviewsByProductId: Ném BadRequestException nếu productId null")
    void getApprovedReviewsByProductId_Null_ThrowsException() {
        assertThrows(BadRequestException.class, () -> reviewService.getApprovedReviewsByProductId(null));
    }

    @Test
    @DisplayName("createReview: Thành công khi đã mua hàng và đơn hàng DELIVERED, tạo review với approved=false")
    void createReview_Success() {
        ReviewCreateRequest request = ReviewCreateRequest.builder()
                .productId(10L)
                .rating(5)
                .comment("Tuyệt phẩm!")
                .reviewImageUrl("https://image.com/my-box.png")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(productRepository.findById(10L)).thenReturn(Optional.of(sampleProduct));
        when(reviewRepository.existsByUserIdAndProductId(1L, 10L)).thenReturn(false);
        when(orderItemRepository.hasUserPurchasedProductDelivered(1L, 10L)).thenReturn(true);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(500L);
            return r;
        });

        ReviewResponse response = reviewService.createReview(1L, request);

        assertNotNull(response);
        assertEquals(500L, response.getId());
        assertEquals(5, response.getRating());
        assertEquals("Tuyệt phẩm!", response.getComment());
        assertTrue(response.getApproved()); // Hậu kiểm: Khách hàng mua hàng thành công được duyệt ngay lập tức
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("createReview: Ném BadRequestException khi chưa mua hàng hoặc đơn hàng chưa DELIVERED")
    void createReview_NotDeliveredOrNonOwner_ThrowsBadRequestException() {
        ReviewCreateRequest request = ReviewCreateRequest.builder()
                .productId(10L)
                .rating(5)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(productRepository.findById(10L)).thenReturn(Optional.of(sampleProduct));
        when(reviewRepository.existsByUserIdAndProductId(1L, 10L)).thenReturn(false);
        when(orderItemRepository.hasUserPurchasedProductDelivered(1L, 10L)).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> reviewService.createReview(1L, request));
        assertTrue(ex.getMessage().contains("chỉ có thể đánh giá những sản phẩm đã mua và nhận hàng thành công"));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("createReview: Ném BadRequestException khi đã từng gửi đánh giá (chống duplicate review)")
    void createReview_Duplicate_ThrowsBadRequestException() {
        ReviewCreateRequest request = ReviewCreateRequest.builder()
                .productId(10L)
                .rating(5)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(productRepository.findById(10L)).thenReturn(Optional.of(sampleProduct));
        when(reviewRepository.existsByUserIdAndProductId(1L, 10L)).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> reviewService.createReview(1L, request));
        assertTrue(ex.getMessage().contains("đã gửi đánh giá cho sản phẩm này rồi"));
        verify(orderItemRepository, never()).hasUserPurchasedProductDelivered(any(), any());
    }

    @Test
    @DisplayName("createReview: Ném BadRequestException khi số sao không hợp lệ (<1 hoặc >5)")
    void createReview_InvalidRating_ThrowsBadRequestException() {
        ReviewCreateRequest badLow = ReviewCreateRequest.builder().productId(10L).rating(0).build();
        assertThrows(BadRequestException.class, () -> reviewService.createReview(1L, badLow));

        ReviewCreateRequest badHigh = ReviewCreateRequest.builder().productId(10L).rating(6).build();
        assertThrows(BadRequestException.class, () -> reviewService.createReview(1L, badHigh));
    }

    @Test
    @DisplayName("createReview: Rating null sẽ mặc định là 5 sao theo UX Collector của POP MART")
    void createReview_NullRating_DefaultsToFive() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(productRepository.findById(10L)).thenReturn(Optional.of(sampleProduct));
        when(reviewRepository.existsByUserIdAndProductId(1L, 10L)).thenReturn(false);
        when(orderItemRepository.hasUserPurchasedProductDelivered(1L, 10L)).thenReturn(true);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review r = invocation.getArgument(0);
            r.setId(999L);
            return r;
        });

        ReviewCreateRequest nullRating = ReviewCreateRequest.builder().productId(10L).rating(null).comment("Đẹp xuất sắc").build();
        ReviewResponse res = reviewService.createReview(1L, nullRating);

        assertNotNull(res);
        assertEquals(5, res.getRating());
        assertEquals("Đẹp xuất sắc", res.getComment());
        assertTrue(res.getApproved());
    }

    @Test
    @DisplayName("createReview: Khách hàng mua hàng thành công sẽ được duyệt ngay (approved=true) theo cơ chế hậu kiểm")
    void createReview_PostModeration_ApprovedIsTrue() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(productRepository.findById(10L)).thenReturn(Optional.of(sampleProduct));
        when(reviewRepository.existsByUserIdAndProductId(1L, 10L)).thenReturn(false);
        when(orderItemRepository.hasUserPurchasedProductDelivered(1L, 10L)).thenReturn(true);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewCreateRequest req = ReviewCreateRequest.builder().productId(10L).comment("Hàng chuẩn đẹp").build();
        ReviewResponse res = reviewService.createReview(1L, req);

        assertNotNull(res);
        assertTrue(res.getApproved(), "Hậu kiểm: Đánh giá phải được duyệt ngay (approved = true) để hiển thị công khai");
    }

    @Test
    @DisplayName("createReview: Ném BadRequestException khi userId hoặc request null")
    void createReview_NullInputs_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> reviewService.createReview(null, ReviewCreateRequest.builder().build()));
        assertThrows(BadRequestException.class, () -> reviewService.createReview(1L, null));
    }

    @Test
    @DisplayName("isUserEligibleToReview: Trả về true khi chưa review và đã nhận hàng DELIVERED")
    void isUserEligibleToReview_True() {
        when(reviewRepository.existsByUserIdAndProductId(1L, 10L)).thenReturn(false);
        when(orderItemRepository.hasUserPurchasedProductDelivered(1L, 10L)).thenReturn(true);

        assertTrue(reviewService.isUserEligibleToReview(1L, 10L));
    }

    @Test
    @DisplayName("isUserEligibleToReview: Trả về false khi đã review")
    void isUserEligibleToReview_FalseWhenAlreadyReviewed() {
        when(reviewRepository.existsByUserIdAndProductId(1L, 10L)).thenReturn(true);

        assertFalse(reviewService.isUserEligibleToReview(1L, 10L));
    }

    @Test
    @DisplayName("isUserEligibleToReview: Trả về false khi chưa nhận hàng DELIVERED")
    void isUserEligibleToReview_FalseWhenNotDelivered() {
        when(reviewRepository.existsByUserIdAndProductId(1L, 10L)).thenReturn(false);
        when(orderItemRepository.hasUserPurchasedProductDelivered(1L, 10L)).thenReturn(false);

        assertFalse(reviewService.isUserEligibleToReview(1L, 10L));
    }

    @Test
    @DisplayName("getUserReviewForProduct: Lấy đánh giá của người dùng cho sản phẩm")
    void getUserReviewForProduct_Success() {
        when(reviewRepository.findByUserIdAndProductId(1L, 10L)).thenReturn(Optional.of(pendingReview));

        ReviewResponse res = reviewService.getUserReviewForProduct(1L, 10L);

        assertNotNull(res);
        assertEquals(100L, res.getId());
    }

    @Test
    @DisplayName("getReviewsByUserId: Lấy lịch sử đánh giá của người dùng")
    void getReviewsByUserId_Success() {
        when(reviewRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(pendingReview, approvedReview));

        List<ReviewResponse> list = reviewService.getReviewsByUserId(1L);

        assertEquals(2, list.size());
    }

    @Test
    @DisplayName("createReview: URL ảnh hợp lệ (http:// và https://) -> Chấp nhận lưu")
    void createReview_ValidHttpAndHttpsUrls_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(productRepository.findById(10L)).thenReturn(Optional.of(sampleProduct));
        when(orderItemRepository.hasUserPurchasedProductDelivered(1L, 10L)).thenReturn(true);
        when(reviewRepository.existsByUserIdAndProductId(1L, 10L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        // Test https://
        ReviewCreateRequest httpsReq = ReviewCreateRequest.builder()
                .productId(10L).rating(5).reviewImageUrl("https://cdn.popworld.com/unbox.jpg").build();
        ReviewResponse res1 = reviewService.createReview(1L, httpsReq);
        assertEquals("https://cdn.popworld.com/unbox.jpg", res1.getReviewImageUrl());

        // Test http://
        ReviewCreateRequest httpReq = ReviewCreateRequest.builder()
                .productId(10L).rating(4).reviewImageUrl("http://res.cloudinary.com/box.png").build();
        ReviewResponse res2 = reviewService.createReview(1L, httpReq);
        assertEquals("http://res.cloudinary.com/box.png", res2.getReviewImageUrl());

        // Test null / blank -> chuẩn hóa thành null
        ReviewCreateRequest blankReq = ReviewCreateRequest.builder()
                .productId(10L).rating(5).reviewImageUrl("   ").build();
        ReviewResponse res3 = reviewService.createReview(1L, blankReq);
        assertNull(res3.getReviewImageUrl());
    }

    @Test
    @DisplayName("createReview: Ngăn chặn XSS qua javascript: URL scheme")
    void createReview_JavascriptUrl_ThrowsBadRequestException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(productRepository.findById(10L)).thenReturn(Optional.of(sampleProduct));
        when(orderItemRepository.hasUserPurchasedProductDelivered(1L, 10L)).thenReturn(true);
        when(reviewRepository.existsByUserIdAndProductId(1L, 10L)).thenReturn(false);

        ReviewCreateRequest jsReq = ReviewCreateRequest.builder()
                .productId(10L).rating(5).reviewImageUrl("javascript:alert(document.cookie)").build();
        BadRequestException ex1 = assertThrows(BadRequestException.class, () -> reviewService.createReview(1L, jsReq));
        assertTrue(ex1.getMessage().contains("không an toàn") || ex1.getMessage().contains("không hợp lệ"));

        ReviewCreateRequest upperJsReq = ReviewCreateRequest.builder()
                .productId(10L).rating(5).reviewImageUrl("JAVASCRIPT:/*--></title></style>*/<script>alert(1)</script>").build();
        assertThrows(BadRequestException.class, () -> reviewService.createReview(1L, upperJsReq));
    }

    @Test
    @DisplayName("createReview: Ngăn chặn các scheme nguy hiểm khác (data:, vbscript:, file:, protocol-relative)")
    void createReview_DangerousSchemes_ThrowsBadRequestException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(productRepository.findById(10L)).thenReturn(Optional.of(sampleProduct));
        when(orderItemRepository.hasUserPurchasedProductDelivered(1L, 10L)).thenReturn(true);
        when(reviewRepository.existsByUserIdAndProductId(1L, 10L)).thenReturn(false);

        // data:
        ReviewCreateRequest dataReq = ReviewCreateRequest.builder()
                .productId(10L).rating(5).reviewImageUrl("data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg==").build();
        assertThrows(BadRequestException.class, () -> reviewService.createReview(1L, dataReq));

        // vbscript:
        ReviewCreateRequest vbReq = ReviewCreateRequest.builder()
                .productId(10L).rating(5).reviewImageUrl("vbscript:msgbox(1)").build();
        assertThrows(BadRequestException.class, () -> reviewService.createReview(1L, vbReq));

        // file:
        ReviewCreateRequest fileReq = ReviewCreateRequest.builder()
                .productId(10L).rating(5).reviewImageUrl("file:///etc/passwd").build();
        assertThrows(BadRequestException.class, () -> reviewService.createReview(1L, fileReq));

        // Protocol-relative (//evil.com)
        ReviewCreateRequest protoReq = ReviewCreateRequest.builder()
                .productId(10L).rating(5).reviewImageUrl("//evil.com/xss.jpg").build();
        assertThrows(BadRequestException.class, () -> reviewService.createReview(1L, protoReq));

        // Missing host (https://)
        ReviewCreateRequest noHostReq = ReviewCreateRequest.builder()
                .productId(10L).rating(5).reviewImageUrl("https://").build();
        assertThrows(BadRequestException.class, () -> reviewService.createReview(1L, noHostReq));
    }
}
