package com.startin.api.dto.company;

import com.startin.api.model.Company;

public class CompanyDTO {

    private Long id;
    private String name;
    private String type;
    private String description;
    private String tags;

    public CompanyDTO() {
    }

    public static CompanyDTO fromEntity(Company entity) {
        if (entity == null) return null;
        CompanyDTO dto = new CompanyDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setType(entity.getType() != null ? entity.getType().name() : "EMPRESA");
        dto.setDescription(entity.getDescription());
        dto.setTags(entity.getTags());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
}
