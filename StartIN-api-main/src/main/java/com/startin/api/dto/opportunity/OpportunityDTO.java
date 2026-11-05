package com.startin.api.dto.opportunity;

import com.startin.api.model.Opportunity;

public class OpportunityDTO {

    private Long id;
    private String code;
    private String type;
    private String title;
    private String shortDescription;
    private String description;
    private Integer match; // match percentage
    private String meta;
    private String tags;
    private String color;
    private String status;
    private String externalUrl;
    private Long companyId;
    private String companyName;
    private Long startupId;
    private String startupName;
    private String targetCategory;

    public OpportunityDTO() {
    }

    public static OpportunityDTO fromEntity(Opportunity entity) {
        if (entity == null) return null;
        OpportunityDTO dto = new OpportunityDTO();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode() != null ? entity.getCode() : String.valueOf(entity.getId()));
        dto.setType(entity.getType() != null ? entity.getType().name() : "EDITAL");
        dto.setTitle(entity.getTitle());
        dto.setShortDescription(entity.getShortDescription());
        dto.setDescription(entity.getDescription());
        dto.setMatch(entity.getMatchScore());
        dto.setMeta(entity.getMetaInfo());
        dto.setTags(entity.getTags());
        dto.setColor(entity.getColor());
        dto.setStatus(entity.getStatus() != null ? entity.getStatus().name() : "PUBLICADA");
        dto.setExternalUrl(entity.getExternalUrl());
        dto.setTargetCategory(entity.getTargetCategory());
        if (entity.getCompany() != null) {
            dto.setCompanyId(entity.getCompany().getId());
            dto.setCompanyName(entity.getCompany().getName());
        }
        if (entity.getStartup() != null) {
            dto.setStartupId(entity.getStartup().getId());
            dto.setStartupName(entity.getStartup().getName());
        }
        return dto;
    }

    public Long getStartupId() { return startupId; }
    public void setStartupId(Long startupId) { this.startupId = startupId; }
    public String getStartupName() { return startupName; }
    public void setStartupName(String startupName) { this.startupName = startupName; }
    public String getTargetCategory() { return targetCategory; }
    public void setTargetCategory(String targetCategory) { this.targetCategory = targetCategory; }

    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getShortDescription() { return shortDescription; }
    public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getMatch() { return match; }
    public void setMatch(Integer match) { this.match = match; }
    public String getMeta() { return meta; }
    public void setMeta(String meta) { this.meta = meta; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getExternalUrl() { return externalUrl; }
    public void setExternalUrl(String externalUrl) { this.externalUrl = externalUrl; }
}
