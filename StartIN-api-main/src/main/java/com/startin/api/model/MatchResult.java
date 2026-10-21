package com.startin.api.model;

import com.startin.api.model.enums.MatchStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "matches")
public class MatchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "startup_id", nullable = false)
    private StartupProfile startupProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id", nullable = false)
    private Opportunity opportunity;

    @Column(nullable = false)
    private Integer matchScore;

    @Column(length = 3000)
    private String justification;

    @Column(length = 2000)
    private String recommendedAction;

    // Quatro pilares exibidos na UI (Página 9 e tela de detalhe)
    @Column(length = 1000)
    private String reasonProfile;

    @Column(length = 1000)
    private String reasonRequirements;

    @Column(length = 1000)
    private String reasonObjective;

    @Column(length = 1000)
    private String reasonContext;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status = MatchStatus.NOVO;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MatchResult() {
    }

    public MatchResult(Long id, StartupProfile startupProfile, Opportunity opportunity,
                       Integer matchScore, String justification, String recommendedAction,
                       String reasonProfile, String reasonRequirements, String reasonObjective,
                       String reasonContext, MatchStatus status) {
        this.id = id;
        this.startupProfile = startupProfile;
        this.opportunity = opportunity;
        this.matchScore = matchScore;
        this.justification = justification;
        this.recommendedAction = recommendedAction;
        this.reasonProfile = reasonProfile;
        this.reasonRequirements = reasonRequirements;
        this.reasonObjective = reasonObjective;
        this.reasonContext = reasonContext;
        this.status = status != null ? status : MatchStatus.NOVO;
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
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StartupProfile getStartupProfile() {
        return startupProfile;
    }

    public void setStartupProfile(StartupProfile startupProfile) {
        this.startupProfile = startupProfile;
    }

    public Opportunity getOpportunity() {
        return opportunity;
    }

    public void setOpportunity(Opportunity opportunity) {
        this.opportunity = opportunity;
    }

    public Integer getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Integer matchScore) {
        this.matchScore = matchScore;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public String getReasonProfile() {
        return reasonProfile;
    }

    public void setReasonProfile(String reasonProfile) {
        this.reasonProfile = reasonProfile;
    }

    public String getReasonRequirements() {
        return reasonRequirements;
    }

    public void setReasonRequirements(String reasonRequirements) {
        this.reasonRequirements = reasonRequirements;
    }

    public String getReasonObjective() {
        return reasonObjective;
    }

    public void setReasonObjective(String reasonObjective) {
        this.reasonObjective = reasonObjective;
    }

    public String getReasonContext() {
        return reasonContext;
    }

    public void setReasonContext(String reasonContext) {
        this.reasonContext = reasonContext;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public void setStatus(MatchStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
