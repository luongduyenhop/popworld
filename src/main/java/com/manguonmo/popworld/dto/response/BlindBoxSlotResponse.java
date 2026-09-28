package com.manguonmo.popworld.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlindBoxSlotResponse {
    private Integer slotIndex;
    private String status; // AVAILABLE, HELD, SOLD
}
