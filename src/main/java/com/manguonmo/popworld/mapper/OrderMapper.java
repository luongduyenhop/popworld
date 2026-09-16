package com.manguonmo.popworld.mapper;

import com.manguonmo.popworld.dto.response.OrderItemResponse;
import com.manguonmo.popworld.dto.response.OrderResponse;
import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    @Mapping(target = "productId",source = "product.id")
    @Mapping(target = "productName",source = "product.name")
    @Mapping(target = "productImage",expression = "java(item.getProduct() != null ? item.getProduct().getMainImageUrl() : null)")
      OrderItemResponse toItemResponse(OrderItem item);

    @Mapping(target = "items",source = "items")
    @Mapping(target = "fullAddress", expression = "java(joinFullAddress(order))")
    OrderResponse toResponse(Order order, List<OrderItem> items);
    default String joinFullAddress(Order order) {
        if (order == null) return null;
        return Stream.of(order.getDetailedAddress(), order.getWard(), order.getDistrict(), order.getProvinceCity())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(", "));
    }
}
