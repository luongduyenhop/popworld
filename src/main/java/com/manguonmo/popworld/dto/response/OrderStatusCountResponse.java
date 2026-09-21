package com.manguonmo.popworld.dto.response;

import lombok.*;

/**
 * Data Transfer Object (DTO) chứa số lượng đơn hàng theo từng trạng thái vòng đời.
 * Dùng để hiển thị huy hiệu (badge count) trên các tab lọc của Admin Dashboard.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusCountResponse {
    private long all;
    private long toPay;
    private long processing;
    private long shipped;
    private long completed;
    private long cancelled;
}
