package com.startin.api.model.enums;

public enum OpportunityType {
    EDITAL,
    EVENTO,
    POC,
    COMPRA;

    public static OpportunityType fromString(String value) {
        if (value == null) return EDITAL;
        String normalized = value.trim().toUpperCase();
        if (normalized.startsWith("EDITAL")) return EDITAL;
        if (normalized.startsWith("EVENTO")) return EVENTO;
        if (normalized.startsWith("POC")) return POC;
        if (normalized.startsWith("COMPRA")) return COMPRA;
        return EDITAL;
    }
}
