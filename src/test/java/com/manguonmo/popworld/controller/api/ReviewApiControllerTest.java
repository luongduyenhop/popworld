package com.manguonmo.popworld.controller.api;

import com.manguonmo.popworld.dto.request.ReviewCreateRequest;
import com.manguonmo.popworld.dto.response.ApiResponse;
import com.manguonmo.popworld.dto.response.ReviewResponse;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.service.ReviewService;
import com.manguonmo.popworld.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewApiControllerTest {

    @Mock
    private ReviewService reviewService;

    @Mock
    private UserService userService;

    @Mock
    private Principal principal;

    @InjectMocks
    private ReviewApiController controller;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder().id(1L).email("user@test.com").enabled(true).build();
    }

    @Test
    @DisplayName("submitReview: Chưa đăng nhập -> Trả về 401 UNAUTHORIZED")
    void submitReview_Unauthenticated_ReturnsUnauthorized() {
        ReviewCreateRequest request = ReviewCreateRequest.builder().productId(10L).rating(5).build();

        ResponseEntity<ApiResponse<ReviewResponse>> response = controller.submitReview(request, null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(reviewService, never()).createReview(any(), any());
    }

    @Test
    @DisplayName("submitReview: Hợp lệ -> Trả về 201 CREATED")
    void submitReview_Valid_ReturnsCreated() {
        ReviewCreateRequest request = ReviewCreateRequest.builder().productId(10L).rating(5).comment("Good").build();

        when(principal.getName()).thenReturn("user@test.com");
        when(userService.getUserByEmail("user@test.com")).thenReturn(sampleUser);

        ReviewResponse reviewResponse = ReviewResponse.builder().id(100L).approved(false).build();
        when(reviewService.createReview(1L, request)).thenReturn(reviewResponse);

        ResponseEntity<ApiResponse<ReviewResponse>> response = controller.submitReview(request, principal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(100L, response.getBody().getData().getId());
        verify(reviewService).createReview(1L, request);
    }

    @Test
    @DisplayName("getApprovedReviewsByProduct: Trả về 200 OK với danh sách đánh giá đã duyệt")
    void getApprovedReviewsByProduct_ReturnsOk() {
        when(reviewService.getApprovedReviewsByProductId(10L)).thenReturn(List.of(
                ReviewResponse.builder().id(100L).approved(true).build()
        ));

        ResponseEntity<ApiResponse<List<ReviewResponse>>> response = controller.getApprovedReviewsByProduct(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getData().size());
    }
}
