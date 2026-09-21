package com.api.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "startups")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Startup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "nome_fantasia", nullable = false, length = 150)
    private String nomeFantasia;

    @Column(nullable = false, length = 14)
    private String cnpj;

    @Column(length = 50)
    private String estagio;

    @Column(length = 100)
    private String segmento;

    @Column(name = "descricao_solucao", columnDefinition = "TEXT")
    private String descricaoSolucao;

    @Column(name = "site_url", length = 255)
    private String siteUrl;
}