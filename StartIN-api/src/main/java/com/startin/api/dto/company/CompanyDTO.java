package com.startin.api.dto.company;

import com.startin.api.model.Company;
import com.startin.api.model.enums.CompanyType;

public class CompanyDTO {

    private Long id;
    private Long userId;
    private String name;
    private String type;
    private String sector;
    private String location;
    private String cnpj;
    private String phone;
    private String website;
    private String description;
    private String demands;
    private String tags;
    private String preferredStage;
    private String operationMode;
    private String avatarUrl;

    public CompanyDTO() {
    }

    public static CompanyDTO fromEntity(Company entity) {
        if (entity == null) return null;
        CompanyDTO dto = new CompanyDTO();
        dto.setId(entity.getId());
        if (entity.getUser() != null) {
            dto.setUserId(entity.getUser().getId());
        }
        String resolvedAvatar = entity.getAvatarUrl();
        if ((resolvedAvatar == null || resolvedAvatar.isBlank()) && entity.getUser() != null) {
            resolvedAvatar = entity.getUser().getAvatarUrl();
        }
        dto.setAvatarUrl(resolvedAvatar);
        dto.setName(entity.getName());
        dto.setType(entity.getType() != null ? entity.getType().name() : "EMPRESA");
        dto.setSector(entity.getSector() != null ? entity.getSector() : "Inovação Corporativa & B2B");
        dto.setLocation(entity.getLocation() != null ? entity.getLocation() : "Brasil · Nacional");
        dto.setCnpj(entity.getCnpj());
        String resolvedPhone = entity.getPhone();
        if ((resolvedPhone == null || resolvedPhone.isBlank()) && entity.getUser() != null) {
            resolvedPhone = entity.getUser().getPhone();
        }
        dto.setPhone(resolvedPhone);
        dto.setWebsite(entity.getWebsite());
        dto.setDescription(entity.getDescription());
        dto.setDemands(entity.getDemands() != null ? entity.getDemands() : entity.getDescription());
        dto.setTags(entity.getTags() != null ? entity.getTags() : "IA, SaaS, B2B, Dados");
        dto.setPreferredStage(entity.getPreferredStage() != null ? entity.getPreferredStage() : "Validação / Tração");
        dto.setOperationMode(entity.getOperationMode() != null ? entity.getOperationMode() : "POC, Compra, POC + Compra");
        return dto;
    }

    public void applyToEntity(Company entity) {
        if (entity == null) return;
        if (this.name != null && !this.name.isBlank()) entity.setName(this.name.trim());
        if (this.type != null && !this.type.isBlank()) entity.setType(CompanyType.fromString(this.type));
        if (this.sector != null) entity.setSector(this.sector);
        if (this.location != null) entity.setLocation(this.location);
        if (this.cnpj != null) entity.setCnpj(this.cnpj);
        if (this.phone != null) {
            entity.setPhone(this.phone);
            if (entity.getUser() != null) {
                entity.getUser().setPhone(this.phone);
            }
        }
        if (this.website != null) entity.setWebsite(this.website);
        if (this.description != null) entity.setDescription(this.description);
        if (this.demands != null) entity.setDemands(this.demands);
        if (this.tags != null) entity.setTags(this.tags);
        if (this.preferredStage != null) entity.setPreferredStage(this.preferredStage);
        if (this.operationMode != null) entity.setOperationMode(this.operationMode);
        if (this.avatarUrl != null) {
            entity.setAvatarUrl(this.avatarUrl);
            if (entity.getUser() != null) {
                entity.getUser().setAvatarUrl(this.avatarUrl);
            }
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDemands() { return demands; }
    public void setDemands(String demands) { this.demands = demands; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getPreferredStage() { return preferredStage; }
    public void setPreferredStage(String preferredStage) { this.preferredStage = preferredStage; }
    public String getOperationMode() { return operationMode; }
    public void setOperationMode(String operationMode) { this.operationMode = operationMode; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}
