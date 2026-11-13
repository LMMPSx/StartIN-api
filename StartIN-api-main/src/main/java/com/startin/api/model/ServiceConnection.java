package com.startin.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_connections")
public class ServiceConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "startup_id", nullable = false)
    private StartupProfile startup;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "provider_id", nullable = false)
    private ServiceProvider provider;

    @Column(nullable = false)
    private String senderType = "STARTUP"; // "STARTUP" ou "PROVIDER"

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private String status = "PENDING"; // "PENDING", "ACCEPTED", "REJECTED"

    private Integer matchScore = 94;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ServiceConnection() {
    }

    public ServiceConnection(StartupProfile startup, ServiceProvider provider, String senderType, String message, String status, Integer matchScore) {
        this.startup = startup;
        this.provider = provider;
        this.senderType = senderType != null ? senderType : "STARTUP";
        this.message = message;
        this.status = status != null ? status : "PENDING";
        this.matchScore = matchScore != null ? matchScore : 94;
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public StartupProfile getStartup() { return startup; }
    public void setStartup(StartupProfile startup) { this.startup = startup; }

    public ServiceProvider getProvider() { return provider; }
    public void setProvider(ServiceProvider provider) { this.provider = provider; }

    public String getSenderType() { return senderType; }
    public void setSenderType(String senderType) { this.senderType = senderType; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getMatchScore() { return matchScore; }
    public void setMatchScore(Integer matchScore) { this.matchScore = matchScore; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
