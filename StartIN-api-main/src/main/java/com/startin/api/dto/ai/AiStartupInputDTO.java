package com.startin.api.dto.ai;

import java.util.List;

public class AiStartupInputDTO {

    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;

    // 01. Identidade
    private String name;
    private String sector;
    private String mainArea;
    private String description;
    private String location;
    private String cnpj;
    private String phone;
    private String website;
    private String foundedYear;

    // 02. Solução
    private String problem;
    private String howItWorks;
    private String differentiator;
    private String impact;
    private List<String> tags;
    private List<String> technologies;
    private String businessModel;

    // 03. Mercado
    private String targetAudience;
    private String mainSector;
    private String operationRegion;
    private String state;
    private String customerProfile;
    private String marketSize;
    private String competitors;
    private String mainChannel;

    // 04. Momento e Tração
    private String stage;
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

    // 05. Objetivos
    private String mainObjective;
    private String investmentNeed;
    private String executionDeadline;
    private String resourceUse;
    private String twelveMonthGoal;
    private String strategicPriority;
    private String desiredPartners;

    // 06. Preferências de Matching
    private String areasOfInterest;
    private String opportunityTypes;
    private Boolean receiveAiRecommendations;

    public AiStartupInputDTO() {
    }

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
    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }
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
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public List<String> getTechnologies() { return technologies; }
    public void setTechnologies(List<String> technologies) { this.technologies = technologies; }
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
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
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
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Integer getTrlScore() { return trlScore; }
    public void setTrlScore(Integer trlScore) { this.trlScore = trlScore; }
    public Boolean getTractionAttested() { return tractionAttested; }
    public void setTractionAttested(Boolean tractionAttested) { this.tractionAttested = tractionAttested; }
    public String getTractionAttestationInfo() { return tractionAttestationInfo; }
    public void setTractionAttestationInfo(String tractionAttestationInfo) { this.tractionAttestationInfo = tractionAttestationInfo; }
}
