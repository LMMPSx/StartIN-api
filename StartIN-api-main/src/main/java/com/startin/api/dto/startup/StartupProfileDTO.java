package com.startin.api.dto.startup;

import com.startin.api.model.StartupProfile;

public class StartupProfileDTO {

    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;

    // Etapa 1: Identidade
    private String name;
    private String mainArea;
    private String description;
    private String location;
    private String cnpj;
    private String phone;
    private String website;
    private String foundedYear;

    // Etapa 2: Solução
    private String problem;
    private String howItWorks;
    private String differentiator;
    private String impact;
    private String technologies;
    private String businessModel;

    // Etapa 3: Mercado
    private String targetAudience;
    private String mainSector;
    private String operationRegion;
    private String state;
    private String customerProfile;
    private String marketSize;
    private String competitors;
    private String mainChannel;

    // Etapa 4: Momento
    private String currentStage;
    private String tractionCustomers;
    private String productMaturity;
    private String teamSize;
    private String annualRevenue;
    private String recentGrowth;
    private String investmentRaised;
    private Integer trlScore;
    private Boolean tractionAttested;
    private String tractionAttestationInfo;
    private Integer cumulativeOpportunitiesFound;
    private Integer lastUpdateNewMatchesCount;

    // Etapa 5: Objetivos
    private String mainObjective;
    private String investmentNeed;
    private String executionDeadline;
    private String resourceUse;
    private String twelveMonthGoal;
    private String strategicPriority;
    private String desiredPartners;

    // Etapa 6: Preferências de Matching
    private String areasOfInterest;
    private String opportunityTypes;
    private Boolean receiveAiRecommendations;
    private Boolean emailNotifications;
    private Boolean matchAlerts;
    private String avatarUrl;

    public StartupProfileDTO() {
    }

