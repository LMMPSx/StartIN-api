package com.api.backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class StartupRequestDTO {

    @NotNull(message = "O ID do usuário proprietário é obrigatório.")
    private Long usuarioId;

    @NotBlank(message = "O nome fantasia é obrigatório.")
    @Size(max = 150, message = "O nome fantasia deve ter no máximo 150 caracteres.")
    private String nomeFantasia;

    @NotBlank(message = "O CNPJ é obrigatório.")
    @Size(min = 14, max = 14, message = "O CNPJ deve conter exatamente 14 caracteres numéricos.")
    private String cnpj;

    @Size(max = 50, message = "O estágio deve ter no máximo 50 caracteres.")
    private String estagio;

    @Size(max = 100, message = "O segmento deve ter no máximo 100 caracteres.")
    private String segmento;

    private String descricaoSolucao;

    @Size(max = 255, message = "A URL do site deve ter no máximo 255 caracteres.")
    private String siteUrl;
}