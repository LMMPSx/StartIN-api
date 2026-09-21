package com.api.backend.dto;

import lombok.Data;

@Data
public class StartupResponseDTO {
    private Long id;
    private Long usuarioId;
    private String nomeFantasia;
    private String cnpj;
    private String estagio;
    private String segmento;
    private String descricaoSolucao;
    private String siteUrl;
}