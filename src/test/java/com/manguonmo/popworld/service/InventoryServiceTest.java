package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.response.InventorySummaryResponse;
import com.manguonmo.popworld.entity.Category;
import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.repository.ProductRepository;
import com.manguonmo.popworld.service.impl.InventoryServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    @Test
    @DisplayName("Tính toán tổng hợp thống kê tồn kho chính xác")
    void getInventorySummary_calculatesCorrectKPIs() {
        Category cat = Category.builder().name("Blind Box").build();

        Product p1 = Product.builder().id(1L).name("Labubu").stockQuantity(0).singlePrice(new BigDecimal("300000")).category(cat).build(); // OUT_OF_STOCK
        Product p2 = Product.builder().id(2L).name("Molly").stockQuantity(5).singlePrice(new BigDecimal("250000")).category(cat).build();  // LOW_STOCK
        Product p3 = Product.builder().id(3L).name("Skullpanda").stockQuantity(20).singlePrice(new BigDecimal("400000")).category(cat).build(); // SAFE

        when(productRepository.findAll()).thenReturn(List.of(p1, p2, p3));

        InventorySummaryResponse summary = inventoryService.getInventorySummary();

        assertEquals(3, summary.getTotalProductCount());
        assertEquals(25, summary.getTotalStockUnits()); // 0 + 5 + 20
        assertEquals(1, summary.getOutOfStockCount());
        assertEquals(1, summary.getLowStockCount());
        assertEquals(1, summary.getSafeStockCount());

        // Value: (5 * 250000) + (20 * 400000) = 1,250,000 + 8,000,000 = 9,250,000
        assertEquals(new BigDecimal("9250000"), summary.getTotalInventoryValue());
    }

    @Test
    @DisplayName("Lọc danh sách tồn kho theo ngưỡng trạng thái")
    void getInventoryProducts_filtersByStockLevel() {
        Product p1 = Product.builder().id(1L).name("Labubu").stockQuantity(0).build();
        Product p2 = Product.builder().id(2L).name("Molly").stockQuantity(4).build();
        Product p3 = Product.builder().id(3L).name("Skullpanda").stockQuantity(30).build();

        when(productRepository.findAll()).thenReturn(List.of(p1, p2, p3));

        List<Product> outOfStock = inventoryService.getInventoryProducts("OUT_OF_STOCK", null);
        assertEquals(1, outOfStock.size());
        assertEquals("Labubu", outOfStock.get(0).getName());

        List<Product> lowStock = inventoryService.getInventoryProducts("LOW_STOCK", null);
        assertEquals(1, lowStock.size());
        assertEquals("Molly", lowStock.get(0).getName());

        List<Product> safeStock = inventoryService.getInventoryProducts("SAFE", null);
        assertEquals(1, safeStock.size());
        assertEquals("Skullpanda", safeStock.get(0).getName());
    }

    @Test
    @DisplayName("Nhập thêm kho nhanh thành công và tăng số lượng tồn")
    void quickRestock_increasesStockQuantity() {
        Product p = Product.builder().id(1L).name("Crybaby").stockQuantity(10).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product updated = inventoryService.quickRestock(1L, 15, "Bổ sung hàng đợt 2");

        assertEquals(25, updated.getStockQuantity());
        verify(productRepository).save(p);
    }

    @Test
    @DisplayName("Nhập kho với số lượng nhỏ hơn hoặc bằng 0 ném lỗi")
    void quickRestock_invalidQuantity_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                inventoryService.quickRestock(1L, 0, "note"));

        assertThrows(IllegalArgumentException.class, () ->
                inventoryService.quickRestock(1L, -5, "note"));
    }

    @Test
    @DisplayName("Nhập kho cho sản phẩm không tồn tại ném ResourceNotFoundException")
    void quickRestock_notFound_throwsException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                inventoryService.quickRestock(999L, 10, "note"));
    }

    @Test
    @DisplayName("Điều chỉnh kiểm kê tồn kho cập nhật đúng số lượng tồn thực tế")
    void adjustStock_updatesStockQuantitySuccessfully() {
        Product p = Product.builder().id(2L).name("Dimoo").stockQuantity(50).build();
        when(productRepository.findById(2L)).thenReturn(Optional.of(p));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        // Adjust directly down to 4 (triggering low stock)
        Product updated = inventoryService.adjustStock(2L, 4, "Kiểm kê thực tế");

        assertEquals(4, updated.getStockQuantity());
        verify(productRepository).save(p);
    }

    @Test
    @DisplayName("Điều chỉnh tồn kho với số lượng âm ném IllegalArgumentException")
    void adjustStock_negativeQuantity_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                inventoryService.adjustStock(1L, -3, "Kiểm kê lỗi"));
    }

    @Test
    @DisplayName("Thống kê tồn kho với ngưỡng cảnh báo tùy chỉnh")
    void getInventorySummary_customThreshold() {
        Category cat = Category.builder().name("Blind Box").build();

        Product p1 = Product.builder().id(1L).name("Labubu").stockQuantity(0).singlePrice(new BigDecimal("100000")).category(cat).build(); // 0 -> OUT_OF_STOCK
        Product p2 = Product.builder().id(2L).name("Molly").stockQuantity(8).singlePrice(new BigDecimal("100000")).category(cat).build();  // 8 -> LOW if threshold=10, but SAFE if threshold=5
        Product p3 = Product.builder().id(3L).name("Skullpanda").stockQuantity(15).singlePrice(new BigDecimal("100000")).category(cat).build();

        when(productRepository.findAll()).thenReturn(List.of(p1, p2, p3));

        // When threshold is 5: p2 has 8 > 5, so safeStock is 2, lowStock is 0
        InventorySummaryResponse summary5 = inventoryService.getInventorySummary(5);
        assertEquals(1, summary5.getOutOfStockCount());
        assertEquals(0, summary5.getLowStockCount());
        assertEquals(2, summary5.getSafeStockCount());
        assertEquals(5, summary5.getLowStockThreshold());

        // When threshold is 10: p2 has 8 <= 10, so lowStock is 1, safeStock is 1
        InventorySummaryResponse summary10 = inventoryService.getInventorySummary(10);
        assertEquals(1, summary10.getOutOfStockCount());
        assertEquals(1, summary10.getLowStockCount());
        assertEquals(1, summary10.getSafeStockCount());
        assertEquals(10, summary10.getLowStockThreshold());
    }
}
