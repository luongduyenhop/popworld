package com.manguonmo.popworld.mapper;

import com.manguonmo.popworld.dto.response.CartItemResponse;
import com.manguonmo.popworld.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CartMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productImage", expression = "java(cartItem.getProduct() != null ? cartItem.getProduct().getMainImageUrl() : null)")
    @Mapping(target = "unitPrice", expression = "java(calculateUnitPrice(cartItem))")
    @Mapping(target = "totalPrice", expression = "java(calculateTotalPrice(cartItem))")
    CartItemResponse toResponse(CartItem cartItem);

    List<CartItemResponse> toResponseList(List<CartItem> cartItems);

    default BigDecimal calculateUnitPrice(CartItem cartItem) {
        if (cartItem == null || cartItem.getProduct() == null) {
            return BigDecimal.ZERO;
        }
        return "WHOLE_SET".equalsIgnoreCase(cartItem.getPurchaseType())
                ? cartItem.getProduct().getWholeSetPrice()
                : cartItem.getProduct().getSinglePrice();
    }

    default BigDecimal calculateTotalPrice(CartItem cartItem) {
        if (cartItem == null || cartItem.getQuantity() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal unitPrice = calculateUnitPrice(cartItem);
        return unitPrice != null ? unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())) : BigDecimal.ZERO;
    }
}
