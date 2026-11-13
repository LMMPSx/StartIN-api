package com.startin.api.dto.provider;

import com.startin.api.model.ServiceConnection;
import com.startin.api.model.ServiceProvider;
import com.startin.api.model.StartupProfile;
import com.startin.api.model.User;
import java.time.LocalDateTime;

public class ServiceConnectionDTO {

    private Long id;

    // Dados da Startup
    private Long startupId;
    private String startupName;
    private String startupSegment;
    private String startupStage;
    private String startupAvatarUrl;
    private String startupEmail;
    private String startupPhone;
    private String startupWebsite;
    private String startupLocation;

    // Dados do Prestador
    private Long providerId;
    private String providerName;
    private String providerCategory;
    private String providerMainService;
    private String providerAvatarUrl;
    private String providerEmail;
    private String providerPhone;
    private String providerLinkedinUrl;
    private String providerPortfolioUrl;
    private String providerLocation;

    private String senderType; // STARTUP ou PROVIDER
    private String message;
    private String status; // PENDING, ACCEPTED, REJECTED
    private Integer matchScore;
    private LocalDateTime createdAt;

    public ServiceConnectionDTO() {
    }

    public static ServiceConnectionDTO fromEntity(ServiceConnection entity) {
        if (entity == null) return null;
        ServiceConnectionDTO dto = new ServiceConnectionDTO();
        dto.setId(entity.getId());

        StartupProfile st = entity.getStartup();
        if (st != null) {
            dto.setStartupId(st.getId());
            dto.setStartupName(st.getName());
            dto.setStartupSegment(st.getMainArea() != null ? st.getMainArea() : st.getMainSector());
            dto.setStartupStage(st.getCurrentStage() != null ? st.getCurrentStage() : "Tração");
            dto.setStartupWebsite(st.getWebsite());
            dto.setStartupLocation(st.getLocation());
            User stUser = st.getUser();
            dto.setStartupAvatarUrl(st.getAvatarUrl() != null ? st.getAvatarUrl() : (stUser != null ? stUser.getAvatarUrl() : null));
            dto.setStartupEmail(stUser != null ? stUser.getEmail() : null);
            dto.setStartupPhone(st.getPhone() != null && !st.getPhone().isBlank() ? st.getPhone() : (stUser != null ? stUser.getPhone() : null));
        }

        ServiceProvider pr = entity.getProvider();
        if (pr != null) {
            dto.setProviderId(pr.getId());
            dto.setProviderName(pr.getName());
            dto.setProviderCategory(pr.getCategory());
            dto.setProviderMainService(pr.getMainService());
            dto.setProviderLinkedinUrl(pr.getLinkedinUrl());
            dto.setProviderPortfolioUrl(pr.getPortfolioUrl());
            dto.setProviderLocation(pr.getLocation());
            User prUser = pr.getUser();
            dto.setProviderAvatarUrl(pr.getAvatarUrl() != null ? pr.getAvatarUrl() : (prUser != null ? prUser.getAvatarUrl() : null));
            dto.setProviderEmail(prUser != null ? prUser.getEmail() : null);
            dto.setProviderPhone(prUser != null ? prUser.getPhone() : null);
        }

        dto.setSenderType(entity.getSenderType());
        dto.setMessage(entity.getMessage());
        dto.setStatus(entity.getStatus());
        dto.setMatchScore(entity.getMatchScore());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStartupId() { return startupId; }
    public void setStartupId(Long startupId) { this.startupId = startupId; }

    public String getStartupName() { return startupName; }
    public void setStartupName(String startupName) { this.startupName = startupName; }

    public String getStartupSegment() { return startupSegment; }
    public void setStartupSegment(String startupSegment) { this.startupSegment = startupSegment; }

    public String getStartupStage() { return startupStage; }
    public void setStartupStage(String startupStage) { this.startupStage = startupStage; }

    public String getStartupAvatarUrl() { return startupAvatarUrl; }
    public void setStartupAvatarUrl(String startupAvatarUrl) { this.startupAvatarUrl = startupAvatarUrl; }

    public String getStartupEmail() { return startupEmail; }
    public void setStartupEmail(String startupEmail) { this.startupEmail = startupEmail; }

    public String getStartupPhone() { return startupPhone; }
    public void setStartupPhone(String startupPhone) { this.startupPhone = startupPhone; }

    public String getStartupWebsite() { return startupWebsite; }
    public void setStartupWebsite(String startupWebsite) { this.startupWebsite = startupWebsite; }

    public String getStartupLocation() { return startupLocation; }
    public void setStartupLocation(String startupLocation) { this.startupLocation = startupLocation; }

    public Long getProviderId() { return providerId; }
    public void setProviderId(Long providerId) { this.providerId = providerId; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public String getProviderCategory() { return providerCategory; }
    public void setProviderCategory(String providerCategory) { this.providerCategory = providerCategory; }

    public String getProviderMainService() { return providerMainService; }
    public void setProviderMainService(String providerMainService) { this.providerMainService = providerMainService; }

    public String getProviderAvatarUrl() { return providerAvatarUrl; }
    public void setProviderAvatarUrl(String providerAvatarUrl) { this.providerAvatarUrl = providerAvatarUrl; }

    public String getProviderEmail() { return providerEmail; }
    public void setProviderEmail(String providerEmail) { this.providerEmail = providerEmail; }

    public String getProviderPhone() { return providerPhone; }
    public void setProviderPhone(String providerPhone) { this.providerPhone = providerPhone; }

    public String getProviderLinkedinUrl() { return providerLinkedinUrl; }
    public void setProviderLinkedinUrl(String providerLinkedinUrl) { this.providerLinkedinUrl = providerLinkedinUrl; }

    public String getProviderPortfolioUrl() { return providerPortfolioUrl; }
    public void setProviderPortfolioUrl(String providerPortfolioUrl) { this.providerPortfolioUrl = providerPortfolioUrl; }

    public String getProviderLocation() { return providerLocation; }
    public void setProviderLocation(String providerLocation) { this.providerLocation = providerLocation; }

    public String getSenderType() { return senderType; }
    public void setSenderType(String senderType) { this.senderType = senderType; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getMatchScore() { return matchScore; }
    public void setMatchScore(Integer matchScore) { this.matchScore = matchScore; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
