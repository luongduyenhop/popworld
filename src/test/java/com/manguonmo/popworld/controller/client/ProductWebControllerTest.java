package com.manguonmo.popworld.controller.client;

import com.manguonmo.popworld.entity.Category;
import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.service.CartService;
import com.manguonmo.popworld.service.CategoryService;
import com.manguonmo.popworld.service.CharacterIpService;
import com.manguonmo.popworld.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductWebController.class)
class ProductWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private CharacterIpService characterIpService;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private com.manguonmo.popworld.service.UserService userService;

    @Test
    @DisplayName("Truy cập trang chi tiết sản phẩm /products/{slug} thành công")
    void test_ProductDetail_Success() throws Exception {
        Category cat = Category.builder().id(1L).name("Blind Box").slug("blind-box").build();
        Product product = Product.builder()
                .id(1L)
                .name("Labubu Fall in Wild")
                .slug("labubu-fall-in-wild")
                .singlePrice(new BigDecimal("350000"))
                .category(cat)
                .build();

        when(productService.getProductBySlug("labubu-fall-in-wild")).thenReturn(Optional.of(product));
        when(productService.getProductsByCategorySlug("blind-box")).thenReturn(List.of(product));

        mockMvc.perform(get("/products/labubu-fall-in-wild"))
                .andExpect(status().isOk())
                .andExpect(view().name("product-detail"))
                .andExpect(model().attributeExists("product"))
                .andExpect(model().attributeExists("relatedProducts"));
    }

    @Test
    @DisplayName("Truy cập trang danh mục /categories/{slug} thành công")
    void test_ProductsByCategory_Success() throws Exception {
        Category cat = Category.builder().id(1L).name("Blind Box").slug("blind-box").build();
        when(categoryService.getCategoryBySlug("blind-box")).thenReturn(Optional.of(cat));
        when(productService.getProductsByCategorySlug("blind-box")).thenReturn(List.of());

        mockMvc.perform(get("/categories/blind-box"))
                .andExpect(status().isOk())
                .andExpect(view().name("product-list"))
                .andExpect(model().attributeExists("products"))
                .andExpect(model().attributeExists("currentCategory"));
    }
}
