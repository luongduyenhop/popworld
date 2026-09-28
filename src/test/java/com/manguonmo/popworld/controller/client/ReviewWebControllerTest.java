package com.manguonmo.popworld.controller.client;

import com.manguonmo.popworld.dto.request.ReviewCreateRequest;
import com.manguonmo.popworld.dto.response.ReviewResponse;
import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.service.ProductService;
import com.manguonmo.popworld.service.ReviewService;
import com.manguonmo.popworld.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewWebControllerTest {

    @Mock
    private ReviewService reviewService;

    @Mock
    private UserService userService;

    @Mock
    private ProductService productService;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private RedirectAttributes redirectAttributes;

    @Mock
    private Principal principal;

    @InjectMocks
    private ReviewWebController controller;

    private User sampleUser;
    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder().id(1L).email("user@test.com").enabled(true).build();
        sampleProduct = Product.builder().id(10L).name("Hirono Little Mischief").slug("hirono-little-mischief").build();
    }

    @Test
    @DisplayName("submitReview: Chưa đăng nhập (principal null) -> Redirect về /login")
    void submitReview_Unauthenticated_RedirectsToLogin() {
        ReviewCreateRequest request = ReviewCreateRequest.builder().productId(10L).rating(5).build();

        String view = controller.submitReview(request, bindingResult, null, redirectAttributes);

        assertEquals("redirect:/login", view);
        verify(reviewService, never()).createReview(any(), any());
    }

    @Test
    @DisplayName("submitReview: Dữ liệu hợp lệ -> Gửi đánh giá thành công và chuyển hướng về trang sản phẩm")
    void submitReview_Valid_Success() {
        ReviewCreateRequest request = ReviewCreateRequest.builder()
                .productId(10L)
                .rating(5)
                .comment("Hộp rất đẹp")
                .build();

        when(principal.getName()).thenReturn("user@test.com");
        when(userService.getUserByEmail("user@test.com")).thenReturn(sampleUser);
        when(productService.getProductById(10L)).thenReturn(sampleProduct);
        when(bindingResult.hasErrors()).thenReturn(false);

        ReviewResponse response = ReviewResponse.builder().id(100L).approved(false).build();
        when(reviewService.createReview(1L, request)).thenReturn(response);

        String view = controller.submitReview(request, bindingResult, principal, redirectAttributes);

        assertEquals("redirect:/products/hirono-little-mischief#reviews", view);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), contains("chờ ban quản trị kiểm duyệt"));
    }

    @Test
    @DisplayName("submitReview: Lỗi validation (thiếu sao) -> Redirect kèm flash lỗi")
    void submitReview_ValidationErrors_RedirectsWithError() {
        ReviewCreateRequest request = ReviewCreateRequest.builder().productId(10L).build();

        when(principal.getName()).thenReturn("user@test.com");
        when(userService.getUserByEmail("user@test.com")).thenReturn(sampleUser);
        when(productService.getProductById(10L)).thenReturn(sampleProduct);
        when(bindingResult.hasErrors()).thenReturn(true);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(new FieldError("reviewRequest", "rating", "Vui lòng chọn số sao")));

        String view = controller.submitReview(request, bindingResult, principal, redirectAttributes);

        assertEquals("redirect:/products/hirono-little-mischief#reviews", view);
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), contains("Vui lòng chọn số sao"));
        verify(reviewService, never()).createReview(any(), any());
    }

    @Test
    @DisplayName("submitReview: Service ném BadRequestException (chưa mua/duplicate) -> Redirect kèm flash lỗi")
    void submitReview_ServiceBadRequest_RedirectsWithError() {
        ReviewCreateRequest request = ReviewCreateRequest.builder().productId(10L).rating(5).build();

        when(principal.getName()).thenReturn("user@test.com");
        when(userService.getUserByEmail("user@test.com")).thenReturn(sampleUser);
        when(productService.getProductById(10L)).thenReturn(sampleProduct);
        when(bindingResult.hasErrors()).thenReturn(false);
        when(reviewService.createReview(1L, request)).thenThrow(new BadRequestException("Bạn chỉ có thể đánh giá sau khi nhận hàng thành công"));

        String view = controller.submitReview(request, bindingResult, principal, redirectAttributes);

        assertEquals("redirect:/products/hirono-little-mischief#reviews", view);
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), contains("sau khi nhận hàng thành công"));
    }

    @Test
    @DisplayName("submitReview: URL ảnh độc hại (javascript:) bị bắt lỗi validation -> Redirect kèm flash lỗi")
    void submitReview_MaliciousUrl_RedirectsWithError() {
        ReviewCreateRequest request = ReviewCreateRequest.builder()
                .productId(10L).rating(5).reviewImageUrl("javascript:alert(1)").build();

        when(principal.getName()).thenReturn("user@test.com");
        when(userService.getUserByEmail("user@test.com")).thenReturn(sampleUser);
        when(productService.getProductById(10L)).thenReturn(sampleProduct);
        when(bindingResult.hasErrors()).thenReturn(true);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("reviewRequest", "reviewImageUrl", "Đường dẫn ảnh chỉ chấp nhận URL tuyệt đối sử dụng giao thức http:// hoặc https:// hợp lệ!")
        ));

        String view = controller.submitReview(request, bindingResult, principal, redirectAttributes);

        assertEquals("redirect:/products/hirono-little-mischief#reviews", view);
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), contains("chỉ chấp nhận URL"));
        verify(reviewService, never()).createReview(any(), any());
    }
}
