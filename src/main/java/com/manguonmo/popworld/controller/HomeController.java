package com.manguonmo.popworld.controller;

import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.service.CartService;
import com.manguonmo.popworld.service.CategoryService;
import com.manguonmo.popworld.service.CharacterIpService;
import com.manguonmo.popworld.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/")
public class HomeController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final CharacterIpService characterIpService;
    private final CartService cartService;

    public HomeController(ProductService productService,
                          CategoryService categoryService,
                          CharacterIpService characterIpService,
                          CartService cartService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.characterIpService = characterIpService;
        this.cartService = cartService;
    }

    @GetMapping
    public String home(Model model) {
        List<Product> featuredProducts = productService.getFeaturedProducts();
        List<Product> newReleases = productService.getNewReleases();

        model.addAttribute("featuredProducts", featuredProducts);
        model.addAttribute("newReleases", newReleases);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("characterIps", characterIpService.getAllCharacterIps());

        try {
            Long defaultUserId = cartService.getDefaultUser().getId();
            model.addAttribute("cartCount", cartService.getCartCount(defaultUserId));
        } catch (Exception e) {
            model.addAttribute("cartCount", 0);
        }

        return "index";
    }
}
