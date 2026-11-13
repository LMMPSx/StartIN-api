package com.startin.api.dto.provider;

public class CreateServiceConnectionDTO {

    private Long startupId;
    private Long providerId;
    private String senderType = "STARTUP"; // "STARTUP" ou "PROVIDER"
    private String message;
    private Integer matchScore = 94;

    public CreateServiceConnectionDTO() {
    }

    public Long getStartupId() { return startupId; }
    public void setStartupId(Long startupId) { this.startupId = startupId; }
    public Long getProviderId() { return providerId; }
    public void setProviderId(Long providerId) { this.providerId = providerId; }
    public String getSenderType() { return senderType; }
    public void setSenderType(String senderType) { this.senderType = senderType; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Integer getMatchScore() { return matchScore; }
    public void setMatchScore(Integer matchScore) { this.matchScore = matchScore; }
}