    public static StartupProfileDTO fromEntity(StartupProfile entity) {
        if (entity == null) return null;
        StartupProfileDTO dto = new StartupProfileDTO();
        dto.setId(entity.getId());
        if (entity.getUser() != null) {
            dto.setUserId(entity.getUser().getId());
            dto.setUserName(entity.getUser().getName());
            dto.setUserEmail(entity.getUser().getEmail());
        }
        String resolvedAvatar = entity.getAvatarUrl();
        if ((resolvedAvatar == null || resolvedAvatar.isBlank()) && entity.getUser() != null) {
            resolvedAvatar = entity.getUser().getAvatarUrl();
        }
        dto.setAvatarUrl(resolvedAvatar);
        dto.setName(entity.getName());
        dto.setMainArea(entity.getMainArea());
        dto.setDescription(entity.getDescription());
        dto.setLocation(entity.getLocation());
        String resolvedCnpj = entity.getCnpj();
        if ((resolvedCnpj == null || resolvedCnpj.isBlank()) && entity.getUser() != null) {
            resolvedCnpj = entity.getUser().getCnpj();
        }
        dto.setCnpj(resolvedCnpj);
        String resolvedPhone = entity.getPhone();
        if ((resolvedPhone == null || resolvedPhone.isBlank()) && entity.getUser() != null) {
            resolvedPhone = entity.getUser().getPhone();
        }
        dto.setPhone(resolvedPhone);
        dto.setWebsite(entity.getWebsite());
        dto.setFoundedYear(entity.getFoundedYear());

        dto.setProblem(entity.getProblem());
        dto.setHowItWorks(entity.getHowItWorks());
        dto.setDifferentiator(entity.getDifferentiator());
        dto.setImpact(entity.getImpact());
        dto.setTechnologies(entity.getTechnologies());
        dto.setBusinessModel(entity.getBusinessModel());

        dto.setTargetAudience(entity.getTargetAudience());
        dto.setMainSector(entity.getMainSector());
        dto.setOperationRegion(entity.getOperationRegion());
        dto.setState(entity.getState());
        dto.setCustomerProfile(entity.getCustomerProfile());
        dto.setMarketSize(entity.getMarketSize());
        dto.setCompetitors(entity.getCompetitors());
        dto.setMainChannel(entity.getMainChannel());

        dto.setCurrentStage(entity.getCurrentStage());
        dto.setTractionCustomers(entity.getTractionCustomers());
        dto.setProductMaturity(entity.getProductMaturity());
        dto.setTeamSize(entity.getTeamSize());
        dto.setAnnualRevenue(entity.getAnnualRevenue());
        dto.setRecentGrowth(entity.getRecentGrowth());
        dto.setInvestmentRaised(entity.getInvestmentRaised());
        dto.setTrlScore(entity.getTrlScore());
        dto.setTractionAttested(entity.getTractionAttested() != null ? entity.getTractionAttested() : false);
        dto.setTractionAttestationInfo(entity.getTractionAttestationInfo());
        dto.setCumulativeOpportunitiesFound(entity.getCumulativeOpportunitiesFound() != null ? entity.getCumulativeOpportunitiesFound() : 0);
        dto.setLastUpdateNewMatchesCount(entity.getLastUpdateNewMatchesCount() != null ? entity.getLastUpdateNewMatchesCount() : 0);

        dto.setMainObjective(entity.getMainObjective());
        dto.setInvestmentNeed(entity.getInvestmentNeed());
        dto.setExecutionDeadline(entity.getExecutionDeadline());
        dto.setResourceUse(entity.getResourceUse());
        dto.setTwelveMonthGoal(entity.getTwelveMonthGoal());
        dto.setStrategicPriority(entity.getStrategicPriority());
        dto.setDesiredPartners(entity.getDesiredPartners());

        dto.setAreasOfInterest(entity.getAreasOfInterest());
        dto.setOpportunityTypes(entity.getOpportunityTypes());
        dto.setReceiveAiRecommendations(entity.getReceiveAiRecommendations());
        dto.setEmailNotifications(entity.getEmailNotifications() != null ? entity.getEmailNotifications() : true);
        dto.setMatchAlerts(entity.getMatchAlerts() != null ? entity.getMatchAlerts() : true);

        return dto;
    }

