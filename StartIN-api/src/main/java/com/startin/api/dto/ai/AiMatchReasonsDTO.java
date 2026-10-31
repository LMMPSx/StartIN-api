package com.startin.api.dto.ai;

public class AiMatchReasonsDTO {

    private String perfil;
    private String requisitos;
    private String objetivo;
    private String contexto;

    public AiMatchReasonsDTO() {
    }

    public AiMatchReasonsDTO(String perfil, String requisitos, String objetivo, String contexto) {
        this.perfil = perfil;
        this.requisitos = requisitos;
        this.objetivo = objetivo;
        this.contexto = contexto;
    }

    public String getPerfil() { return perfil; }
    public void setPerfil(String perfil) { this.perfil = perfil; }
    public String getRequisitos() { return requisitos; }
    public void setRequisitos(String requisitos) { this.requisitos = requisitos; }
    public String getObjetivo() { return objetivo; }
    public void setObjetivo(String objetivo) { this.objetivo = objetivo; }
    public String getContexto() { return contexto; }
    public void setContexto(String contexto) { this.contexto = contexto; }
}
