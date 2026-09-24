package com.manguonmo.popworld.controller.client;

import com.manguonmo.popworld.entity.CartItem;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.service.CartService;
import com.manguonmo.popworld.service.CategoryService;
import com.manguonmo.popworld.service.CharacterIpService;
import com.manguonmo.popworld.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/cart")
public class CartWebController {

    private final CartService cartService;
    private final CategoryService categoryService;
    private final CharacterIpService characterIpService;
    private final UserService userService;
    public CartWebController(CartService cartService,
                             CategoryService categoryService,
                             CharacterIpService characterIpService, UserService userService) {
        this.cartService = cartService;
        this.categoryService = categoryService;
        this.characterIpService = characterIpService;
        this.userService = userService;
    }

    private void addCommonAttributes(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("characterIps", characterIpService.getAllCharacterIps());
    }

    @GetMapping
    public String viewCart(Model model, Principal principal) {
        addCommonAttributes(model);
        String userName = principal.getName();
        User user = userService.getUserByEmail(userName);
        List<CartItem> cartItems = cartService.getCartItems(user.getId());
        BigDecimal totalAmount = cartService.calculateSelectedTotal(user.getId());
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0 && !cartItems.isEmpty()) {
            totalAmount = cartItems.stream()
                    .map(item -> {
                        BigDecimal price = "SINGLE_BOX".equalsIgnoreCase(item.getPurchaseType())
                                ? item.getProduct().getSinglePrice()
                                : (item.getProduct().getWholeSetPrice() != null ? item.getProduct().getWholeSetPrice() : item.getProduct().getSinglePrice());
                        return price.multiply(BigDecimal.valueOf(item.getQuantity()));
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        int cartCount = cartService.getCartCount(user.getId());

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("cartCount", cartCount);
        model.addAttribute("user", user);

        return "cart";
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam Long productId,
                            @RequestParam(defaultValue = "SINGLE_BOX") String purchaseType,
                            @RequestParam(defaultValue = "1") int quantity,
                            RedirectAttributes redirectAttributes,
                            Principal principal) {
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
                                 @RequestParam int quantity) {
        cartService.updateQuantity(cartItemId, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/toggle-select")
    public String toggleSelection(@RequestParam Long cartItemId,
                                  @RequestParam(defaultValue = "false") boolean isSelected) {
        cartService.updateSelection(cartItemId, isSelected);
        return "redirect:/cart";
    }

    @PostMapping("/delete/{id}")
    public String deleteCartItem(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        cartService.removeFromCart(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi giỏ hàng.");
        return "redirect:/cart";
    }
}
