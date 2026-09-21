package com.manguonmo.popworld.controller.admin;

import com.manguonmo.popworld.dto.response.ProductStatsResponse;
import com.manguonmo.popworld.entity.Category;
import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.service.CategoryService;
import com.manguonmo.popworld.service.ProductService;
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

    private final ProductService productService;
    private final CategoryService categoryService;

    @GetMapping
    public String listProducts(@RequestParam(value = "categoryId", required = false) Long categoryId,
                               @RequestParam(value = "keyword", required = false) String keyword,
                               Model model) {
        List<Product> products = productService.getAdminProducts(categoryId, keyword);
        List<Category> categories = categoryService.getAllCategories();
        ProductStatsResponse stats = productService.getProductStats();

        model.addAttribute("products", products);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("activeNav", "products");

        model.addAttribute("totalCount", stats.getTotalCount());
        model.addAttribute("activeCount", stats.getActiveCount());
        model.addAttribute("lowStockCount", stats.getLowStockCount());

        return "admin/products";
    }

    @PostMapping("/{id}/stock")
    public String updateStock(@PathVariable Long id,
                              @RequestParam("stockQuantity") Integer stockQuantity,
                              RedirectAttributes redirectAttributes) {
        try {
            Product product = productService.updateStock(id, stockQuantity);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật tồn kho cho " + product.getName() + " thành: " + stockQuantity + " hộp!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Product product = productService.toggleActive(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã " + (Boolean.TRUE.equals(product.getActive()) ? "kích hoạt mở bán" : "tạm ẩn") + " sản phẩm: " + product.getName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }
}
