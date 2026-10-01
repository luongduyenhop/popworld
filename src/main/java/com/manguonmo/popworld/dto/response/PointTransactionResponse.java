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
public class PointTransactionResponse {
    private LocalDateTime timestamp;
    private String timeFormatted;
    private String remarks;
    private Integer pointsChange;
    private Boolean isPositive;
    private String transactionType;
}
