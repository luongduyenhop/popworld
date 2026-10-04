package com.manguonmo.popworld.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TopProductRevenueDto {
    private Long productId;
    private String productName;
    private String imageUrl;
    private long unitsSold;
    private BigDecimal revenue;
}
