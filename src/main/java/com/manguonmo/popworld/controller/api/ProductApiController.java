package com.manguonmo.popworld.controller.api;

import com.manguonmo.popworld.dto.response.ApiResponse;
import com.manguonmo.popworld.dto.response.ProductResponse;
import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.mapper.ProductMapper;
import com.manguonmo.popworld.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API Controller chuẩn hóa cho Sản phẩm (Product)
 */
@RestController
@RequestMapping("/api/products")
public class ProductApiController {

    private final ProductService productService;
    private final ProductMapper productMapper;

    public ProductApiController(ProductService productService, ProductMapper productMapper) {
        this.productService = productService;
        this.productMapper = productMapper;
    }

    /**
     * Lấy toàn bộ sản phẩm đang mở bán
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts() {
        List<Product> products = productService.getAllActiveProducts();
        List<ProductResponse> responseList = productMapper.toResponseList(products);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm thành công", responseList));
    }

    /**
     * Lấy chi tiết sản phẩm theo SEO slug
     */
    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductBySlug(@PathVariable String slug) {
        Product product = productService.getProductBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với slug: " + slug));

        ProductResponse response = productMapper.toResponse(product);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết sản phẩm thành công", response));
    }

    /**
     * Lấy danh sách sản phẩm nổi bật (Featured / Hot)
     */
    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getFeaturedProducts() {
        List<Product> products = productService.getFeaturedProducts();
        List<ProductResponse> responseList = productMapper.toResponseList(products);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm nổi bật thành công", responseList));
    }

    /**
     * Lấy danh sách hàng mới về (New Releases)
     */
    @GetMapping("/new-releases")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getNewReleases() {
        List<Product> products = productService.getNewReleases();
        List<ProductResponse> responseList = productMapper.toResponseList(products);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm mới thành công", responseList));
    }

    /**
     * Lọc sản phẩm theo danh mục
     */
    @GetMapping("/category/{categorySlug}")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProductsByCategory(@PathVariable String categorySlug) {
        List<Product> products = productService.getProductsByCategorySlug(categorySlug);
        List<ProductResponse> responseList = productMapper.toResponseList(products);
        return ResponseEntity.ok(ApiResponse.success("Lấy sản phẩm theo danh mục thành công", responseList));
    }

    /**
     * Tìm kiếm sản phẩm theo từ khóa
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchProducts(@RequestParam String keyword) {
        List<Product> products = productService.searchProducts(keyword);
        List<ProductResponse> responseList = productMapper.toResponseList(products);
        return ResponseEntity.ok(ApiResponse.success("Tìm kiếm sản phẩm thành công", responseList));
    }
}
