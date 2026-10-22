package com.startin.api.service;

import com.startin.api.dto.startup.MatchingPreferencesDTO;
import com.startin.api.dto.startup.StartupProfileDTO;
import com.startin.api.model.Notification;
import com.startin.api.model.StartupProfile;
import com.startin.api.repository.NotificationRepository;
import com.startin.api.repository.StartupProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public Optional<StartupProfileDTO> getProfile(Long id) {
        return startupProfileRepository.findById(id).map(StartupProfileDTO::fromEntity);
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
        StartupProfile profile = startupProfileRepository.findById(id)
                .orElseGet(() -> startupProfileRepository.findAll().stream().findFirst()
                        .orElseGet(() -> {
                            StartupProfile p = new StartupProfile();
                            return startupProfileRepository.save(p);
                        }));

        dto.applyToEntity(profile);
        StartupProfile saved = startupProfileRepository.save(profile);

        notificationRepository.save(new Notification(
                null,
                "Perfil da startup atualizado",
                "Os dados da startup '" + saved.getName() + "' foram salvos no MySQL e enviados para o motor de IA.",
                "Agora",
                "Sistema",
                false
        ));

        return StartupProfileDTO.fromEntity(saved);
    }

    @Transactional
    public StartupProfileDTO updatePreferences(Long id, MatchingPreferencesDTO prefs) {
        StartupProfile profile = startupProfileRepository.findById(id)
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

        StartupProfile saved = startupProfileRepository.save(profile);

        notificationRepository.save(new Notification(
                null,
                "Preferências de matching atualizadas",
                "A IA aplicou suas novas preferências (" + (saved.getAreasOfInterest() != null ? saved.getAreasOfInterest() : "geral") + ").",
                "Agora",
                "Sistema",
                false
        ));

        return StartupProfileDTO.fromEntity(saved);
    }
}
