package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.request.ProductCreateRequest;
import com.manguonmo.popworld.dto.request.ProductUpdateRequest;
import com.manguonmo.popworld.dto.response.CloudinaryUploadResult;
import com.manguonmo.popworld.dto.response.ProductStatsResponse;
import com.manguonmo.popworld.entity.Category;
import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.entity.ProductImage;
import com.manguonmo.popworld.entity.Series;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.repository.*;
import com.manguonmo.popworld.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SeriesRepository seriesRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    @DisplayName("Test lấy danh sách sản phẩm nổi bật thành công")
    void test_GetFeaturedProducts_Success() {
        Product p1 = Product.builder().id(1L).name("Labubu Fall in Wild").isFeatured(true).build();
        Product p2 = Product.builder().id(2L).name("Molly Space").isFeatured(true).build();
        List<Product> mockList = List.of(p1, p2);

        when(productRepository.findByIsFeaturedTrueAndActiveTrue()).thenReturn(mockList);

        List<Product> actualResult = productService.getFeaturedProducts();

        assertNotNull(actualResult);
        assertEquals(2, actualResult.size());
        assertEquals("Labubu Fall in Wild", actualResult.get(0).getName());
        verify(productRepository).findByIsFeaturedTrueAndActiveTrue();
    }

    @Test
    @DisplayName("Test lấy danh sách hàng mới về thành công")
    void test_GetNewReleases_Success() {
        Product p1 = Product.builder().id(1L).name("Skullpanda Everyday").isNewRelease(true).build();
        when(productRepository.findByIsNewReleaseTrueAndActiveTrue()).thenReturn(List.of(p1));

        List<Product> actualResult = productService.getNewReleases();

        assertEquals(1, actualResult.size());
        assertTrue(actualResult.get(0).getIsNewRelease());
    }

    @Test
    @DisplayName("Test tìm sản phẩm theo Slug - Tìm thấy")
    void test_GetProductBySlug_Found() {
        Product mockProduct = Product.builder().id(1L).name("Hirono Little Mischief").slug("hirono-little-mischief").build();
        when(productRepository.findBySlug("hirono-little-mischief")).thenReturn(Optional.of(mockProduct));

        Optional<Product> actualResult = productService.getProductBySlug("hirono-little-mischief");

        assertTrue(actualResult.isPresent());
        assertEquals("Hirono Little Mischief", actualResult.get().getName());
    }

    @Test
    @DisplayName("Test tìm sản phẩm theo Slug - Không tìm thấy")
    void test_GetProductBySlug_NotFound() {
        when(productRepository.findBySlug("khong-ton-tai")).thenReturn(Optional.empty());

        Optional<Product> actualResult = productService.getProductBySlug("khong-ton-tai");

        assertFalse(actualResult.isPresent());
    }

    @Test
    @DisplayName("Test lọc sản phẩm theo Category Slug")
    void test_GetProductsByCategorySlug() {
        Product p1 = Product.builder().id(1L).name("Blind Box 1").build();
        when(productRepository.findByCategorySlugAndActiveTrue("blind-box")).thenReturn(List.of(p1));

        List<Product> actualResult = productService.getProductsByCategorySlug("blind-box");

        assertEquals(1, actualResult.size());
        assertEquals("Blind Box 1", actualResult.get(0).getName());
    }

    @Test
    @DisplayName("Test tìm kiếm sản phẩm theo Keyword")
    void test_SearchProducts_WithKeyword() {
        Product p1 = Product.builder().id(1L).name("Molly Mega 400%").build();
        when(productRepository.findByNameContainingIgnoreCaseAndActiveTrue("Molly")).thenReturn(List.of(p1));

        List<Product> actualResult = productService.searchProducts("Molly");

        assertEquals(1, actualResult.size());
        assertEquals("Molly Mega 400%", actualResult.get(0).getName());
    }

    @Test
    @DisplayName("Test tìm kiếm sản phẩm với Keyword rỗng -> Trả về tất cả")
    void test_SearchProducts_EmptyKeyword() {
        Product p1 = Product.builder().id(1L).name("P1").build();
        Product p2 = Product.builder().id(2L).name("P2").build();
        when(productRepository.findByActiveTrue()).thenReturn(List.of(p1, p2));

        List<Product> actualResult = productService.searchProducts("   ");

        assertEquals(2, actualResult.size());
        verify(productRepository).findByActiveTrue();
    }

    @Test
    @DisplayName("Admin: getProductStats trả về đúng thống kê")
    void test_GetProductStats() {
        when(productRepository.count()).thenReturn(100L);
        when(productRepository.countByActiveTrue()).thenReturn(85L);
        when(productRepository.countByStockQuantityLessThanEqual(10)).thenReturn(12L);

        ProductStatsResponse stats = productService.getProductStats();

        assertNotNull(stats);
        assertEquals(100L, stats.getTotalCount());
        assertEquals(85L, stats.getActiveCount());
        assertEquals(12L, stats.getLowStockCount());
    }

    @Test
    @DisplayName("Admin: updateStock thành công")
    void test_UpdateStock_Success() {
        Product p = Product.builder().id(1L).name("Labubu").stockQuantity(5).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product updated = productService.updateStock(1L, 20);

        assertNotNull(updated);
        assertEquals(20, updated.getStockQuantity());
        verify(productRepository).save(p);
    }

    @Test
    @DisplayName("Admin: toggleActive đảo trạng thái active")
    void test_ToggleActive_Success() {
        Product p = Product.builder().id(1L).name("Labubu").active(true).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product toggled = productService.toggleActive(1L);

        assertNotNull(toggled);
        assertFalse(toggled.getActive());
        verify(productRepository).save(p);
    }

    @Test
    @DisplayName("getProductById: Thành công khi ID tồn tại")
    void getProductById_Success() {
        Product p = Product.builder().id(1L).name("Molly Space").build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));

        Product result = productService.getProductById(1L);

        assertEquals("Molly Space", result.getName());
    }

    @Test
    @DisplayName("getProductById: Ném ResourceNotFoundException khi ID không tồn tại")
    void getProductById_NotFound_ThrowsException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.getProductById(999L));
    }

    @Test
    @DisplayName("createProduct: Tự động sinh unique slug và đặt ảnh đầu tiên làm thumbnail")
    void createProduct_AutoSlug_And_FirstImageThumbnail() {
        Category category = Category.builder().id(10L).name("Blind Box").build();
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(productRepository.existsBySlug("hirono-little-mischief")).thenReturn(true);
        when(productRepository.existsBySlug("hirono-little-mischief-1")).thenReturn(false);

        MockMultipartFile file1 = new MockMultipartFile("f1", "img1.png", "image/png", new byte[]{1});
        MockMultipartFile file2 = new MockMultipartFile("f2", "img2.png", "image/png", new byte[]{2});

        when(cloudinaryService.uploadImage(file1)).thenReturn(new CloudinaryUploadResult("https://cdn.com/1.png", "pub1"));
        when(cloudinaryService.uploadImage(file2)).thenReturn(new CloudinaryUploadResult("https://cdn.com/2.png", "pub2"));

        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });

        ProductCreateRequest req = ProductCreateRequest.builder()
                .name("Hirono Little Mischief")
                .categoryId(10L)
                .singlePrice(BigDecimal.valueOf(350000))
                .stockQuantity(50)
                .imageFiles(List.of(file1, file2))
                .build();

        Product created = productService.createProduct(req);

        assertNotNull(created);
        assertEquals("hirono-little-mischief-1", created.getSlug());
        assertEquals(2, created.getImages().size());
        assertTrue(created.getImages().get(0).getIsThumbnail());
        assertFalse(created.getImages().get(1).getIsThumbnail());
        verify(productImageRepository, times(2)).save(any(ProductImage.class));
    }

    @Test
    @DisplayName("updateProduct: Giữ nguyên slug cũ, xóa ảnh chỉ định trên Cloudinary và cập nhật thumbnail")
    void updateProduct_KeepsSlug_And_CleansCloudinary() {
        Category cat = Category.builder().id(10L).build();
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(cat));

        ProductImage img1 = ProductImage.builder().id(101L).imageUrl("https://cdn.com/1.png").publicId("pub1").isThumbnail(true).displayOrder(0).build();
        ProductImage img2 = ProductImage.builder().id(102L).imageUrl("https://cdn.com/2.png").publicId("pub2").isThumbnail(false).displayOrder(1).build();

        List<ProductImage> imageList = new ArrayList<>(List.of(img1, img2));
        Product existingProduct = Product.builder()
                .id(1L)
                .name("Old Name")
                .slug("original-permanent-slug")
                .singlePrice(BigDecimal.valueOf(300000))
                .stockQuantity(10)
                .category(cat)
                .images(imageList)
                .build();
        img1.setProduct(existingProduct);
        img2.setProduct(existingProduct);

        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(productImageRepository.findById(101L)).thenReturn(Optional.of(img1));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        // Xóa img1 (thumbnail), ảnh còn lại img2 phải tự động được đôn lên làm thumbnail!
        ProductUpdateRequest updateReq = ProductUpdateRequest.builder()
                .name("New Name After Edit")
                .categoryId(10L)
                .singlePrice(BigDecimal.valueOf(380000))
                .stockQuantity(25)
                .deleteImageIds(List.of(101L))
                .build();

        Product updated = productService.updateProduct(1L, updateReq);

        assertNotNull(updated);
        assertEquals("original-permanent-slug", updated.getSlug()); // Bất biến: giữ slug cũ
        assertEquals("New Name After Edit", updated.getName());
        verify(cloudinaryService).deleteImage("pub1"); // Dọn dẹp Cloudinary
        verify(productImageRepository).delete(img1);
        assertEquals(1, updated.getImages().size());
        assertTrue(updated.getImages().get(0).getIsThumbnail()); // img2 được đôn lên làm thumbnail
    }

    @Test
    @DisplayName("deleteProduct: Sản phẩm có OrderItem -> Soft delete (active=false)")
    void deleteProduct_WithOrders_PerformsSoftDelete() {
        Product product = Product.builder().id(5L).name("Hot Toy").active(true).build();
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(orderItemRepository.existsByProductId(5L)).thenReturn(true);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean physical = productService.deleteProduct(5L);

        assertFalse(physical);
        assertFalse(product.getActive());
        verify(productRepository).save(product);
        verify(productRepository, never()).delete(any());
        verifyNoInteractions(cloudinaryService);
    }

    @Test
    @DisplayName("deleteProduct: Sản phẩm không có OrderItem -> Physical delete + Cloudinary cleanup")
    void deleteProduct_WithoutOrders_PerformsPhysicalDeleteAndCloudinaryCleanup() {
        ProductImage img = ProductImage.builder().id(11L).publicId("pub_del").imageUrl("url").build();
        Product product = Product.builder().id(6L).name("Unordered Toy").active(true).build();

        when(productRepository.findById(6L)).thenReturn(Optional.of(product));
        when(orderItemRepository.existsByProductId(6L)).thenReturn(false);
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(6L)).thenReturn(List.of(img));

        boolean physical = productService.deleteProduct(6L);

        assertTrue(physical);
        verify(cartItemRepository).deleteByProductId(6L);
        verify(cloudinaryService).deleteImage("pub_del");
        verify(productRepository).delete(product);
    }
}