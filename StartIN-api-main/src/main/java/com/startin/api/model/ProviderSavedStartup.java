package com.startin.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "provider_saved_startups",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_provider_saved_startup", columnNames = {"provider_id", "startup_id"})
    }
)
public class ProviderSavedStartup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "startup_id", nullable = false)
    private Long startupId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public ProviderSavedStartup() {
    }

    public ProviderSavedStartup(Long providerId, Long startupId) {
        this.providerId = providerId;
        this.startupId = startupId;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProviderId() { return providerId; }
    public void setProviderId(Long providerId) { this.providerId = providerId; }

    public Long getStartupId() { return startupId; }
    public void setStartupId(Long startupId) { this.startupId = startupId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
