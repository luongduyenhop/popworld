package com.manguonmo.popworld.dto.response;

import lombok.*;

/**
 * Data Transfer Object (DTO) chứa số lượng đơn hàng theo từng trạng thái vòng đời chuẩn hóa.
 * Dùng để hiển thị huy hiệu (badge count) trên các tab lọc của Admin Dashboard và Order Management.
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
    private long shipping;
    private long delivered;
    private long cancelled;
    private long expired;

    // Backward compatibility aliases for existing code and templates
    public long getShipped() {
        return shipping;
    }

    public long getCompleted() {
        return delivered;
    }
}
