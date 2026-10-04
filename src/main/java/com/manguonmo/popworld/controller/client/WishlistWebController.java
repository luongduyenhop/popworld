package com.manguonmo.popworld.controller.client;

import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.service.UserService;
import com.manguonmo.popworld.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/wishlist")
@RequiredArgsConstructor
public class WishlistWebController {

    private final WishlistService wishlistService;
    private final UserService userService;

    @GetMapping
    public String showWishlistPage(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login?continue=/wishlist";
        }

        User user = userService.getUserByEmail(principal.getName());
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            return "redirect:/login";
        }

        List<Product> products = wishlistService.getWishlistProducts(user.getId());
        model.addAttribute("products", products);
        model.addAttribute("wishlistCount", products.size());
        model.addAttribute("pageTitle", "Danh Sách Yêu Thích - PopWorld Art Toy");

        return "wishlist";
    }

    @PostMapping("/remove/{productId}")
    public String removeFromWishlist(
            @PathVariable Long productId,
            RedirectAttributes redirectAttributes,
            Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        User user = userService.getUserByEmail(principal.getName());
        if (user != null) {
            wishlistService.removeFromWishlist(user.getId(), productId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi danh sách yêu thích.");
        }

        return "redirect:/wishlist";
    }
}
