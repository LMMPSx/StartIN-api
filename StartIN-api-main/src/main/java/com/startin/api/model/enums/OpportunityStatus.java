package com.startin.api.model.enums;

public enum OpportunityStatus {
    PUBLICADA,
    EM_ANALISE,
    ENCERRADA;

    public static OpportunityStatus fromString(String value) {
        if (value == null) return PUBLICADA;
        try {
            return OpportunityStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PUBLICADA;
        }
    }
}
