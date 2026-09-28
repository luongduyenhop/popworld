package com.manguonmo.popworld.controller.client;

import com.manguonmo.popworld.dto.request.ReviewCreateRequest;
import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.service.ProductService;
import com.manguonmo.popworld.service.ReviewService;
import com.manguonmo.popworld.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Slf4j
@Controller
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewWebController {

    private final ReviewService reviewService;
    private final UserService userService;
    private final ProductService productService;

    @PostMapping
    public String submitReview(@Valid @ModelAttribute("reviewRequest") ReviewCreateRequest request,
                               BindingResult bindingResult,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }

        User user = userService.getUserByEmail(principal.getName());
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            return "redirect:/login";
        }

        String fallbackRedirect = "redirect:/";
        if (request != null && request.getProductId() != null) {
            try {
                Product product = productService.getProductById(request.getProductId());
                if (product != null && product.getSlug() != null) {
                    fallbackRedirect = "redirect:/products/" + product.getSlug() + "#reviews";
                }
            } catch (Exception ignored) {
            }
        }

        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .findFirst()
                    .orElse("Thông tin đánh giá không hợp lệ");
            redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
            return fallbackRedirect;
        }

        try {
            reviewService.createReview(user.getId(), request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cảm ơn bạn! Đánh giá đã được gửi thành công và đang chờ ban quản trị kiểm duyệt trước khi hiển thị.");
        } catch (BadRequestException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi khi gửi đánh giá: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể gửi đánh giá: " + e.getMessage());
        }

        return fallbackRedirect;
    }
}
