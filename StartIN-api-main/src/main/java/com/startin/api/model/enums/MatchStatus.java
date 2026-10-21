package com.startin.api.model.enums;

public enum MatchStatus {
    NOVO,
    SALVO,
    INTERESSE_ENVIADO,
    CONTATO_INICIADO,
    EM_NEGOCIACAO;

    public static MatchStatus fromString(String value) {
        if (value == null) return NOVO;
        try {
            return MatchStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NOVO;
        }
    }
}
