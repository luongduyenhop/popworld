package com.manguonmo.popworld.controller.client;

import com.manguonmo.popworld.entity.CartItem;
import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.service.CartService;
import com.manguonmo.popworld.service.CategoryService;
import com.manguonmo.popworld.service.CharacterIpService;
import com.manguonmo.popworld.service.ProductService;
import com.manguonmo.popworld.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/cart")
public class CartWebController {

    private final CartService cartService;
    private final CategoryService categoryService;
    private final CharacterIpService characterIpService;
    private final UserService userService;
    private final ProductService productService;

    public CartWebController(CartService cartService,
                             CategoryService categoryService,
                             CharacterIpService characterIpService,
                             UserService userService,
                             ProductService productService) {
        this.cartService = cartService;
        this.categoryService = categoryService;
        this.characterIpService = characterIpService;
        this.userService = userService;
        this.productService = productService;
    }

    private void addCommonAttributes(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("characterIps", characterIpService.getAllCharacterIps());
    }

    @GetMapping({"", "/"})
    public String viewCart(Model model, Principal principal) {
        addCommonAttributes(model);
        if (principal == null) {
            return "redirect:/login";
        }
        User user;
        try {
            user = userService.getUserByEmail(principal.getName());
        } catch (Exception e) {
            return "redirect:/login";
        }
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            return "redirect:/login";
        }
        List<CartItem> cartItems = cartService.getCartItems(user.getId());
        BigDecimal totalAmount = cartService.calculateSelectedTotal(user.getId());
        int cartCount = cartService.getCartCount(user.getId());
        long selectedCount = cartItems.stream().filter(item -> Boolean.TRUE.equals(item.getIsSelected())).count();
        boolean allSelected = !cartItems.isEmpty() && selectedCount == cartItems.size();

        BigDecimal freeShippingThreshold = BigDecimal.valueOf(500000);
        boolean isFreeShipping = totalAmount != null && totalAmount.compareTo(freeShippingThreshold) >= 0;
        BigDecimal freeShippingRemaining = isFreeShipping
                ? BigDecimal.ZERO
                : freeShippingThreshold.subtract(totalAmount != null ? totalAmount : BigDecimal.ZERO);

        List<Product> recommendedProducts = productService != null
                ? productService.getFeaturedProducts()
                : Collections.emptyList();

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("cartCount", cartCount);
        model.addAttribute("selectedCount", selectedCount);
        model.addAttribute("allSelected", allSelected);
        model.addAttribute("isFreeShipping", isFreeShipping);
        model.addAttribute("freeShippingRemaining", freeShippingRemaining);
        model.addAttribute("recommendedProducts", recommendedProducts);
        model.addAttribute("user", user);

        return "cart";
    }

    @PostMapping("/select-all")
    public String selectAll(@RequestParam(defaultValue = "true") boolean selectAll,
                            Principal principal,
                            RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }
        try {
            User user = userService.getUserByEmail(principal.getName());
            cartService.selectAll(user.getId(), selectAll);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam Long productId,
                            @RequestParam(defaultValue = "SINGLE_BOX") String purchaseType,
                            @RequestParam(defaultValue = "1") int quantity,
                            RedirectAttributes redirectAttributes,
                            Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        try {
            String username = principal.getName();
            User user = userService.getUserByEmail(username);
            cartService.addToCart(user.getId(), productId, purchaseType, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm vào giỏ hàng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể thêm vào giỏ hàng: " + e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/update")
    public String updateQuantity(@RequestParam Long cartItemId,
                                 @RequestParam int quantity,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }
        try {
            User user = userService.getUserByEmail(principal.getName());
            cartService.updateQuantity(user.getId(), cartItemId, quantity);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/toggle-select")
    public String toggleSelection(@RequestParam Long cartItemId,
                                  @RequestParam(defaultValue = "false") boolean isSelected,
                                  Principal principal,
                                  RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }
        try {
            User user = userService.getUserByEmail(principal.getName());
            cartService.updateSelection(user.getId(), cartItemId, isSelected);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/delete/{id}")
    public String deleteCartItem(@PathVariable Long id,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }
        try {
            User user = userService.getUserByEmail(principal.getName());
            cartService.removeFromCart(user.getId(), id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi giỏ hàng.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/cart";
    }
}
