package com.manguonmo.popworld.controller.api;

import com.manguonmo.popworld.dto.response.ApiResponse;
import com.manguonmo.popworld.dto.response.ProductResponse;
import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.mapper.ProductMapper;
import com.manguonmo.popworld.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProductApiControllerTest {

    @Mock
    private ProductService productService;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductApiController productApiController;

    @Test
    @DisplayName("getAllProducts trả về danh sách sản phẩm active")
    public void getAllProducts_shouldReturnList() {
        Product p = Product.builder().id(1L).name("P1").build();
        ProductResponse pr = ProductResponse.builder().id(1L).name("P1").build();

        when(productService.getAllActiveProducts()).thenReturn(List.of(p));
        when(productMapper.toResponseList(List.of(p))).thenReturn(List.of(pr));

        ResponseEntity<ApiResponse<List<ProductResponse>>> response = productApiController.getAllProducts();

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    @DisplayName("getProductBySlug trả về 200 khi tìm thấy sản phẩm")
    public void getProductBySlug_shouldReturnProduct_whenFound() {
        Product p = Product.builder().id(2L).slug("hirono").build();
        ProductResponse pr = ProductResponse.builder().id(2L).slug("hirono").build();

        when(productService.getProductBySlug("hirono")).thenReturn(Optional.of(p));
        when(productMapper.toResponse(p)).thenReturn(pr);

        ResponseEntity<ApiResponse<ProductResponse>> response = productApiController.getProductBySlug("hirono");

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("hirono", response.getBody().getData().getSlug());
    }

    @Test
    @DisplayName("getProductBySlug ném ResourceNotFoundException khi không thấy slug")
    public void getProductBySlug_shouldThrowException_whenNotFound() {
        when(productService.getProductBySlug("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            productApiController.getProductBySlug("unknown");
        });
    }

    @Test
    @DisplayName("getFeaturedProducts trả về danh sách sản phẩm nổi bật")
    public void getFeaturedProducts_shouldReturnList() {
        Product p = Product.builder().id(3L).build();
        ProductResponse pr = ProductResponse.builder().id(3L).build();

        when(productService.getFeaturedProducts()).thenReturn(List.of(p));
        when(productMapper.toResponseList(List.of(p))).thenReturn(List.of(pr));

        ResponseEntity<ApiResponse<List<ProductResponse>>> response = productApiController.getFeaturedProducts();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
    }
}
