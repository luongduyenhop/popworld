package com.manguonmo.popworld.mapper;

import com.manguonmo.popworld.dto.response.OrderItemResponse;
import com.manguonmo.popworld.dto.response.OrderResponse;
import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.OrderItem;
import com.manguonmo.popworld.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class OrderMapperTest {
    private final OrderMapper orderMapper = Mappers.getMapper(OrderMapper.class);

    @Test
    @DisplayName("Test map itemResponse thành công đầy đủ trường")
    public void toItemResponse_shouldMapAllFieldsCorrectly() {
        Product product = Product.builder()
                .id(100L)
                .name("Hirono Little Mischief")
                .build();

        OrderItem item = OrderItem.builder()
                .product(product)
                .purchaseType("SINGLE_BOX")
                .quantity(2)
                .unitPrice(new BigDecimal("350000"))
                .totalPrice(new BigDecimal("700000"))
                .build();

        OrderItemResponse result = orderMapper.toItemResponse(item);

        assertNotNull(result);
        assertEquals(100L, result.getProductId());
        assertEquals("Hirono Little Mischief", result.getProductName());
        assertEquals(product.getMainImageUrl(), result.getProductImage());
        assertEquals("SINGLE_BOX", result.getPurchaseType());
        assertEquals(2, result.getQuantity());
        assertEquals(0, new BigDecimal("350000").compareTo(result.getUnitPrice()));
        assertEquals(0, new BigDecimal("700000").compareTo(result.getTotalPrice()));
    }

    @Test
    @DisplayName("Test map toResponse thành công với địa chỉ đầy đủ và danh sách items")
    public void toResponse_shouldMapOrderAndJoinAddressCorrectly() {
        LocalDateTime now = LocalDateTime.now();

        Product product = Product.builder()
                .id(101L)
                .name("Skullpanda City of Night")
                .build();

        OrderItem item = OrderItem.builder()
                .product(product)
                .purchaseType("WHOLE_SET")
                .quantity(1)
                .unitPrice(new BigDecimal("3600000"))
                .totalPrice(new BigDecimal("3600000"))
                .build();

        Order order = Order.builder()
                .orderCode("PW-20260916-001")
                .recipientName("Tran Van B")
                .recipientPhone("0988888888")
                .detailedAddress("So 10 Trang Tien")
                .ward("Phuong Trang Tien")
                .district("Quan Hoan Kiem")
                .provinceCity("Ha Noi")
                .subtotalAmount(new BigDecimal("3600000"))
                .shippingFee(BigDecimal.ZERO)
                .discountAmount(new BigDecimal("200000"))
                .totalAmount(new BigDecimal("3400000"))
                .status("PROCESSING")
                .paymentMethod("SEPAY")
                .build();
        order.setCreatedAt(now);

        OrderResponse response = orderMapper.toResponse(order, List.of(item));

        assertNotNull(response);
        assertEquals("PW-20260916-001", response.getOrderCode());
        assertEquals("Tran Van B", response.getRecipientName());
        assertEquals("0988888888", response.getRecipientPhone());
        assertEquals("So 10 Trang Tien, Phuong Trang Tien, Quan Hoan Kiem, Ha Noi", response.getFullAddress());
        assertEquals(0, new BigDecimal("3600000").compareTo(response.getSubtotalAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getShippingFee()));
        assertEquals(0, new BigDecimal("200000").compareTo(response.getDiscountAmount()));
        assertEquals(0, new BigDecimal("3400000").compareTo(response.getTotalAmount()));
        assertEquals("PROCESSING", response.getStatus());
        assertEquals("SEPAY", response.getPaymentMethod());
        assertEquals(now, response.getCreatedAt());

        assertNotNull(response.getItems());
        assertEquals(1, response.getItems().size());
        assertEquals(101L, response.getItems().get(0).getProductId());
        assertEquals("Skullpanda City of Night", response.getItems().get(0).getProductName());
    }

    @Test
    @DisplayName("Test toResponse bỏ qua ward khi null để tạo fullAddress sạch")
    public void toResponse_shouldHandleNullWardInAddress() {
        Order order = Order.builder()
                .orderCode("PW-NOWARD-001")
                .recipientName("Le Van C")
                .recipientPhone("0912345678")
                .detailedAddress("123 Nguyen Trai")
                .ward(null) // ward null
                .district("Thanh Xuan")
                .provinceCity("Ha Noi")
                .subtotalAmount(new BigDecimal("500000"))
                .totalAmount(new BigDecimal("500000"))
                .status("TO_PAY")
                .paymentMethod("COD")
                .build();

        OrderResponse response = orderMapper.toResponse(order, List.of());

        assertNotNull(response);
        assertEquals("123 Nguyen Trai, Thanh Xuan, Ha Noi", response.getFullAddress());
    }

    @Test
    @DisplayName("Test Null Safety: bảo vệ hệ thống không bị crash khi tham số null")
    public void shouldHandleNullSafety() {
        // 1. Khi item null -> trả về null
        assertNull(orderMapper.toItemResponse(null));
        // 2. Khi item có product null -> MapStruct vẫn tạo DTO an toàn, nhưng các trường product là null
        OrderItem itemWithoutProduct = OrderItem.builder().product(null).build();
        OrderItemResponse resWithoutProduct = orderMapper.toItemResponse(itemWithoutProduct);
        assertNotNull(resWithoutProduct);
        assertNull(resWithoutProduct.getProductId());
        assertNull(resWithoutProduct.getProductName());
        // 3. Khi order null -> trả về null
        assertNull(orderMapper.toResponse(null, null));
        // 4. Khi order hợp lệ nhưng danh sách items null -> trường items là null, không crash NPE
        Order order = Order.builder()
                .orderCode("PW-NO-ITEMS")
                .subtotalAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .build();
        OrderResponse response = orderMapper.toResponse(order, null);
        assertNotNull(response);
        assertNull(response.getItems()); // MapStruct gán null an toàn
    }
}
