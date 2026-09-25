package com.manguonmo.popworld.controller.api;

import com.manguonmo.popworld.dto.response.ApiResponse;
import com.manguonmo.popworld.dto.response.CartItemResponse;
import com.manguonmo.popworld.entity.CartItem;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.mapper.CartMapper;
import com.manguonmo.popworld.service.CartService;
import com.manguonmo.popworld.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * REST API Controller chuẩn hóa cho Giỏ hàng (Cart)
 */
@RestController
@RequestMapping("/api/cart")
public class CartApiController {

    private final CartService cartService;
    private final CartMapper cartMapper;
    private final UserService userService;

    public CartApiController(CartService cartService, CartMapper cartMapper, UserService userService) {
        this.cartService = cartService;
        this.cartMapper = cartMapper;
        this.userService = userService;
    }

    private User getAuthenticatedUser(Principal principal) {
        if (principal == null) {
            throw new BadRequestException("Vui lòng đăng nhập để thao tác giỏ hàng.");
        }
        User user = userService.getUserByEmail(principal.getName());
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            throw new BadRequestException("Tài khoản không hợp lệ hoặc đã bị vô hiệu hóa.");
        }
        return user;
    }

    /**
     * Lấy danh sách sản phẩm trong giỏ của người dùng hiện tại
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CartItemResponse>>> getCartItems(Principal principal) {
        User user = getAuthenticatedUser(principal);
        List<CartItem> cartItems = cartService.getCartItems(user.getId());
        List<CartItemResponse> responseList = cartMapper.toResponseList(cartItems);

        return ResponseEntity.ok(ApiResponse.success("Lấy giỏ hàng thành công", responseList));
    }

    /**
     * Lấy tóm tắt giỏ hàng (tổng tiền thanh toán được chọn, tổng số lượng món, danh sách items)
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCartSummary(Principal principal) {
        User user = getAuthenticatedUser(principal);
        List<CartItem> cartItems = cartService.getCartItems(user.getId());
        List<CartItemResponse> responseList = cartMapper.toResponseList(cartItems);
        BigDecimal totalAmount = cartService.calculateSelectedTotal(user.getId());
        int cartCount = cartService.getCartCount(user.getId());

        Map<String, Object> summary = Map.of(
                "cartCount", cartCount,
                "totalAmount", totalAmount,
                "items", responseList
        );

        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin tóm tắt giỏ hàng thành công", summary));
    }

    /**
     * Thêm sản phẩm vào giỏ hàng qua REST API
     */
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartItemResponse>> addToCart(@RequestParam Long productId,
                                                                   @RequestParam(defaultValue = "SINGLE_BOX") String purchaseType,
                                                                   @RequestParam(defaultValue = "1") int quantity,
                                                                   Principal principal) {
        User user = getAuthenticatedUser(principal);
        CartItem item = cartService.addToCart(user.getId(), productId, purchaseType, quantity);
        CartItemResponse response = cartMapper.toResponse(item);

        return ResponseEntity.ok(ApiResponse.success("Thêm vào giỏ hàng thành công", response));
    }

    /**
     * Cập nhật số lượng món hàng
     */
    @PatchMapping("/items/{cartItemId}/quantity")
    public ResponseEntity<ApiResponse<Void>> updateQuantity(@PathVariable Long cartItemId,
                                                            @RequestParam int quantity,
                                                            Principal principal) {
        User user = getAuthenticatedUser(principal);
        cartService.updateQuantity(user.getId(), cartItemId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật số lượng thành công", null));
    }

    /**
     * Chọn hoặc bỏ chọn sản phẩm để thanh toán
     */
    @PatchMapping("/items/{cartItemId}/selection")
    public ResponseEntity<ApiResponse<Void>> updateSelection(@PathVariable Long cartItemId,
                                                             @RequestParam boolean isSelected,
                                                             Principal principal) {
        User user = getAuthenticatedUser(principal);
        cartService.updateSelection(user.getId(), cartItemId, isSelected);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái chọn thành công", null));
    }

    /**
     * Xóa sản phẩm khỏi giỏ hàng
     */
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<Void>> removeItem(@PathVariable Long cartItemId,
                                                        Principal principal) {
        User user = getAuthenticatedUser(principal);
        cartService.removeFromCart(user.getId(), cartItemId);
        return ResponseEntity.ok(ApiResponse.success("Xóa sản phẩm khỏi giỏ hàng thành công", null));
    }
}
