package com.manguonmo.popworld.controller.admin;

import com.manguonmo.popworld.entity.Category;
import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.repository.CategoryRepository;
import com.manguonmo.popworld.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductWebController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @GetMapping
    public String listProducts(@RequestParam(value = "categoryId", required = false) Long categoryId,
                               @RequestParam(value = "keyword", required = false) String keyword,
                               Model model) {
        List<Product> products;

        if (keyword != null && !keyword.trim().isEmpty()) {
            products = productRepository.findByNameContainingIgnoreCaseAndActiveTrue(keyword.trim());
        } else if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId).orElse(null);
            if (category != null) {
                products = productRepository.findByCategorySlugAndActiveTrue(category.getSlug());
            } else {
                products = productRepository.findAllByOrderByCreatedAtDesc();
            }
        } else {
            products = productRepository.findAllByOrderByCreatedAtDesc();
        }

        List<Category> categories = categoryRepository.findAll();

        model.addAttribute("products", products);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("activeNav", "products");

        model.addAttribute("totalCount", productRepository.count());
        model.addAttribute("activeCount", productRepository.countByActiveTrue());
        model.addAttribute("lowStockCount", productRepository.countByStockQuantityLessThanEqual(10));

        return "admin/products";
    }

    @PostMapping("/{id}/stock")
    public String updateStock(@PathVariable Long id,
                              @RequestParam("stockQuantity") Integer stockQuantity,
                              RedirectAttributes redirectAttributes) {
        Product product = productRepository.findById(id).orElse(null);
        if (product != null && stockQuantity != null && stockQuantity >= 0) {
            product.setStockQuantity(stockQuantity);
            productRepository.save(product);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật tồn kho cho " + product.getName() + " thành: " + stockQuantity + " hộp!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật tồn kho hợp lệ!");
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Product product = productRepository.findById(id).orElse(null);
        if (product != null) {
            boolean newStatus = !Boolean.TRUE.equals(product.getActive());
            product.setActive(newStatus);
            productRepository.save(product);
            redirectAttributes.addFlashAttribute("successMessage", "Đã " + (newStatus ? "kích hoạt mở bán" : "tạm ẩn") + " sản phẩm: " + product.getName());
        }
        return "redirect:/admin/products";
    }
}
