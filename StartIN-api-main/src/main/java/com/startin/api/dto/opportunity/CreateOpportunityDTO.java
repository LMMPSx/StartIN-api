package com.startin.api.dto.opportunity;

import jakarta.validation.constraints.NotBlank;

public class CreateOpportunityDTO {

    private String type = "POC"; // POC, Compra, POC + Compra, Edital, Evento

    @NotBlank(message = "Título é obrigatório")
    private String title;

    @NotBlank(message = "Descrição é obrigatória")
    private String description;

    private String tags; // Perfil de startup desejado para o match
    private String meta;
    private String externalUrl;

    public CreateOpportunityDTO() {
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getMeta() { return meta; }
    public void setMeta(String meta) { this.meta = meta; }
    public String getExternalUrl() { return externalUrl; }
    public void setExternalUrl(String externalUrl) { this.externalUrl = externalUrl; }
}
