package com.manguonmo.popworld.controller.client;

import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.service.CartService;
import com.manguonmo.popworld.service.CategoryService;
import com.manguonmo.popworld.service.CharacterIpService;
import com.manguonmo.popworld.service.UserService;
import com.manguonmo.popworld.service.WishlistService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;
import java.util.Collections;

@ControllerAdvice(basePackages = "com.manguonmo.popworld.controller.client")
public class GlobalNavbarAdvice {

    private final CategoryService categoryService;
    private final CharacterIpService characterIpService;
    private final CartService cartService;
    private final UserService userService;
    private final WishlistService wishlistService;

    public GlobalNavbarAdvice(CategoryService categoryService,
                              CharacterIpService characterIpService,
                              CartService cartService,
                              UserService userService,
                              WishlistService wishlistService) {
        this.categoryService = categoryService;
        this.characterIpService = characterIpService;
        this.cartService = cartService;
        this.userService = userService;
        this.wishlistService = wishlistService;
    }

    @ModelAttribute
    public void populateGlobalNavbarData(Model model, Principal principal) {
        if (!model.containsAttribute("categories")) {
            try {
                model.addAttribute("categories", categoryService.getAllCategories());
            } catch (Exception ignored) {
            }
        }

        if (!model.containsAttribute("characterIps")) {
            try {
                model.addAttribute("characterIps", characterIpService.getAllCharacterIps());
            } catch (Exception ignored) {
            }
        }

        if (principal != null) {
            try {
                User user = userService.getUserByEmail(principal.getName());
                if (user != null) {
                    if (!model.containsAttribute("cartCount")) {
                        model.addAttribute("cartCount", cartService.getCartCount(user.getId()));
                    }
                    if (!model.containsAttribute("wishlistCount")) {
                        model.addAttribute("wishlistCount", wishlistService.getWishlistCount(user.getId()));
                    }
                    if (!model.containsAttribute("wishlistProductIds")) {
                        model.addAttribute("wishlistProductIds", wishlistService.getWishlistProductIds(user.getId()));
                    }
                    String displayName = (user.getFullName() != null && !user.getFullName().isBlank())
                            ? user.getFullName()
                            : "POPMART MEMBER";
                    model.addAttribute("currentUserDisplayName", displayName);
                    model.addAttribute("currentUserPoints", user.getRewardPoints() != null ? user.getRewardPoints() : 0);
                    model.addAttribute("currentUserTier", user.getMembershipTier() != null ? user.getMembershipTier() : "MEMBER");
                    model.addAttribute("currentUser", user);
                }
            } catch (Exception ignored) {
            }
        } else {
            if (!model.containsAttribute("cartCount")) {
                model.addAttribute("cartCount", 0);
            }
            if (!model.containsAttribute("wishlistCount")) {
                model.addAttribute("wishlistCount", 0);
            }
            if (!model.containsAttribute("wishlistProductIds")) {
                model.addAttribute("wishlistProductIds", Collections.emptySet());
            }
        }
    }
}
