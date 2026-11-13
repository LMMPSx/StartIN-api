package com.startin.api.model;

import com.startin.api.model.enums.OpportunityStatus;
import com.startin.api.model.enums.OpportunityType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "opportunities")
public class Opportunity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code; // ex: "edital", "poc", "evento", "compra"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50, columnDefinition = "VARCHAR(50)")
    private OpportunityType type = OpportunityType.EDITAL;

    @Column(nullable = false)
    private String title;

    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer matchScore = 85;

    private String metaInfo; // ex: "Prazo 15/10/2026 · Brasil · Remoto"
    private String tags;     // ex: "setor • tecnologia • objetivo"
    private String externalUrl;
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50, columnDefinition = "VARCHAR(50)")
    private OpportunityStatus status = OpportunityStatus.PUBLICADA;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "startup_id")
    private StartupProfile startup;

    private String targetCategory;

    private Boolean proMember = false;

    private LocalDateTime createdAt;

    public Opportunity() {
    }

    public Opportunity(Long id, String code, OpportunityType type, String title, String shortDescription,
                       String description, Integer matchScore, String metaInfo, String tags,
                       String externalUrl, String color, OpportunityStatus status) {
        this.id = id;
        this.code = code;
        this.type = type != null ? type : OpportunityType.EDITAL;
        this.title = title;
        this.shortDescription = shortDescription;
        this.description = description;
        this.matchScore = matchScore != null ? matchScore : 85;
        this.metaInfo = metaInfo;
        this.tags = tags;
        this.externalUrl = externalUrl;
        this.color = color;
        this.status = status != null ? status : OpportunityStatus.PUBLICADA;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public OpportunityType getType() {
        return type;
    }

    public void setType(OpportunityType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Integer matchScore) {
        this.matchScore = matchScore;
    }

    public String getMetaInfo() {
        return metaInfo;
    }

    public void setMetaInfo(String metaInfo) {
        this.metaInfo = metaInfo;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public OpportunityStatus getStatus() {
        return status;
    }

    public void setStatus(OpportunityStatus status) {
        this.status = status;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public StartupProfile getStartup() {
        return startup;
    }

    public void setStartup(StartupProfile startup) {
        this.startup = startup;
    }

    public String getTargetCategory() {
        return targetCategory;
    }

    public void setTargetCategory(String targetCategory) {
        this.targetCategory = targetCategory;
    }

    public Boolean getProMember() {
        return proMember != null ? proMember : false;
    }

    public void setProMember(Boolean proMember) {
        this.proMember = proMember != null ? proMember : false;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
