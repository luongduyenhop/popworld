package com.manguonmo.popworld.entity;

public enum OwnedItemStatus {
    IN_CABINET("Trong tủ đồ ảo"),
    REQUESTED_SHIPPING("Yêu cầu giao hàng"),
    SHIPPED("Đã giao hàng");

    private final String description;

    OwnedItemStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
