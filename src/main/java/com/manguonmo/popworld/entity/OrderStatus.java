package com.manguonmo.popworld.entity;

/**
 * Định nghĩa vòng đời và trạng thái chuẩn hóa cho Đơn hàng (Order State Machine)
 * 
 * Vòng đời chuẩn:
 * TO_PAY -> PROCESSING -> SHIPPING -> DELIVERED
 * Branching:
 * TO_PAY -> CANCELLED (Customer cancel hoặc Admin cancel)
 * TO_PAY -> EXPIRED (Scheduler timeout 15 phút)
 * PROCESSING -> CANCELLED (Admin cancel only)
 */
public enum OrderStatus {
    TO_PAY("Chờ thanh toán"),
    PROCESSING("Đang xử lý"),
    SHIPPING("Đang giao hàng"),
    DELIVERED("Đã giao hàng"),
    CANCELLED("Đã hủy"),
    EXPIRED("Hết hạn");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Các trạng thái kết thúc (Terminal states), không thể chuyển tiếp sang bất kỳ trạng thái nào khác.
     */
    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED || this == EXPIRED;
    }

    /**
     * Khách hàng CHỈ được phép hủy đơn hàng khi đơn ở trạng thái TO_PAY.
     */
    public boolean canCustomerCancel() {
        return this == TO_PAY;
    }

    /**
     * Quản trị viên (Admin) chỉ được phép hủy đơn khi đơn ở TO_PAY hoặc PROCESSING.
     */
    public boolean canAdminCancel() {
        return this == TO_PAY || this == PROCESSING;
    }
}
