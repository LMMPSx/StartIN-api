package com.startin.api.dto.notification;

import com.startin.api.model.Notification;

public class NotificationDTO {

    private Long id;
    private String title;
    private String description;
    private String timeAgo;
    private String type;
    private Boolean read;

    public NotificationDTO() {
    }

    public static NotificationDTO fromEntity(Notification entity) {
        if (entity == null) return null;
        NotificationDTO dto = new NotificationDTO();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setTimeAgo(entity.getTimeAgo());
        dto.setType(entity.getType());
        dto.setRead(entity.getReadStatus());
        return dto;
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
}
