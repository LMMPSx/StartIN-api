package com.startin.api.dto.ai;

public class AiMatchItemDTO {

    private String opportunityId;
    private String opportunityTitle;
    private String opportunityType;
    private String opportunityDescription;
    private String opportunityMeta;
    private String opportunityTags;
    private String url;
    private Integer matchScore;
    private String justification;
    private String recommendedAction;
    private AiMatchReasonsDTO reasons;
    private java.util.Map<String, Object> auditBreakdown;

    public AiMatchItemDTO() {
    }

    public String getOpportunityId() { return opportunityId; }
    public void setOpportunityId(String opportunityId) { this.opportunityId = opportunityId; }
    public String getOpportunityTitle() { return opportunityTitle; }
    public void setOpportunityTitle(String opportunityTitle) { this.opportunityTitle = opportunityTitle; }
    public String getOpportunityType() { return opportunityType; }
    public void setOpportunityType(String opportunityType) { this.opportunityType = opportunityType; }
    public String getOpportunityDescription() { return opportunityDescription; }
    public void setOpportunityDescription(String opportunityDescription) { this.opportunityDescription = opportunityDescription; }
    public String getOpportunityMeta() { return opportunityMeta; }
    public void setOpportunityMeta(String opportunityMeta) { this.opportunityMeta = opportunityMeta; }
    public String getOpportunityTags() { return opportunityTags; }
    public void setOpportunityTags(String opportunityTags) { this.opportunityTags = opportunityTags; }
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
    public java.util.Map<String, Object> getAuditBreakdown() { return auditBreakdown; }
    public void setAuditBreakdown(java.util.Map<String, Object> auditBreakdown) { this.auditBreakdown = auditBreakdown; }
}
