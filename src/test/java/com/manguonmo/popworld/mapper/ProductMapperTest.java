package com.manguonmo.popworld.mapper;

import com.manguonmo.popworld.dto.response.ProductResponse;
import com.manguonmo.popworld.entity.Category;
import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.entity.ProductImage;
import com.manguonmo.popworld.entity.Series;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProductMapperTest {

    private final ProductMapper productMapper = Mappers.getMapper(ProductMapper.class);

    @Test
    @DisplayName("Test toResponse map đầy đủ thông tin Product, Category và Series")
    public void toResponse_shouldMapAllFieldsCorrectly() {
        Category category = Category.builder()
                .id(1L)
                .name("Blind Box")
                .slug("blind-box")
                .build();

        Series series = Series.builder()
                .id(10L)
                .name("The Monsters")
                .build();

        List<ProductImage> images = new ArrayList<>();
        images.add(ProductImage.builder()
                .imageUrl("https://popworld.vn/labubu-thumb.jpg")
                .isThumbnail(true)
                .displayOrder(1)
                .build());

        Product product = Product.builder()
                .id(200L)
                .name("Labubu The Monsters Tasty Macarons")
                .slug("labubu-tasty-macarons")
                .description("Mô hình hộp mù Labubu Macarons siêu hot")
                .singlePrice(new BigDecimal("380000"))
                .wholeSetPrice(new BigDecimal("2280000"))
                .stockQuantity(50)
                .packagingType("1 Box / 6 Boxes per Set")
                .secretRatio("1/72")
                .material("PVC/Plush")
                .sizeDimensions("Height: 17cm")
                .category(category)
                .series(series)
                .images(images)
                .isFeatured(true)
                .isNewRelease(true)
                .build();

        ProductResponse response = productMapper.toResponse(product);

        assertNotNull(response);
        assertEquals(200L, response.getId());
        assertEquals("Labubu The Monsters Tasty Macarons", response.getName());
        assertEquals("labubu-tasty-macarons", response.getSlug());
        assertEquals(0, new BigDecimal("380000").compareTo(response.getSinglePrice()));
        assertEquals(0, new BigDecimal("2280000").compareTo(response.getWholeSetPrice()));
        assertEquals(50, response.getStockQuantity());
        assertEquals("1 Box / 6 Boxes per Set", response.getPackagingType());
        assertEquals("1/72", response.getSecretRatio());
        assertEquals("Blind Box", response.getCategoryName());
        assertEquals("The Monsters", response.getSeriesName());
        assertEquals("https://popworld.vn/labubu-thumb.jpg", response.getMainImageUrl());
        assertTrue(response.getIsFeatured());
        assertTrue(response.getIsNewRelease());
    }

    @Test
    @DisplayName("Test toResponseList map danh sách sản phẩm thành công")
    public void toResponseList_shouldMapListCorrectly() {
        Product p1 = Product.builder().id(1L).name("P1").singlePrice(BigDecimal.TEN).build();
        Product p2 = Product.builder().id(2L).name("P2").singlePrice(BigDecimal.ONE).build();

        List<ProductResponse> list = productMapper.toResponseList(List.of(p1, p2));

        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals("P1", list.get(0).getName());
        assertEquals("P2", list.get(1).getName());
    }

    @Test
    @DisplayName("Test Null Safety cho ProductMapper")
    public void shouldHandleNullSafety() {
        assertNull(productMapper.toResponse(null));
        assertNull(productMapper.toResponseList(null));

        Product productWithoutCatAndSeries = Product.builder()
                .id(300L)
                .name("No Cat Series")
                .build();

        ProductResponse response = productMapper.toResponse(productWithoutCatAndSeries);
        assertNotNull(response);
        assertNull(response.getCategoryName());
        assertNull(response.getSeriesName());
        assertEquals("https://placehold.co/400x400?text=No+Image", response.getMainImageUrl());
    }
}
