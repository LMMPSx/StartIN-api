package com.startin.api.dto.match;

import com.startin.api.model.MatchResult;
import java.util.HashMap;
import java.util.Map;

public class MatchResultDTO {

    private Long id;
    private Long startupId;
    private String startupName;
    private Long opportunityId;
    private String opportunityCode;
    private String opportunityTitle;
    private String opportunityType;
    private String opportunityShort;
    private String opportunityMeta;
    private Integer matchScore;
    private String justification;
    private String recommendedAction;
    private Map<String, String> reasons = new HashMap<>();
    private String status;

    public MatchResultDTO() {
    }

    public static MatchResultDTO fromEntity(MatchResult entity) {
        if (entity == null) return null;
        MatchResultDTO dto = new MatchResultDTO();
        dto.setId(entity.getId());
        if (entity.getStartupProfile() != null) {
            dto.setStartupId(entity.getStartupProfile().getId());
            dto.setStartupName(entity.getStartupProfile().getName());
        }
        if (entity.getOpportunity() != null) {
            dto.setOpportunityId(entity.getOpportunity().getId());
            dto.setOpportunityCode(entity.getOpportunity().getCode());
            dto.setOpportunityTitle(entity.getOpportunity().getTitle());
            dto.setOpportunityType(entity.getOpportunity().getType() != null ? entity.getOpportunity().getType().name() : "EDITAL");
            dto.setOpportunityShort(entity.getOpportunity().getShortDescription());
            dto.setOpportunityMeta(entity.getOpportunity().getMetaInfo());
        }
        dto.setMatchScore(entity.getMatchScore());
        dto.setJustification(entity.getJustification());
        dto.setRecommendedAction(entity.getRecommendedAction());

        Map<String, String> r = new HashMap<>();
        if (entity.getReasonProfile() != null) r.put("perfil", entity.getReasonProfile());
        if (entity.getReasonRequirements() != null) r.put("requisitos", entity.getReasonRequirements());
        if (entity.getReasonObjective() != null) r.put("objetivo", entity.getReasonObjective());
        if (entity.getReasonContext() != null) r.put("contexto", entity.getReasonContext());
        dto.setReasons(r);

        dto.setStatus(entity.getStatus() != null ? entity.getStatus().name() : "NOVO");
        return dto;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStartupId() { return startupId; }
    public void setStartupId(Long startupId) { this.startupId = startupId; }
    public String getStartupName() { return startupName; }
    public void setStartupName(String startupName) { this.startupName = startupName; }
    public Long getOpportunityId() { return opportunityId; }
    public void setOpportunityId(Long opportunityId) { this.opportunityId = opportunityId; }
    public String getOpportunityCode() { return opportunityCode; }
    public void setOpportunityCode(String opportunityCode) { this.opportunityCode = opportunityCode; }
    public String getOpportunityTitle() { return opportunityTitle; }
    public void setOpportunityTitle(String opportunityTitle) { this.opportunityTitle = opportunityTitle; }
    public String getOpportunityType() { return opportunityType; }
    public void setOpportunityType(String opportunityType) { this.opportunityType = opportunityType; }
    public String getOpportunityShort() { return opportunityShort; }
    public void setOpportunityShort(String opportunityShort) { this.opportunityShort = opportunityShort; }
    public String getOpportunityMeta() { return opportunityMeta; }
    public void setOpportunityMeta(String opportunityMeta) { this.opportunityMeta = opportunityMeta; }
    public Integer getMatchScore() { return matchScore; }
    public void setMatchScore(Integer matchScore) { this.matchScore = matchScore; }
    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }
    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
    public Map<String, String> getReasons() { return reasons; }
    public void setReasons(Map<String, String> reasons) { this.reasons = reasons; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
