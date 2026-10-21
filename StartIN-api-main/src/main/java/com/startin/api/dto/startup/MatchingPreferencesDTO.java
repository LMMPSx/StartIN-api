package com.startin.api.dto.startup;

public class MatchingPreferencesDTO {

    private String areasOfInterest;
    private String opportunityTypes;
    private Boolean receiveAiRecommendations;

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
}
