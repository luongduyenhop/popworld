package com.manguonmo.popworld.service;


// 1. Nhúng thư viện JUnit 5 và Mockito

import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// 2. Kích hoạt Mockito chạy cùng JUnit 5
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    // 3. @Mock: Tạo ra một ProductRepository "giả vờ"
    // Mockito tạo ra một con rối rỗng, không hề kết nối đến MySQL
    @Mock
    private ProductRepository productRepository;

    // 4. @InjectMocks: Tạo ra đối tượng THẬT mà chúng ta muốn test
    // Mockito sẽ tự động tiêm con rối productRepository ở trên vào ProductServiceImpl!
    @InjectMocks
    private ProductServiceImpl productService;

    // 5. @Test: Báo cho JUnit biết đây là một kịch bản kiểm thử
    @Test
    @DisplayName("Test lấy danh sách sản phẩm nổi bật thành công")
    void test_GetFeaturedProducts_Success() {
        // --- BƯỚC 1: ARRANGE (Chuẩn bị hiện trường & Dữ liệu giả) ---
        // Ta tạo sẵn 2 sản phẩm mẫu bằng tay
        Product p1 = Product.builder().id(1L).name("Labubu Fall in Wild").isFeatured(true).build();
        Product p2 = Product.builder().id(2L).name("Molly Space").isFeatured(true).build();
        List<Product> mockList = List.of(p1, p2);

        // Dạy cho con rối Repository: "Hễ ai gọi hàm findByIsFeaturedTrueAndActiveTrue() thì hãy trả về mockList!"
        when(productRepository.findByIsFeaturedTrueAndActiveTrue()).thenReturn(mockList);

        // --- BƯỚC 2: ACT (Thực thi hành động cần test) ---
        // Gọi hàm của Service - đây là đối tượng ta đang muốn kiểm thử
        List<Product> actualResult = productService.getFeaturedProducts();

        // --- BƯỚC 3: ASSERT (Trọng tài kiểm tra kết quả) ---
        // Kết quả trả về có bị null không?
        assertNotNull(actualResult, "Danh sách trả về không được null!");

        // Kích thước danh sách có đúng bằng 2 không?
        assertEquals(2, actualResult.size(), "Phải trả về đúng 2 sản phẩm!");

        // Tên sản phẩm đầu tiên có đúng là Labubu không?
        assertEquals("Labubu Fall in Wild", actualResult.get(0).getName());

        // Kiểm tra xem Service có thực sự gọi xuống Repository đúng 1 lần không?
        verify(productRepository, times(1)).findByIsFeaturedTrueAndActiveTrue();
    }

    @Test
    @DisplayName("Lay danh sach hang moi thanh cong")
    void test_GetNewReleasesProducts_Success(){
        Product p1 = Product.builder().id(1L).name("Nyota").isNewRelease(true).build();
        Product p2 = Product.builder().id(2L).name("Maruko").isNewRelease(true).build();

        List<Product> mockList = List.of(p1,p2);

        when(productRepository.findByIsNewReleaseTrueAndActiveTrue()).thenReturn(mockList);

        List<Product> result = productService.getNewReleases();

        assertNotNull(result,"Danh sach khong duoc null");

        assertEquals(2,result.size(),"danh sach phai tra ve 2 san pham");

        assertEquals("Nyota", result.get(0).getName());

        verify(productRepository, times(1)).findByIsNewReleaseTrueAndActiveTrue();

    }

    @Test
    @DisplayName("Lay chi tiet san pham theo slug - tim thay")
    void test_GetProductBySlug_Success(){
        Product p3 = Product.builder().id(3L).name("twinkle chibi").slug("chibi").build();


        Optional<Product> test = Optional.of(p3);

        when(productRepository.findBySlug("chibi")).thenReturn(test);

        Optional<Product> rsTest = productService.getProductBySlug("chibi");

        assertTrue(rsTest.isPresent());

        assertEquals("twinkle chibi",rsTest.get().getName());


    }

    @Test
    @DisplayName("Lay chi tiet san pham theo slug - khong tim thay")
    void test_GetProductBySlug_Fail(){
        Product p1 = Product.builder().id(1L).name("maruko chibi").slug("chibi").build();


        when(productRepository.findBySlug("slug khong ton tai")).thenReturn(Optional.empty());

        Optional<Product> rsTest = productService.getProductBySlug("slug khong ton tai");

        assertTrue(rsTest.isEmpty());



    }
}