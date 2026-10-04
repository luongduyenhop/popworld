package com.manguonmo.popworld.controller.api;

import com.manguonmo.popworld.dto.request.WishlistToggleRequest;
import com.manguonmo.popworld.dto.response.ApiResponse;
import com.manguonmo.popworld.dto.response.WishlistToggleResponse;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.service.UserService;
import com.manguonmo.popworld.service.WishlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistApiController {

    private final WishlistService wishlistService;
    private final UserService userService;

    private User getAuthenticatedUser(Principal principal) {
        if (principal == null) {
            throw new BadRequestException("Vui lòng đăng nhập để lưu sản phẩm yêu thích.");
        }
        User user = userService.getUserByEmail(principal.getName());
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            throw new BadRequestException("Tài khoản không hợp lệ hoặc đã bị vô hiệu hóa.");
        }
        return user;
    }

    @PostMapping("/toggle")
    public ResponseEntity<ApiResponse<WishlistToggleResponse>> toggleWishlist(
            @Valid @RequestBody WishlistToggleRequest request,
            Principal principal) {
        User user = getAuthenticatedUser(principal);
        boolean isWishlisted = wishlistService.toggleWishlist(user.getId(), request.getProductId());
        long count = wishlistService.getWishlistCount(user.getId());

        String message = isWishlisted
                ? "Đã thêm sản phẩm vào danh sách yêu thích!"
                : "Đã xóa sản phẩm khỏi danh sách yêu thích!";

        WishlistToggleResponse response = WishlistToggleResponse.builder()
                .wishlisted(isWishlisted)
                .wishlistCount(count)
                .productId(request.getProductId())
                .message(message)
                .build();

        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getWishlistCount(Principal principal) {
        if (principal == null) {
            return ResponseEntity.ok(ApiResponse.success("Success", 0L));
        }
        User user = userService.getUserByEmail(principal.getName());
        long count = user != null ? wishlistService.getWishlistCount(user.getId()) : 0L;
        return ResponseEntity.ok(ApiResponse.success("Success", count));
    }

    @GetMapping("/status/{productId}")
    public ResponseEntity<ApiResponse<Boolean>> isWishlisted(
            @PathVariable Long productId,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.ok(ApiResponse.success("Success", false));
        }
        User user = userService.getUserByEmail(principal.getName());
        boolean status = user != null && wishlistService.isWishlisted(user.getId(), productId);
        return ResponseEntity.ok(ApiResponse.success("Success", status));
    }
}
