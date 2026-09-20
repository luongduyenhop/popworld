package com.manguonmo.popworld.controller.client;


import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
@WebMvcTest(HomeController.class)
public class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private com.manguonmo.popworld.service.CategoryService categoryService;

    @MockitoBean
    private com.manguonmo.popworld.service.CharacterIpService characterIpService;

    @MockitoBean
    private com.manguonmo.popworld.service.CartService cartService;

    @Test
    @DisplayName("Truy cap trang chu GET / phai tra ve HTTP 200, View 'index'")
    void test_HomePage_ShouldReturnIndexViewWithModelAttributes() throws Exception {

        Product p = Product.builder().id(1L).name("Labubu").build();
        when(productService.getFeaturedProducts()).thenReturn(List.of(p));
        when(productService.getNewReleases()).thenReturn(List.of(p));


        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("featuredProducts"))
                .andExpect(model().attributeExists("newReleases"));


    }
}
