package com.startin.api.dto.ai;

public class AiMatchItemDTO {

    private String opportunityId;
    private String opportunityTitle;
    private String url;
    private Integer matchScore;
    private String justification;
    private String recommendedAction;
    private AiMatchReasonsDTO reasons;

    public AiMatchItemDTO() {
    }

    public String getOpportunityId() { return opportunityId; }
    public void setOpportunityId(String opportunityId) { this.opportunityId = opportunityId; }
    public String getOpportunityTitle() { return opportunityTitle; }
    public void setOpportunityTitle(String opportunityTitle) { this.opportunityTitle = opportunityTitle; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public Integer getMatchScore() { return matchScore; }
    public void setMatchScore(Integer matchScore) { this.matchScore = matchScore; }
    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }
    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
    public AiMatchReasonsDTO getReasons() { return reasons; }
    public void setReasons(AiMatchReasonsDTO reasons) { this.reasons = reasons; }
}
