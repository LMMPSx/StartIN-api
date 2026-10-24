package com.startin.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "startup_profiles")
public class StartupProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    // Etapa 1: Identidade
    @Column(nullable = false)
    private String name = "Minha Startup";

    @Column(length = 150)
    private String mainArea;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 150)
    private String location;

    @Column(length = 30)
    private String cnpj;

    private String website;

    @Column(length = 20)
    private String foundedYear;

    // Etapa 2: Solução
    @Column(columnDefinition = "TEXT")
    private String problem;

    @Column(columnDefinition = "TEXT")
    private String howItWorks;

    @Column(columnDefinition = "TEXT")
    private String differentiator;

    @Column(columnDefinition = "TEXT")
    private String impact;

    @Column(columnDefinition = "TEXT")
    private String technologies;

    @Column(length = 150)
    private String businessModel;

    // Etapa 3: Mercado
    private String targetAudience;

    @Column(length = 150)
    private String mainSector;

    @Column(length = 150)
    private String operationRegion;

    @Column(length = 100)
    private String state;

    private String customerProfile;

    @Column(length = 150)
    private String marketSize;

    @Column(columnDefinition = "TEXT")
    private String competitors;

    @Column(length = 150)
    private String mainChannel;

    // Etapa 4: Momento
    @Column(length = 100)
    private String currentStage;

    @Column(length = 150)
    private String tractionCustomers;

    @Column(length = 150)
    private String productMaturity;

    @Column(length = 100)
    private String teamSize;

    @Column(length = 150)
    private String annualRevenue;

    @Column(length = 150)
    private String recentGrowth;

    @Column(length = 150)
    private String investmentRaised;

    // Etapa 5: Objetivos
    @Column(columnDefinition = "TEXT")
    private String mainObjective;

    @Column(length = 150)
    private String investmentNeed;

    @Column(length = 100)
    private String executionDeadline;

    @Column(columnDefinition = "TEXT")
    private String resourceUse;

    @Column(columnDefinition = "TEXT")
    private String twelveMonthGoal;

    private String strategicPriority;

    @Column(columnDefinition = "TEXT")
    private String desiredPartners;

    // Etapa 6: Preferências de Matching
    @Column(columnDefinition = "TEXT")
    private String areasOfInterest;

    private String opportunityTypes;
    private Boolean receiveAiRecommendations = true;
    private Boolean emailNotifications = true;
    private Boolean matchAlerts = true;

    @Column(columnDefinition = "LONGTEXT")
    private String avatarUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public StartupProfile() {
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
    public String getMainArea() { return mainArea; }
    public void setMainArea(String mainArea) { this.mainArea = mainArea; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public String getFoundedYear() { return foundedYear; }
    public void setFoundedYear(String foundedYear) { this.foundedYear = foundedYear; }
    public String getProblem() { return problem; }
    public void setProblem(String problem) { this.problem = problem; }
    public String getHowItWorks() { return howItWorks; }
    public void setHowItWorks(String howItWorks) { this.howItWorks = howItWorks; }
    public String getDifferentiator() { return differentiator; }
    public void setDifferentiator(String differentiator) { this.differentiator = differentiator; }
    public String getImpact() { return impact; }
    public void setImpact(String impact) { this.impact = impact; }
    public String getTechnologies() { return technologies; }
    public void setTechnologies(String technologies) { this.technologies = technologies; }
    public String getBusinessModel() { return businessModel; }
    public void setBusinessModel(String businessModel) { this.businessModel = businessModel; }
    public String getTargetAudience() { return targetAudience; }
    public void setTargetAudience(String targetAudience) { this.targetAudience = targetAudience; }
    public String getMainSector() { return mainSector; }
    public void setMainSector(String mainSector) { this.mainSector = mainSector; }
    public String getOperationRegion() { return operationRegion; }
    public void setOperationRegion(String operationRegion) { this.operationRegion = operationRegion; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getCustomerProfile() { return customerProfile; }
    public void setCustomerProfile(String customerProfile) { this.customerProfile = customerProfile; }
    public String getMarketSize() { return marketSize; }
    public void setMarketSize(String marketSize) { this.marketSize = marketSize; }
    public String getCompetitors() { return competitors; }
    public void setCompetitors(String competitors) { this.competitors = competitors; }
    public String getMainChannel() { return mainChannel; }
    public void setMainChannel(String mainChannel) { this.mainChannel = mainChannel; }
    public String getCurrentStage() { return currentStage; }
    public void setCurrentStage(String currentStage) { this.currentStage = currentStage; }
    public String getTractionCustomers() { return tractionCustomers; }
    public void setTractionCustomers(String tractionCustomers) { this.tractionCustomers = tractionCustomers; }
    public String getProductMaturity() { return productMaturity; }
    public void setProductMaturity(String productMaturity) { this.productMaturity = productMaturity; }
    public String getTeamSize() { return teamSize; }
    public void setTeamSize(String teamSize) { this.teamSize = teamSize; }
    public String getAnnualRevenue() { return annualRevenue; }
    public void setAnnualRevenue(String annualRevenue) { this.annualRevenue = annualRevenue; }
    public String getRecentGrowth() { return recentGrowth; }
    public void setRecentGrowth(String recentGrowth) { this.recentGrowth = recentGrowth; }
    public String getInvestmentRaised() { return investmentRaised; }
    public void setInvestmentRaised(String investmentRaised) { this.investmentRaised = investmentRaised; }
    public String getMainObjective() { return mainObjective; }
    public void setMainObjective(String mainObjective) { this.mainObjective = mainObjective; }
    public String getInvestmentNeed() { return investmentNeed; }
    public void setInvestmentNeed(String investmentNeed) { this.investmentNeed = investmentNeed; }
    public String getExecutionDeadline() { return executionDeadline; }
    public void setExecutionDeadline(String executionDeadline) { this.executionDeadline = executionDeadline; }
    public String getResourceUse() { return resourceUse; }
    public void setResourceUse(String resourceUse) { this.resourceUse = resourceUse; }
    public String getTwelveMonthGoal() { return twelveMonthGoal; }
    public void setTwelveMonthGoal(String twelveMonthGoal) { this.twelveMonthGoal = twelveMonthGoal; }
    public String getStrategicPriority() { return strategicPriority; }
    public void setStrategicPriority(String strategicPriority) { this.strategicPriority = strategicPriority; }
    public String getDesiredPartners() { return desiredPartners; }
    public void setDesiredPartners(String desiredPartners) { this.desiredPartners = desiredPartners; }
    public String getAreasOfInterest() { return areasOfInterest; }
    public void setAreasOfInterest(String areasOfInterest) { this.areasOfInterest = areasOfInterest; }
    public String getOpportunityTypes() { return opportunityTypes; }
    public void setOpportunityTypes(String opportunityTypes) { this.opportunityTypes = opportunityTypes; }
    public Boolean getReceiveAiRecommendations() { return receiveAiRecommendations; }
    public void setReceiveAiRecommendations(Boolean receiveAiRecommendations) { this.receiveAiRecommendations = receiveAiRecommendations; }
    public Boolean getEmailNotifications() { return emailNotifications; }
    public void setEmailNotifications(Boolean emailNotifications) { this.emailNotifications = emailNotifications; }
    public Boolean getMatchAlerts() { return matchAlerts; }
    public void setMatchAlerts(Boolean matchAlerts) { this.matchAlerts = matchAlerts; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
