package com.manguonmo.popworld.entity;

public enum SlotStatus {
    AVAILABLE("Còn trống"),
    HELD("Đang giữ"),
    SOLD("Đã bán");

    private final String description;

    SlotStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
