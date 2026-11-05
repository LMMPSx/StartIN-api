package com.startin.api.repository;

import com.startin.api.model.ServiceConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceConnectionRepository extends JpaRepository<ServiceConnection, Long> {

    List<ServiceConnection> findByStartupIdOrderByIdDesc(Long startupId);

    List<ServiceConnection> findByStartupIdOrderByCreatedAtDesc(Long startupId);

    List<ServiceConnection> findByProviderIdOrderByIdDesc(Long providerId);

    List<ServiceConnection> findByProviderIdOrderByCreatedAtDesc(Long providerId);

    List<ServiceConnection> findByStartupIdAndStatusOrderByIdDesc(Long startupId, String status);

    List<ServiceConnection> findByProviderIdAndStatusOrderByIdDesc(Long providerId, String status);

    Optional<ServiceConnection> findByStartupIdAndProviderId(Long startupId, Long providerId);
}
