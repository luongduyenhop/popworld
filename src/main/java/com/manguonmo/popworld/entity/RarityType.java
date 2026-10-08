package com.manguonmo.popworld.entity;

public enum RarityType {
    REGULAR("Bản thường (Regular)"),
    SECRET("Bản hiếm (Secret)");

    private final String description;

    RarityType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
