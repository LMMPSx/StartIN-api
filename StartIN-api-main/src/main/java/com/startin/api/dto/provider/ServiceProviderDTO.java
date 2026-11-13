package com.startin.api.dto.provider;

import com.startin.api.model.ServiceProvider;
import java.util.Map;

public class ServiceProviderDTO {

    private Long id;
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private String providerType;
    private String category;
    private String description;
    private String location;
    private String portfolioUrl;
    private String linkedinUrl;
    private String availability;
    private String avatarUrl;
    private String photoUrl;

    // Etapa 02
    private String mainService;
    private String serviceExperience;
    private String toolsAndSkills;
    private String complementaryCategory;
    private String targetSegment;
    private String experienceYears;

    // Etapa 03
    private String projectName;
    private String projectLink;
    private String projectDescription;
    private String projectYear;
    private String projectRepoUrl;
    private String projectTools;
    private String projectType;
    private String projectCoverUrl;

    // Etapa 04
    private String interestSectors;
    private String connectionPreference;
    private String opportunitiesSought;
    private String interestRegion;
    private String targetStartupSize;
    private String communicationLanguage;

    private String tags;
    private String savedStartupIds;
    private Integer matchScore;
    private String justification;
    private Map<String, String> matchReasons;

    private Boolean hasConnection;
    private Long connectionId;
    private String connectionStatus;
    private String connectionSenderType;

    public ServiceProviderDTO() {
    }

    public static ServiceProviderDTO fromEntity(ServiceProvider entity) {
        if (entity == null) return null;
        ServiceProviderDTO dto = new ServiceProviderDTO();
        dto.setId(entity.getId());
        if (entity.getUser() != null) {
            dto.setUserId(entity.getUser().getId());
            dto.setEmail(entity.getUser().getEmail());
            dto.setPhone(entity.getUser().getPhone());
        }
        dto.setName(entity.getName());
        dto.setProviderType(entity.getProviderType());
        dto.setCategory(entity.getCategory());
        dto.setDescription(entity.getDescription());
        dto.setLocation(entity.getLocation());
        dto.setPortfolioUrl(entity.getPortfolioUrl());
        dto.setLinkedinUrl(entity.getLinkedinUrl());
        dto.setAvailability(entity.getAvailability());
        dto.setAvatarUrl(entity.getAvatarUrl() != null ? entity.getAvatarUrl() : (entity.getUser() != null ? entity.getUser().getAvatarUrl() : null));

        dto.setMainService(entity.getMainService());
        dto.setServiceExperience(entity.getServiceExperience());
        dto.setToolsAndSkills(entity.getToolsAndSkills());
        dto.setComplementaryCategory(entity.getComplementaryCategory());
        dto.setTargetSegment(entity.getTargetSegment());
        dto.setExperienceYears(entity.getExperienceYears());

        dto.setProjectName(entity.getProjectName());
        dto.setProjectLink(entity.getProjectLink());
        dto.setProjectDescription(entity.getProjectDescription());
        dto.setProjectYear(entity.getProjectYear());
        dto.setProjectRepoUrl(entity.getProjectRepoUrl());
        dto.setProjectTools(entity.getProjectTools());
        dto.setProjectType(entity.getProjectType());
        dto.setProjectCoverUrl(entity.getProjectCoverUrl());

        dto.setInterestSectors(entity.getInterestSectors());
        dto.setConnectionPreference(entity.getConnectionPreference());
        dto.setOpportunitiesSought(entity.getOpportunitiesSought());
        dto.setInterestRegion(entity.getInterestRegion());
        dto.setTargetStartupSize(entity.getTargetStartupSize());
        dto.setCommunicationLanguage(entity.getCommunicationLanguage());

        dto.setTags(entity.getTags());
        dto.setSavedStartupIds(entity.getSavedStartupIds());
        dto.setMatchScore(entity.getMatchScore());
        return dto;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getProviderType() { return providerType; }
    public void setProviderType(String providerType) { this.providerType = providerType; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getPortfolioUrl() { return portfolioUrl; }
    public void setPortfolioUrl(String portfolioUrl) { this.portfolioUrl = portfolioUrl; }
    public String getLinkedinUrl() { return linkedinUrl; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }
    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }
    public String getAvatarUrl() {
        return (photoUrl != null) ? photoUrl : avatarUrl;
    }
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
    public String getPhotoUrl() {
        return (photoUrl != null) ? photoUrl : avatarUrl;
    }
    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
        this.avatarUrl = photoUrl;
    }

