package com.manguonmo.popworld.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoxReservationResponse {
    private String reservationCode;
    private Long productId;
    private String productName;
    private Integer boxIndex;
    private String status;
    private LocalDateTime reservedAt;
    private LocalDateTime expiresAt;
    private BigDecimal price;
}
