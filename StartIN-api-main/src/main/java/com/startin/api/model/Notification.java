package com.startin.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String timeAgo;

    private String type = "TODAS"; // TODAS, Oportunidades, Matches, Sistema

    private Boolean readStatus = false;

    @Column(length = 30)
    private String recipientRole = "STARTUP"; // STARTUP ou COMPANY

    private Long recipientUserId;

    private Long startupId;

    private Long companyId;

    private Long providerId;

    @Column(length = 80)
    private String targetScreen;

    @Column(length = 80)
    private String targetId;

    private LocalDateTime createdAt;

    public Notification() {
    }

    public Notification(Long id, String title, String description, String timeAgo, String type, Boolean readStatus) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.timeAgo = timeAgo;
        this.type = type != null ? type : "TODAS";
        this.readStatus = readStatus != null ? readStatus : false;
        this.recipientRole = "STARTUP";
    }

    public static Notification forStartup(
            Long startupId,
            Long userId,
            String title,
            String description,
            String type,
            String targetScreen,
            String targetId) {
        Notification n = new Notification();
        n.setRecipientRole("STARTUP");
        n.setStartupId(startupId);
        n.setRecipientUserId(userId);
        n.setTitle(title);
        n.setDescription(description);
        n.setTimeAgo("Agora");
        n.setType(type != null ? type : "Sistema");
        n.setTargetScreen(targetScreen);
        n.setTargetId(targetId);
        n.setReadStatus(false);
        return n;
    }

    public static Notification forProvider(
            Long providerId,
            Long userId,
            String title,
            String description,
            String type,
            String targetScreen,
            String targetId) {
        Notification n = new Notification();
        n.setRecipientRole("PROVIDER");
        n.setProviderId(providerId);
        n.setRecipientUserId(userId);
        n.setTitle(title);
        n.setDescription(description);
        n.setTimeAgo("Agora");
        n.setType(type != null ? type : "Sistema");
        n.setTargetScreen(targetScreen);
        n.setTargetId(targetId);
        n.setReadStatus(false);
        return n;
    }

    public static Notification forCompany(
            Long companyId,
            Long userId,
            String title,
            String description,
            String type,
            String targetScreen,
            String targetId) {
        Notification n = new Notification();
        n.setRecipientRole("COMPANY");
        n.setCompanyId(companyId);
        n.setRecipientUserId(userId);
        n.setTitle(title);
        n.setDescription(description);
        n.setTimeAgo("Agora");
        n.setType(type != null ? type : "Sistema");
        n.setTargetScreen(targetScreen);
        n.setTargetId(targetId);
        n.setReadStatus(false);
        return n;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.recipientRole == null || this.recipientRole.isBlank()) {
            this.recipientRole = "STARTUP";
        }
        if (this.readStatus == null) {
            this.readStatus = false;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTimeAgo() {
        return timeAgo;
    }

    public void setTimeAgo(String timeAgo) {
        this.timeAgo = timeAgo;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getReadStatus() {
        return readStatus;
    }

    public void setReadStatus(Boolean readStatus) {
        this.readStatus = readStatus;
    }

    public String getRecipientRole() {
        return recipientRole;
    }

    public void setRecipientRole(String recipientRole) {
        this.recipientRole = recipientRole;
    }

    public Long getRecipientUserId() {
        return recipientUserId;
    }

    public void setRecipientUserId(Long recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public Long getStartupId() {
        return startupId;
    }

    public void setStartupId(Long startupId) {
        this.startupId = startupId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Long getProviderId() {
        return providerId;
    }

    public void setProviderId(Long providerId) {
        this.providerId = providerId;
    }

    public String getTargetScreen() {
        return targetScreen;
    }

    public void setTargetScreen(String targetScreen) {
        this.targetScreen = targetScreen;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