    public void applyToEntity(StartupProfile entity) {
        if (entity == null) return;
        if (this.name != null) {
            entity.setName(this.name);
            if (entity.getUser() != null) {
                entity.getUser().setName(this.name);
            }
        }
        if (this.mainArea != null) entity.setMainArea(this.mainArea);
        if (this.description != null) entity.setDescription(this.description);
        if (this.location != null) entity.setLocation(this.location);
        if (this.cnpj != null) {
            entity.setCnpj(this.cnpj);
            if (entity.getUser() != null) {
                entity.getUser().setCnpj(this.cnpj);
            }
        }
        if (this.phone != null) {
            entity.setPhone(this.phone);
            if (entity.getUser() != null) {
                entity.getUser().setPhone(this.phone);
            }
        }
        if (this.website != null) entity.setWebsite(this.website);
        if (this.foundedYear != null) entity.setFoundedYear(this.foundedYear);

        if (this.problem != null) entity.setProblem(this.problem);
        if (this.howItWorks != null) entity.setHowItWorks(this.howItWorks);
        if (this.differentiator != null) entity.setDifferentiator(this.differentiator);
        if (this.impact != null) entity.setImpact(this.impact);
        if (this.technologies != null) entity.setTechnologies(this.technologies);
        if (this.businessModel != null) entity.setBusinessModel(this.businessModel);

        if (this.targetAudience != null) entity.setTargetAudience(this.targetAudience);
        if (this.mainSector != null) entity.setMainSector(this.mainSector);
        if (this.operationRegion != null) entity.setOperationRegion(this.operationRegion);
        if (this.state != null) entity.setState(this.state);
        if (this.customerProfile != null) entity.setCustomerProfile(this.customerProfile);
        if (this.marketSize != null) entity.setMarketSize(this.marketSize);
        if (this.competitors != null) entity.setCompetitors(this.competitors);
        if (this.mainChannel != null) entity.setMainChannel(this.mainChannel);

        if (this.currentStage != null) entity.setCurrentStage(this.currentStage);
        if (this.tractionCustomers != null) entity.setTractionCustomers(this.tractionCustomers);
        if (this.productMaturity != null) entity.setProductMaturity(this.productMaturity);
        if (this.teamSize != null) entity.setTeamSize(this.teamSize);
        if (this.annualRevenue != null) entity.setAnnualRevenue(this.annualRevenue);
        if (this.recentGrowth != null) entity.setRecentGrowth(this.recentGrowth);
        if (this.investmentRaised != null) entity.setInvestmentRaised(this.investmentRaised);
        if (this.trlScore != null) entity.setTrlScore(this.trlScore);
        if (this.tractionAttested != null) entity.setTractionAttested(this.tractionAttested);
        if (this.tractionAttestationInfo != null) entity.setTractionAttestationInfo(this.tractionAttestationInfo);

        if (this.mainObjective != null) entity.setMainObjective(this.mainObjective);
        if (this.investmentNeed != null) entity.setInvestmentNeed(this.investmentNeed);
        if (this.executionDeadline != null) entity.setExecutionDeadline(this.executionDeadline);
        if (this.resourceUse != null) entity.setResourceUse(this.resourceUse);
        if (this.twelveMonthGoal != null) entity.setTwelveMonthGoal(this.twelveMonthGoal);
        if (this.strategicPriority != null) entity.setStrategicPriority(this.strategicPriority);
        if (this.desiredPartners != null) entity.setDesiredPartners(this.desiredPartners);

        if (this.areasOfInterest != null) entity.setAreasOfInterest(this.areasOfInterest);
        if (this.opportunityTypes != null) entity.setOpportunityTypes(this.opportunityTypes);
        if (this.receiveAiRecommendations != null) entity.setReceiveAiRecommendations(this.receiveAiRecommendations);
        if (this.emailNotifications != null) entity.setEmailNotifications(this.emailNotifications);
        if (this.matchAlerts != null) entity.setMatchAlerts(this.matchAlerts);
        if (this.avatarUrl != null) {
            entity.setAvatarUrl(this.avatarUrl);
            if (entity.getUser() != null) {
                entity.getUser().setAvatarUrl(this.avatarUrl);
            }
        }
    }

    // Getters and Setters
    public Boolean getEmailNotifications() { return emailNotifications; }
    public void setEmailNotifications(Boolean emailNotifications) { this.emailNotifications = emailNotifications; }
    public Boolean getMatchAlerts() { return matchAlerts; }
    public void setMatchAlerts(Boolean matchAlerts) { this.matchAlerts = matchAlerts; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
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
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
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
    public Integer getTrlScore() { return trlScore; }
    public void setTrlScore(Integer trlScore) { this.trlScore = trlScore; }
    public Boolean getTractionAttested() { return tractionAttested; }
    public void setTractionAttested(Boolean tractionAttested) { this.tractionAttested = tractionAttested; }
    public String getTractionAttestationInfo() { return tractionAttestationInfo; }
    public void setTractionAttestationInfo(String tractionAttestationInfo) { this.tractionAttestationInfo = tractionAttestationInfo; }
    public Integer getCumulativeOpportunitiesFound() { return cumulativeOpportunitiesFound; }
    public void setCumulativeOpportunitiesFound(Integer cumulativeOpportunitiesFound) { this.cumulativeOpportunitiesFound = cumulativeOpportunitiesFound; }
    public Integer getLastUpdateNewMatchesCount() { return lastUpdateNewMatchesCount; }
    public void setLastUpdateNewMatchesCount(Integer lastUpdateNewMatchesCount) { this.lastUpdateNewMatchesCount = lastUpdateNewMatchesCount; }
}
