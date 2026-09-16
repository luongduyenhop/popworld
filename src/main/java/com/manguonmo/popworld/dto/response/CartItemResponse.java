package com.manguonmo.popworld.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {

    private Long id;

    private Long productId;

    private String productName;

    private String productImage;

    private String purchaseType;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

    private Boolean isSelected;
}
