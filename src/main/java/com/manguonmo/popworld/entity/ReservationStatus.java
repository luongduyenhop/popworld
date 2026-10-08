package com.manguonmo.popworld.entity;

public enum ReservationStatus {
    RESERVED("Đang giữ hộp"),
    PURCHASED("Đã thanh toán"),
    UNBOXED("Đã mở hộp"),
    EXPIRED("Hết hạn"),
    CANCELLED("Đã hủy");

    private final String description;

    ReservationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
