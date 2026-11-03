package com.startin.api.dto.notification;

import com.startin.api.model.Notification;

public class NotificationDTO {

    private Long id;
    private String title;
    private String description;
    private String timeAgo;
    private String type;
    private Boolean read;
    private String recipientRole;
    private Long startupId;
    private Long companyId;
    private String targetScreen;
    private String targetId;

    public NotificationDTO() {
    }

    public static NotificationDTO fromEntity(Notification entity) {
        if (entity == null) return null;
        NotificationDTO dto = new NotificationDTO();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setTimeAgo(entity.getTimeAgo() != null ? entity.getTimeAgo() : "Agora");
        dto.setType(entity.getType() != null ? entity.getType() : "Sistema");
        dto.setRead(Boolean.TRUE.equals(entity.getReadStatus()));
        dto.setRecipientRole(entity.getRecipientRole() != null ? entity.getRecipientRole() : "STARTUP");
        dto.setStartupId(entity.getStartupId());
        dto.setCompanyId(entity.getCompanyId());
        dto.setTargetScreen(resolveTargetScreen(entity));
        dto.setTargetId(entity.getTargetId());
        return dto;
    }

    private static String resolveTargetScreen(Notification entity) {
        if (entity.getTargetScreen() != null && !entity.getTargetScreen().isBlank()) {
            return entity.getTargetScreen();
        }
        boolean isCompany = "COMPANY".equalsIgnoreCase(entity.getRecipientRole());
        String combined = ((entity.getTitle() != null ? entity.getTitle() : "") + " " +
                (entity.getType() != null ? entity.getType() : "")).toLowerCase();
        if (isCompany) {
            if (combined.contains("oportunidade") || combined.contains("poc") || combined.contains("compra")) {
                return "my-opportunities";
            }
            if (combined.contains("perfil") || combined.contains("empresa")) {
                return "company-profile";
            }
            return "search-startups";
        }
        if (combined.contains("empresa")) {
            return "search-companies";
        }
        if (combined.contains("interesse registrado") || combined.contains("salvo")) {
            return "saved-opportunities";
        }
        if (combined.contains("perfil") || combined.contains("preferência") || combined.contains("conta")) {
            return "account";
        }
        return "for-you";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTimeAgo() { return timeAgo; }
    public void setTimeAgo(String timeAgo) { this.timeAgo = timeAgo; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Boolean getRead() { return read; }
    public void setRead(Boolean read) { this.read = read; }
    public String getRecipientRole() { return recipientRole; }
    public void setRecipientRole(String recipientRole) { this.recipientRole = recipientRole; }
    public Long getStartupId() { return startupId; }
    public void setStartupId(Long startupId) { this.startupId = startupId; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public String getTargetScreen() { return targetScreen; }
    public void setTargetScreen(String targetScreen) { this.targetScreen = targetScreen; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
}
