package com.startin.api.dto.startup;

public class MatchingPreferencesDTO {

    private String areasOfInterest;
    private String opportunityTypes;
    private Boolean receiveAiRecommendations;
    private Boolean emailNotifications;
    private Boolean matchAlerts;
    private String mainSector;
    private String technologies;
    private String operationRegion;
    private String currentStage;
    private String investmentNeed;
    private String avatarUrl;
    private Integer trlScore;
    private Boolean tractionAttested;
    private String tractionAttestationInfo;
    private String productMaturity;
    private String tractionCustomers;
    private String annualRevenue;
    private String recentGrowth;

    public MatchingPreferencesDTO() {
    }

    public MatchingPreferencesDTO(String areasOfInterest, String opportunityTypes, Boolean receiveAiRecommendations) {
        this.areasOfInterest = areasOfInterest;
        this.opportunityTypes = opportunityTypes;
        this.receiveAiRecommendations = receiveAiRecommendations;
    }

    public String getAreasOfInterest() {
        return areasOfInterest;
    }

    public void setAreasOfInterest(String areasOfInterest) {
        this.areasOfInterest = areasOfInterest;
    }

    public String getOpportunityTypes() {
        return opportunityTypes;
    }

    public void setOpportunityTypes(String opportunityTypes) {
        this.opportunityTypes = opportunityTypes;
    }

    public Boolean getReceiveAiRecommendations() {
        return receiveAiRecommendations;
    }

    public void setReceiveAiRecommendations(Boolean receiveAiRecommendations) {
        this.receiveAiRecommendations = receiveAiRecommendations;
    }

    public Boolean getEmailNotifications() {
        return emailNotifications;
    }

    public void setEmailNotifications(Boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }

    public Boolean getMatchAlerts() {
        return matchAlerts;
    }

    public void setMatchAlerts(Boolean matchAlerts) {
        this.matchAlerts = matchAlerts;
    }

    public String getMainSector() {
        return mainSector;
    }

    public void setMainSector(String mainSector) {
        this.mainSector = mainSector;
    }

    public String getTechnologies() {
        return technologies;
    }

    public void setTechnologies(String technologies) {
        this.technologies = technologies;
    }

    public String getOperationRegion() {
        return operationRegion;
    }

    public void setOperationRegion(String operationRegion) {
        this.operationRegion = operationRegion;
    }

    public String getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(String currentStage) {
        this.currentStage = currentStage;
    }

    public String getInvestmentNeed() {
        return investmentNeed;
    }

    public void setInvestmentNeed(String investmentNeed) {
        this.investmentNeed = investmentNeed;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Integer getTrlScore() { return trlScore; }
    public void setTrlScore(Integer trlScore) { this.trlScore = trlScore; }
    public Boolean getTractionAttested() { return tractionAttested; }
    public void setTractionAttested(Boolean tractionAttested) { this.tractionAttested = tractionAttested; }
    public String getTractionAttestationInfo() { return tractionAttestationInfo; }
    public void setTractionAttestationInfo(String tractionAttestationInfo) { this.tractionAttestationInfo = tractionAttestationInfo; }
    public String getProductMaturity() { return productMaturity; }
    public void setProductMaturity(String productMaturity) { this.productMaturity = productMaturity; }
    public String getTractionCustomers() { return tractionCustomers; }
    public void setTractionCustomers(String tractionCustomers) { this.tractionCustomers = tractionCustomers; }
    public String getAnnualRevenue() { return annualRevenue; }
    public void setAnnualRevenue(String annualRevenue) { this.annualRevenue = annualRevenue; }
    public String getRecentGrowth() { return recentGrowth; }
    public void setRecentGrowth(String recentGrowth) { this.recentGrowth = recentGrowth; }
}
