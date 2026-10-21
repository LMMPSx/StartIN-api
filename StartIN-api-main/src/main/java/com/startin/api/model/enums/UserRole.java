package com.startin.api.model.enums;

public enum UserRole {
    USER,
    ADMIN;

    public static UserRole fromString(String value) {
        if (value == null) return USER;
        try {
            return UserRole.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return USER;
        }
    }
}
