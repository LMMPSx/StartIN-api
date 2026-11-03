package com.startin.api.model.enums;

public enum UserRole {
    USER,
    STARTUP,
    COMPANY,
    ADMIN;

    public static UserRole fromString(String value) {
        if (value == null || value.isBlank()) return STARTUP;
        String clean = value.trim().toUpperCase();
        if ("EMPRESA".equals(clean) || "COMPANY".equals(clean) || "ORGANIZACAO".equals(clean)) {
            return COMPANY;
        }
        if ("STARTUP".equals(clean)) {
            return STARTUP;
        }
        try {
            return UserRole.valueOf(clean);
        } catch (IllegalArgumentException e) {
            return STARTUP;
        }
    }
}
