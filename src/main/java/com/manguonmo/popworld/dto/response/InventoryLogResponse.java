package com.manguonmo.popworld.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryLogResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String type;
    private String typeDisplay;
    private Integer quantityChanged;
    private Integer oldStock;
    private Integer newStock;
    private String reason;
    private String actor;
    private LocalDateTime createdAt;
    private String createdAtFormatted;
}
