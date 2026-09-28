package com.manguonmo.popworld.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipCabinetRequest {

    @NotNull(message = "Vui lòng chọn địa chỉ nhận hàng!")
    private Long addressId;

    private Long ownedItemId;

    private List<Long> ownedItemIds;

    public List<Long> resolveItemIds() {
        if (ownedItemIds != null && !ownedItemIds.isEmpty()) {
            return ownedItemIds;
        }
        if (ownedItemId != null) {
            return List.of(ownedItemId);
        }
        return Collections.emptyList();
    }
}
