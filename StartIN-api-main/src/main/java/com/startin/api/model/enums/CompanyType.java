package com.startin.api.model.enums;

public enum CompanyType {
    EMPRESA,
    ORGANIZACAO,
    INSTITUICAO;

    public static CompanyType fromString(String value) {
        if (value == null) return EMPRESA;
        try {
            return CompanyType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return EMPRESA;
        }
    }
}
