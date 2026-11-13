package com.startin.api.service;

import com.startin.api.dto.startup.MatchingPreferencesDTO;
import com.startin.api.dto.startup.StartupProfileDTO;
import com.startin.api.model.Notification;
import com.startin.api.model.StartupProfile;
import com.startin.api.repository.NotificationRepository;
import com.startin.api.repository.StartupProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class StartupProfileService {

    private final StartupProfileRepository startupProfileRepository;
    private final NotificationRepository notificationRepository;

    public StartupProfileService(
            StartupProfileRepository startupProfileRepository,
            NotificationRepository notificationRepository) {
        this.startupProfileRepository = startupProfileRepository;
        this.notificationRepository = notificationRepository;
    }

    public List<StartupProfileDTO> getAllProfiles() {
        return startupProfileRepository.findAll().stream()
                .map(StartupProfileDTO::fromEntity)
                .toList();
    }

    public Optional<StartupProfileDTO> getProfile(Long id) {
        if (id == null) return Optional.empty();
        return startupProfileRepository.findById(id)
                .or(() -> startupProfileRepository.findByUserId(id))
                .map(StartupProfileDTO::fromEntity);
    }

    public Optional<StartupProfileDTO> getProfileByUserId(Long userId) {
        return startupProfileRepository.findByUserId(userId).map(StartupProfileDTO::fromEntity);
    }

    public StartupProfileDTO getDefaultProfile() {
        return startupProfileRepository.findAll().stream()
                .findFirst()
                .map(StartupProfileDTO::fromEntity)
                .orElse(null);
    }

    @Transactional
    public StartupProfileDTO updateProfile(Long id, StartupProfileDTO dto) {
        StartupProfile profile = (dto.getUserId() != null && dto.getUserId() > 0
                ? startupProfileRepository.findByUserId(dto.getUserId())
                : Optional.<StartupProfile>empty())
                .or(() -> id != null && id > 0 ? startupProfileRepository.findById(id) : Optional.empty())
                .or(() -> id != null && id > 0 ? startupProfileRepository.findByUserId(id) : Optional.empty())
                .orElseGet(() -> {
                    StartupProfile p = new StartupProfile();
                    return startupProfileRepository.save(p);
                });

        dto.applyToEntity(profile);
        StartupProfile saved = startupProfileRepository.save(profile);

        Long userId = saved.getUser() != null ? saved.getUser().getId() : null;
        notificationRepository.save(Notification.forStartup(
                saved.getId(),
                userId,
                "Seu perfil foi atualizado",
                "As informações e critérios da startup " + (saved.getName() != null ? saved.getName() : "") + " foram aplicados às recomendações.",
                "Sistema",
                "account",
                String.valueOf(saved.getId())
        ));

        return StartupProfileDTO.fromEntity(saved);
    }

    @Transactional
    public StartupProfileDTO updatePreferences(Long id, MatchingPreferencesDTO prefs) {
        StartupProfile profile = startupProfileRepository.findById(id)
                .or(() -> startupProfileRepository.findByUserId(id))
                .orElseGet(() -> startupProfileRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Perfil de startup não encontrado: " + id)));

        if (prefs.getAreasOfInterest() != null) {
            profile.setAreasOfInterest(prefs.getAreasOfInterest());
        }
        if (prefs.getOpportunityTypes() != null) {
            profile.setOpportunityTypes(prefs.getOpportunityTypes());
        }
        if (prefs.getReceiveAiRecommendations() != null) {
            profile.setReceiveAiRecommendations(prefs.getReceiveAiRecommendations());
        }
        if (prefs.getEmailNotifications() != null) {
            profile.setEmailNotifications(prefs.getEmailNotifications());
        }
        if (prefs.getMatchAlerts() != null) {
            profile.setMatchAlerts(prefs.getMatchAlerts());
        }
        if (prefs.getMainSector() != null) {
            profile.setMainSector(prefs.getMainSector());
        }
        if (prefs.getTechnologies() != null) {
            profile.setTechnologies(prefs.getTechnologies());
        }
        if (prefs.getOperationRegion() != null) {
            profile.setOperationRegion(prefs.getOperationRegion());
        }
        if (prefs.getCurrentStage() != null) {
            profile.setCurrentStage(prefs.getCurrentStage());
        }
        if (prefs.getInvestmentNeed() != null) {
            profile.setInvestmentNeed(prefs.getInvestmentNeed());
        }
        if (prefs.getTrlScore() != null) {
            profile.setTrlScore(prefs.getTrlScore());
        }
        if (prefs.getTractionAttested() != null) {
            profile.setTractionAttested(prefs.getTractionAttested());
        }
        if (prefs.getTractionAttestationInfo() != null) {
            profile.setTractionAttestationInfo(prefs.getTractionAttestationInfo());
        }
        if (prefs.getProductMaturity() != null) {
            profile.setProductMaturity(prefs.getProductMaturity());
        }
        if (prefs.getTractionCustomers() != null) {
            profile.setTractionCustomers(prefs.getTractionCustomers());
        }
        if (prefs.getAnnualRevenue() != null) {
            profile.setAnnualRevenue(prefs.getAnnualRevenue());
        }
        if (prefs.getRecentGrowth() != null) {
            profile.setRecentGrowth(prefs.getRecentGrowth());
        }
        if (prefs.getAvatarUrl() != null) {
            profile.setAvatarUrl(prefs.getAvatarUrl());
            if (profile.getUser() != null) {
                profile.getUser().setAvatarUrl(prefs.getAvatarUrl());
            }
        }

        StartupProfile saved = startupProfileRepository.save(profile);

        Long userId = saved.getUser() != null ? saved.getUser().getId() : null;
        notificationRepository.save(Notification.forStartup(
                saved.getId(),
                userId,
                "Seu perfil foi atualizado",
                "As informações e critérios da startup " + (saved.getName() != null ? saved.getName() : "") + " foram aplicados às recomendações.",
                "Sistema",
                "account",
                String.valueOf(saved.getId())
        ));

        return StartupProfileDTO.fromEntity(saved);
    }
}
