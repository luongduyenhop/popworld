package com.manguonmo.popworld.controller.admin;

import com.manguonmo.popworld.dto.request.ProductCreateRequest;
import com.manguonmo.popworld.dto.request.ProductUpdateRequest;
import com.manguonmo.popworld.dto.response.ProductStatsResponse;
import com.manguonmo.popworld.entity.Category;
import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.repository.SeriesRepository;
import com.manguonmo.popworld.service.CategoryService;
import com.manguonmo.popworld.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductWebController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final SeriesRepository seriesRepository;

    private void addCommonFormData(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("seriesList", seriesRepository.findAllByOrderByReleaseDateDesc());
        model.addAttribute("activeNav", "products");
    }

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

    @GetMapping("/new")
    public String newProductForm(Model model) {
        addCommonFormData(model);
        model.addAttribute("isEdit", false);
        model.addAttribute("productForm", ProductCreateRequest.builder()
                .isFeatured(false)
                .isNewRelease(true)
                .active(true)
                .stockQuantity(0)
                .build());
        return "admin/product-form";
    }

    @PostMapping
    public String createProduct(@Valid @ModelAttribute("productForm") ProductCreateRequest request,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addCommonFormData(model);
            model.addAttribute("isEdit", false);
            return "admin/product-form";
        }
        try {
            Product product = productService.createProduct(request);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm sản phẩm \"" + product.getName() + "\" thành công!");
            return "redirect:/admin/products";
        } catch (Exception e) {
            log.error("Lỗi khi tạo sản phẩm: {}", e.getMessage(), e);
            addCommonFormData(model);
            model.addAttribute("isEdit", false);
            model.addAttribute("errorMessage", "Không thể tạo sản phẩm: " + e.getMessage());
            return "admin/product-form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editProductForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Product product = productService.getProductById(id);
            addCommonFormData(model);
            model.addAttribute("isEdit", true);
            model.addAttribute("product", product);
            model.addAttribute("productForm", ProductUpdateRequest.builder()
                    .name(product.getName())
                    .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                    .seriesId(product.getSeries() != null ? product.getSeries().getId() : null)
                    .singlePrice(product.getSinglePrice())
                    .wholeSetPrice(product.getWholeSetPrice())
                    .stockQuantity(product.getStockQuantity())
                    .packagingType(product.getPackagingType())
                    .secretRatio(product.getSecretRatio())
                    .material(product.getMaterial())
                    .sizeDimensions(product.getSizeDimensions())
                    .description(product.getDescription())
                    .isFeatured(Boolean.TRUE.equals(product.getIsFeatured()))
                    .isNewRelease(Boolean.TRUE.equals(product.getIsNewRelease()))
                    .active(Boolean.TRUE.equals(product.getActive()))
                    .build());
            return "admin/product-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/products";
        }
    }

    @PostMapping("/{id}")
    public String updateProduct(@PathVariable Long id,
                                @Valid @ModelAttribute("productForm") ProductUpdateRequest request,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            Product product = productService.getProductById(id);
            addCommonFormData(model);
            model.addAttribute("isEdit", true);
            model.addAttribute("product", product);
            return "admin/product-form";
        }
        try {
            Product product = productService.updateProduct(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật sản phẩm \"" + product.getName() + "\" thành công!");
            return "redirect:/admin/products";
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật sản phẩm ID={}: {}", id, e.getMessage(), e);
            Product product = productService.getProductById(id);
            addCommonFormData(model);
            model.addAttribute("isEdit", true);
            model.addAttribute("product", product);
            model.addAttribute("errorMessage", "Không thể cập nhật sản phẩm: " + e.getMessage());
            return "admin/product-form";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Product product = productService.getProductById(id);
            boolean physical = productService.deleteProduct(id);
            if (physical) {
                redirectAttributes.addFlashAttribute("successMessage", "Đã xóa vĩnh viễn sản phẩm \"" + product.getName() + "\" và dọn dẹp ảnh trên Cloudinary.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Sản phẩm \"" + product.getName() + "\" đã phát sinh đơn hàng trong lịch sử. Hệ thống đã chuyển trạng thái sang tạm ẩn (Active: False) để bảo toàn dữ liệu kế toán!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa sản phẩm: " + e.getMessage());
        }
        return "redirect:/admin/products";
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
