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
     * Lấy toàn bộ sản phẩm đang mở bán (overload tương thích ngược cho unit test)
     */
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts() {
        return getAllProducts(null, null);
    }

    /**
     * Lấy danh sách sản phẩm đang mở bán (hỗ trợ phân trang an toàn)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        List<Product> products = productService.getAllActiveProducts();
        if (page != null && size != null && page >= 0 && size > 0) {
            int safeSize = Math.min(size, 100);
            int fromIndex = Math.min(page * safeSize, products.size());
            int toIndex = Math.min(fromIndex + safeSize, products.size());
            products = products.subList(fromIndex, toIndex);
        }
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
     * Tìm kiếm sản phẩm (overload tương thích ngược cho unit test)
     */
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchProducts(String keyword) {
        return searchProducts(keyword, null);
    }

    /**
     * Tìm kiếm sản phẩm theo từ khóa (giới hạn tối đa 50 kết quả để bảo vệ tài nguyên)
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(value = "limit", required = false, defaultValue = "50") Integer limit) {
        int safeLimit = (limit != null && limit > 0) ? Math.min(limit, 100) : 50;
        List<Product> products = productService.searchProducts(keyword);
        if (products.size() > safeLimit) {
            products = products.subList(0, safeLimit);
        }
        List<ProductResponse> responseList = productMapper.toResponseList(products);
        return ResponseEntity.ok(ApiResponse.success("Tìm kiếm sản phẩm thành công", responseList));
    }
}
