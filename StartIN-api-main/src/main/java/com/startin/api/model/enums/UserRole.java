package com.startin.api.model.enums;

public enum UserRole {
    USER,
    STARTUP,
    COMPANY,
    PROVIDER,
    ADMIN;

    public static UserRole fromString(String value) {
        if (value == null || value.isBlank()) return STARTUP;
        String clean = value.trim().toUpperCase();
        if ("EMPRESA".equals(clean) || "COMPANY".equals(clean) || "ORGANIZACAO".equals(clean)) {
            return COMPANY;
        }
        if ("PRESTADOR".equals(clean) || "PROVIDER".equals(clean) || "PRESTADOR_SERVICO".equals(clean) || "SERVICE_PROVIDER".equals(clean)) {
            return PROVIDER;
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
