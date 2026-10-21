package com.startin.api.dto.match;

import jakarta.validation.constraints.NotBlank;

public class UpdateMatchStatusDTO {

    @NotBlank(message = "Status é obrigatório")
    private String status; // NOVO, SALVO, INTERESSE_ENVIADO, CONTATO_INICIADO, EM_NEGOCIACAO

    public UpdateMatchStatusDTO() {
    }

    public UpdateMatchStatusDTO(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
