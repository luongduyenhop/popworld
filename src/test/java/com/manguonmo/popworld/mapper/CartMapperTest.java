package com.manguonmo.popworld.mapper;

import com.manguonmo.popworld.dto.response.CartItemResponse;
import com.manguonmo.popworld.entity.CartItem;
import com.manguonmo.popworld.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CartMapperTest {

    private final CartMapper cartMapper = Mappers.getMapper(CartMapper.class);

    @Test
    @DisplayName("Test toResponse tính đúng đơn giá và thành tiền cho SINGLE_BOX")
    public void toResponse_shouldMapCorrectly_forSingleBox() {
        Product product = Product.builder()
                .id(10L)
                .name("Crybaby Sunset Concert")
                .singlePrice(new BigDecimal("350000"))
                .wholeSetPrice(new BigDecimal("2100000"))
                .build();

        CartItem cartItem = CartItem.builder()
                .id(1L)
                .product(product)
                .purchaseType("SINGLE_BOX")
                .quantity(3)
                .isSelected(true)
                .build();

        CartItemResponse response = cartMapper.toResponse(cartItem);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(10L, response.getProductId());
        assertEquals("Crybaby Sunset Concert", response.getProductName());
        assertEquals("SINGLE_BOX", response.getPurchaseType());
        assertEquals(3, response.getQuantity());
        assertEquals(0, new BigDecimal("350000").compareTo(response.getUnitPrice()));
        assertEquals(0, new BigDecimal("1050000").compareTo(response.getTotalPrice()));
        assertTrue(response.getIsSelected());
    }

    @Test
    @DisplayName("Test toResponse tính đúng đơn giá và thành tiền cho WHOLE_SET")
    public void toResponse_shouldMapCorrectly_forWholeSet() {
        Product product = Product.builder()
                .id(20L)
                .name("Dimoo Jurassic World")
                .singlePrice(new BigDecimal("320000"))
                .wholeSetPrice(new BigDecimal("3840000"))
                .build();

        CartItem cartItem = CartItem.builder()
                .id(2L)
                .product(product)
                .purchaseType("WHOLE_SET")
                .quantity(2)
                .isSelected(true)
                .build();

        CartItemResponse response = cartMapper.toResponse(cartItem);

        assertNotNull(response);
        assertEquals(2L, response.getId());
        assertEquals(20L, response.getProductId());
        assertEquals("Dimoo Jurassic World", response.getProductName());
        assertEquals("WHOLE_SET", response.getPurchaseType());
        assertEquals(2, response.getQuantity());
        assertEquals(0, new BigDecimal("3840000").compareTo(response.getUnitPrice()));
        assertEquals(0, new BigDecimal("7680000").compareTo(response.getTotalPrice()));
    }

    @Test
    @DisplayName("Test toResponseList map danh sách CartItem")
    public void toResponseList_shouldMapListCorrectly() {
        Product product = Product.builder().id(1L).singlePrice(BigDecimal.TEN).build();
        CartItem c1 = CartItem.builder().id(101L).product(product).quantity(1).purchaseType("SINGLE_BOX").build();
        CartItem c2 = CartItem.builder().id(102L).product(product).quantity(2).purchaseType("SINGLE_BOX").build();

        List<CartItemResponse> list = cartMapper.toResponseList(List.of(c1, c2));

        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals(101L, list.get(0).getId());
        assertEquals(102L, list.get(1).getId());
    }

    @Test
    @DisplayName("Test Null Safety cho CartMapper")
    public void shouldHandleNullSafety() {
        assertNull(cartMapper.toResponse(null));
        assertNull(cartMapper.toResponseList(null));

        CartItem cartItemWithoutProduct = CartItem.builder().id(999L).build();
        CartItemResponse response = cartMapper.toResponse(cartItemWithoutProduct);

        assertNotNull(response);
        assertEquals(999L, response.getId());
        assertNull(response.getProductId());
        assertNull(response.getProductName());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getUnitPrice()));
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getTotalPrice()));
    }
}
