package com.manguonmo.popworld.controller.admin;

import com.manguonmo.popworld.dto.request.ProductCreateRequest;
import com.manguonmo.popworld.dto.request.ProductUpdateRequest;
import com.manguonmo.popworld.dto.response.ProductStatsResponse;
import com.manguonmo.popworld.entity.Category;
import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.repository.SeriesRepository;
import com.manguonmo.popworld.service.CategoryService;
import com.manguonmo.popworld.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminProductWebControllerTest {

    @Mock
    private ProductService productService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private SeriesRepository seriesRepository;

    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirectAttributes;

    @Mock
    private BindingResult bindingResult;

    @InjectMocks
    private AdminProductWebController controller;

    @Test
    @DisplayName("listProducts: Hiển thị danh sách sản phẩm và thống kê kho")
    void listProducts_ShouldReturnViewWithStats() {
        ProductStatsResponse stats = ProductStatsResponse.builder().totalCount(50L).activeCount(40L).lowStockCount(5L).build();
        when(productService.getAdminProducts(null, null)).thenReturn(Collections.emptyList());
        when(categoryService.getAllCategories()).thenReturn(Collections.emptyList());
        when(productService.getProductStats()).thenReturn(stats);

        String view = controller.listProducts(null, null, model);

        assertEquals("admin/products", view);
        verify(model).addAttribute(eq("totalCount"), eq(50L));
        verify(model).addAttribute(eq("activeCount"), eq(40L));
        verify(model).addAttribute(eq("lowStockCount"), eq(5L));
    }

    @Test
    @DisplayName("newProductForm: Khởi tạo form tạo mới sản phẩm")
    void newProductForm_ShouldReturnFormView() {
        when(categoryService.getAllCategories()).thenReturn(Collections.emptyList());
        when(seriesRepository.findAllByOrderByReleaseDateDesc()).thenReturn(Collections.emptyList());

        String view = controller.newProductForm(model);

        assertEquals("admin/product-form", view);
        verify(model).addAttribute(eq("isEdit"), eq(false));
        verify(model).addAttribute(eq("productForm"), any(ProductCreateRequest.class));
    }

    @Test
    @DisplayName("createProduct: Dữ liệu hợp lệ -> Gọi service và redirect")
    void createProduct_Valid_Redirects() {
        ProductCreateRequest request = ProductCreateRequest.builder().name("Labubu").build();
        Product saved = Product.builder().id(10L).name("Labubu").build();
        when(bindingResult.hasErrors()).thenReturn(false);
        when(productService.createProduct(request)).thenReturn(saved);

        String view = controller.createProduct(request, bindingResult, model, redirectAttributes);

        assertEquals("redirect:/admin/products", view);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), contains("thành công"));
    }

    @Test
    @DisplayName("createProduct: Có lỗi validation -> Trả lại view form")
    void createProduct_ValidationErrors_ReturnsForm() {
        ProductCreateRequest request = ProductCreateRequest.builder().name("").build();
        when(bindingResult.hasErrors()).thenReturn(true);
        when(categoryService.getAllCategories()).thenReturn(Collections.emptyList());
        when(seriesRepository.findAllByOrderByReleaseDateDesc()).thenReturn(Collections.emptyList());

        String view = controller.createProduct(request, bindingResult, model, redirectAttributes);

        assertEquals("admin/product-form", view);
        verify(model).addAttribute(eq("isEdit"), eq(false));
        verify(productService, never()).createProduct(any());
    }

    @Test
    @DisplayName("editProductForm: Sản phẩm tồn tại -> Đổ dữ liệu vào form")
    void editProductForm_Exists_ReturnsFormView() {
        Category cat = Category.builder().id(1L).name("Blind Box").build();
        Product product = Product.builder()
                .id(1L)
                .name("Skullpanda")
                .singlePrice(BigDecimal.valueOf(350000))
                .stockQuantity(10)
                .category(cat)
                .build();
        when(productService.getProductById(1L)).thenReturn(product);
        when(categoryService.getAllCategories()).thenReturn(Collections.emptyList());
        when(seriesRepository.findAllByOrderByReleaseDateDesc()).thenReturn(Collections.emptyList());

        String view = controller.editProductForm(1L, model, redirectAttributes);

        assertEquals("admin/product-form", view);
        verify(model).addAttribute(eq("isEdit"), eq(true));
        verify(model).addAttribute(eq("product"), eq(product));
        verify(model).addAttribute(eq("productForm"), any(ProductUpdateRequest.class));
    }

    @Test
    @DisplayName("updateProduct: Cập nhật thành công -> Redirect về danh sách")
    void updateProduct_Valid_Redirects() {
        ProductUpdateRequest request = ProductUpdateRequest.builder().name("Updated").build();
        Product product = Product.builder().id(1L).name("Updated").build();
        when(bindingResult.hasErrors()).thenReturn(false);
        when(productService.updateProduct(1L, request)).thenReturn(product);

        String view = controller.updateProduct(1L, request, bindingResult, model, redirectAttributes);

        assertEquals("redirect:/admin/products", view);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), contains("thành công"));
    }

    @Test
    @DisplayName("deleteProduct: Xóa vật lý thành công khi chưa có đơn hàng")
    void deleteProduct_PhysicalDelete_Success() {
        Product product = Product.builder().id(1L).name("Toy 1").build();
        when(productService.getProductById(1L)).thenReturn(product);
        when(productService.deleteProduct(1L)).thenReturn(true);

        String view = controller.deleteProduct(1L, redirectAttributes);

        assertEquals("redirect:/admin/products", view);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), contains("xóa vĩnh viễn"));
    }

    @Test
    @DisplayName("deleteProduct: Soft delete khi đã có đơn hàng liên kết")
    void deleteProduct_SoftDelete_Success() {
        Product product = Product.builder().id(1L).name("Ordered Toy").build();
        when(productService.getProductById(1L)).thenReturn(product);
        when(productService.deleteProduct(1L)).thenReturn(false);

        String view = controller.deleteProduct(1L, redirectAttributes);

        assertEquals("redirect:/admin/products", view);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), contains("tạm ẩn"));
    }

    @Test
    @DisplayName("updateStock: Cập nhật tồn kho nhanh thành công")
    void updateStock_Success() {
        Product product = Product.builder().id(1L).name("Toy 1").stockQuantity(50).build();
        when(productService.updateStock(1L, 50)).thenReturn(product);

        String view = controller.updateStock(1L, 50, redirectAttributes);

        assertEquals("redirect:/admin/products", view);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), contains("50"));
    }

    @Test
    @DisplayName("toggleActive: Đảo trạng thái active thành công")
    void toggleActive_Success() {
        Product product = Product.builder().id(1L).name("Toy 1").active(false).build();
        when(productService.toggleActive(1L)).thenReturn(product);

        String view = controller.toggleActive(1L, redirectAttributes);

        assertEquals("redirect:/admin/products", view);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), contains("tạm ẩn"));
    }
}