    public String getMainService() { return mainService; }
    public void setMainService(String mainService) { this.mainService = mainService; }
    public String getServiceExperience() { return serviceExperience; }
    public void setServiceExperience(String serviceExperience) { this.serviceExperience = serviceExperience; }
    public String getToolsAndSkills() { return toolsAndSkills; }
    public void setToolsAndSkills(String toolsAndSkills) { this.toolsAndSkills = toolsAndSkills; }
    public String getComplementaryCategory() { return complementaryCategory; }
    public void setComplementaryCategory(String complementaryCategory) { this.complementaryCategory = complementaryCategory; }
    public String getTargetSegment() { return targetSegment; }
    public void setTargetSegment(String targetSegment) { this.targetSegment = targetSegment; }
    public String getExperienceYears() { return experienceYears; }
    public void setExperienceYears(String experienceYears) { this.experienceYears = experienceYears; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getProjectLink() { return projectLink; }
    public void setProjectLink(String projectLink) { this.projectLink = projectLink; }
    public String getProjectDescription() { return projectDescription; }
    public void setProjectDescription(String projectDescription) { this.projectDescription = projectDescription; }
    public String getProjectYear() { return projectYear; }
    public void setProjectYear(String projectYear) { this.projectYear = projectYear; }
    public String getProjectRepoUrl() { return projectRepoUrl; }
    public void setProjectRepoUrl(String projectRepoUrl) { this.projectRepoUrl = projectRepoUrl; }
    public String getProjectTools() { return projectTools; }
    public void setProjectTools(String projectTools) { this.projectTools = projectTools; }
    public String getProjectType() { return projectType; }
    public void setProjectType(String projectType) { this.projectType = projectType; }
    public String getProjectCoverUrl() { return projectCoverUrl; }
    public void setProjectCoverUrl(String projectCoverUrl) { this.projectCoverUrl = projectCoverUrl; }

    public String getInterestSectors() { return interestSectors; }
    public void setInterestSectors(String interestSectors) { this.interestSectors = interestSectors; }
    public String getConnectionPreference() { return connectionPreference; }
    public void setConnectionPreference(String connectionPreference) { this.connectionPreference = connectionPreference; }
    public String getOpportunitiesSought() { return opportunitiesSought; }
    public void setOpportunitiesSought(String opportunitiesSought) { this.opportunitiesSought = opportunitiesSought; }
    public String getInterestRegion() { return interestRegion; }
    public void setInterestRegion(String interestRegion) { this.interestRegion = interestRegion; }
    public String getTargetStartupSize() { return targetStartupSize; }
    public void setTargetStartupSize(String targetStartupSize) { this.targetStartupSize = targetStartupSize; }
    public String getCommunicationLanguage() { return communicationLanguage; }
    public void setCommunicationLanguage(String communicationLanguage) { this.communicationLanguage = communicationLanguage; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getSavedStartupIds() { return savedStartupIds; }
    public void setSavedStartupIds(String savedStartupIds) { this.savedStartupIds = savedStartupIds; }
    public Integer getMatchScore() { return matchScore; }
    public void setMatchScore(Integer matchScore) { this.matchScore = matchScore; }
    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }
    public Map<String, String> getMatchReasons() { return matchReasons; }
    public void setMatchReasons(Map<String, String> matchReasons) { this.matchReasons = matchReasons; }

    public Boolean getHasConnection() { return hasConnection; }
    public void setHasConnection(Boolean hasConnection) { this.hasConnection = hasConnection; }
    public Long getConnectionId() { return connectionId; }
    public void setConnectionId(Long connectionId) { this.connectionId = connectionId; }
    public String getConnectionStatus() { return connectionStatus; }
    public void setConnectionStatus(String connectionStatus) { this.connectionStatus = connectionStatus; }
    public String getConnectionSenderType() { return connectionSenderType; }
    public void setConnectionSenderType(String connectionSenderType) { this.connectionSenderType = connectionSenderType; }
}
