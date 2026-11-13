package com.startin.api.repository;

import com.startin.api.model.ProviderSavedStartup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderSavedStartupRepository extends JpaRepository<ProviderSavedStartup, Long> {

    List<ProviderSavedStartup> findByProviderIdOrderByCreatedAtDesc(Long providerId);

    Optional<ProviderSavedStartup> findByProviderIdAndStartupId(Long providerId, Long startupId);

    long countByProviderId(Long providerId);
}
