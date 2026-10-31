package com.startin.api.model.enums;

public enum OpportunityType {
    EDITAL,
    EVENTO,
    POC,
    PRIMEIRA_COMPRA,
    COMPRA;

    public static OpportunityType fromString(String value) {
        if (value == null) return EDITAL;
        String normalized = value.trim().toUpperCase();
        if (normalized.contains("PRIMEIRA") || normalized.contains("1A COMPRA") || normalized.contains("1ª COMPRA")) {
            return PRIMEIRA_COMPRA;
        }
        if (normalized.startsWith("EDITAL") || normalized.startsWith("EDITAIS")) return EDITAL;
        if (normalized.startsWith("EVENTO")) return EVENTO;
        if (normalized.startsWith("POC")) return POC;
        if (normalized.startsWith("COMPRA")) return COMPRA;
        return EDITAL;
    }
}
