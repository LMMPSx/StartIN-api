package com.startin.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_providers")
public class ServiceProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(nullable = false)
    private String name;

    private String providerType = "PROFISSIONAL"; // PROFISSIONAL, EMPRESA

    private String category = "Design"; // Design, Tecnologia, Consultoria, etc.

    @Column(columnDefinition = "TEXT")
    private String description;

    private String location = "Recife, PE";
    private String portfolioUrl;
    private String linkedinUrl;
    private String availability = "Disponível";

    @Column(columnDefinition = "LONGTEXT")
    private String avatarUrl;

    // Etapa 02 — Serviços e competências
    @Column(columnDefinition = "TEXT")
    private String mainService; // ex: "Design de produto e UX/UI"
    @Column(columnDefinition = "TEXT")
    private String serviceExperience = "";
    @Column(columnDefinition = "TEXT")
    private String toolsAndSkills; // ex: "Figma, React, pesquisa UX e prototipação"
    private String complementaryCategory; // ex: "Front-end"
    @Column(columnDefinition = "TEXT")
    private String targetSegment; // ex: "Tecnologia"
    private String experienceYears; // ex: "3 anos"

    // Etapa 03 — Trabalhos e casos de sucesso (Portfólio)
    private String projectName; // ex: "Aplicativo de gestão"
    private String projectLink;
    @Column(columnDefinition = "TEXT")
    private String projectDescription;
    private String projectYear = "2026";
    private String projectRepoUrl;
    @Column(columnDefinition = "TEXT")
    private String projectTools;
    private String projectType;
    @Column(columnDefinition = "LONGTEXT")
    private String projectCoverUrl;

    // Etapa 04 — Preferências de conexão
    @Column(columnDefinition = "TEXT")
    private String interestSectors; // ex: "Saúde, educação e tecnologia"
    @Column(columnDefinition = "TEXT")
    private String connectionPreference; // ex: "Startups em estágio inicial"
    @Column(columnDefinition = "TEXT")
    private String opportunitiesSought;
    private String interestRegion = "Brasil";
    private String targetStartupSize = "Qualquer porte";
    private String communicationLanguage = "Português";

    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(name = "saved_startup_ids", columnDefinition = "TEXT")
    private String savedStartupIds;

    private Integer matchScore = 90;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ServiceProvider() {
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

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

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
