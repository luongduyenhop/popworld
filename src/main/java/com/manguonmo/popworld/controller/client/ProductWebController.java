package com.manguonmo.popworld.controller.client;

import com.manguonmo.popworld.entity.Category;
import com.manguonmo.popworld.entity.CharacterIp;
import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.service.CartService;
import com.manguonmo.popworld.service.CategoryService;
import com.manguonmo.popworld.service.CharacterIpService;
import com.manguonmo.popworld.service.ProductService;
import com.manguonmo.popworld.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Controller
public class ProductWebController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final CharacterIpService characterIpService;
    private final CartService cartService;
    private final UserService userService;

    public ProductWebController(ProductService productService,
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

    private void addCommonAttributes(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("characterIps", characterIpService.getAllCharacterIps());
        int cartCount = 0;
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                User user = userService.getUserByEmail(auth.getName());
                if (user != null) {
                    cartCount = cartService.getCartCount(user.getId());
                }
            }
        } catch (Exception ignored) {
        }
        model.addAttribute("cartCount", cartCount);
    }

    @GetMapping("/products")
    public String allProducts(Model model) {
        addCommonAttributes(model);
        List<Product> products = productService.getAllActiveProducts();
        model.addAttribute("products", products);
        model.addAttribute("currentCategory", null);
        model.addAttribute("pageTitle", "Tất Cả Sản Phẩm POP MART");
        model.addAttribute("pageDescription", "Khám phá toàn bộ bộ sưu tập Art Toys & Blind Box chính hãng");
        return "product-list";
    }

    @GetMapping("/products/{slug}")
    public String productDetail(@PathVariable String slug, Model model) {
        addCommonAttributes(model);
        Optional<Product> productOpt = productService.getProductBySlug(slug);
        if (productOpt.isEmpty()) {
            return "redirect:/";
        }

        Product product = productOpt.get();
        model.addAttribute("product", product);

        if (product.getCategory() != null) {
            List<Product> related = productService.getProductsByCategorySlug(product.getCategory().getSlug());
            model.addAttribute("relatedProducts", related.stream()
                    .filter(p -> !p.getId().equals(product.getId()))
                    .limit(4)
                    .toList());
        } else {
            model.addAttribute("relatedProducts", Collections.emptyList());
        }

        return "product-detail";
    }

    @GetMapping("/categories/{slug}")
    public String productsByCategory(@PathVariable String slug, Model model) {
        addCommonAttributes(model);
        Optional<Category> categoryOpt = categoryService.getCategoryBySlug(slug);
        List<Product> products = productService.getProductsByCategorySlug(slug);

        model.addAttribute("products", products);
        model.addAttribute("currentCategory", categoryOpt.orElse(null));
        model.addAttribute("pageTitle", categoryOpt.map(Category::getName).orElse("Danh Mục"));
        model.addAttribute("pageDescription", categoryOpt.map(Category::getDescription).orElse("Khám phá các sản phẩm"));

        return "product-list";
    }

    @GetMapping("/characters/{id}")
    public String productsByCharacter(@PathVariable Long id, Model model) {
        addCommonAttributes(model);
        Optional<CharacterIp> charOpt = characterIpService.getCharacterIpById(id);
        List<Product> products = productService.getProductsByCharacterIp(id);

        model.addAttribute("products", products);
        model.addAttribute("currentCharacter", charOpt.orElse(null));
        model.addAttribute("pageTitle", charOpt.map(c -> "Nhân Vật IP: " + c.getName()).orElse("Nhân Vật IP"));
        model.addAttribute("pageDescription", charOpt.map(CharacterIp::getDescription).orElse(""));

        return "product-list";
    }

    @GetMapping("/search")
    public String searchProducts(@RequestParam(required = false) String keyword, Model model) {
        addCommonAttributes(model);
        List<Product> products = productService.searchProducts(keyword);

        model.addAttribute("products", products);
        model.addAttribute("keyword", keyword);
        model.addAttribute("pageTitle", "Kết quả tìm kiếm cho: \"" + (keyword != null ? keyword : "") + "\"");
        model.addAttribute("pageDescription", "Tìm thấy " + products.size() + " sản phẩm phù hợp");

        return "product-list";
    }
}
