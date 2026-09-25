package com.manguonmo.popworld.controller.client;

import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.service.CartService;
import com.manguonmo.popworld.service.CategoryService;
import com.manguonmo.popworld.service.CharacterIpService;
import com.manguonmo.popworld.service.ProductService;
import com.manguonmo.popworld.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/")
public class HomeController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final CharacterIpService characterIpService;
    private final CartService cartService;
    private final UserService userService;

    public HomeController(ProductService productService,
                          CategoryService categoryService,
                          CharacterIpService characterIpService,
                          CartService cartService,
                          UserService userService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.characterIpService = characterIpService;
        this.cartService = cartService;
        this.userService = userService;
    }

    @GetMapping
    public String home(Model model, Principal principal) {
        List<Product> featuredProducts = productService.getFeaturedProducts();
        List<Product> newReleases = productService.getNewReleases();

        model.addAttribute("featuredProducts", featuredProducts);
        model.addAttribute("newReleases", newReleases);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("characterIps", characterIpService.getAllCharacterIps());

        int cartCount = 0;
        if (principal != null) {
            try {
                User user = userService.getUserByEmail(principal.getName());
                if (user != null) {
                    cartCount = cartService.getCartCount(user.getId());
                }
            } catch (Exception ignored) {
            }
        }
        model.addAttribute("cartCount", cartCount);

        return "index";
    }
}
