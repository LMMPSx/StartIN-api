package com.startin.api.service;

import com.startin.api.dto.startup.MatchingPreferencesDTO;
import com.startin.api.dto.startup.StartupProfileDTO;
import com.startin.api.model.StartupProfile;
import com.startin.api.repository.StartupProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class StartupProfileService {

    private final StartupProfileRepository startupProfileRepository;

    public StartupProfileService(StartupProfileRepository startupProfileRepository) {
        this.startupProfileRepository = startupProfileRepository;
    }

    public Optional<StartupProfileDTO> getProfile(Long id) {
        return startupProfileRepository.findById(id).map(StartupProfileDTO::fromEntity);
    }

    public StartupProfileDTO getDefaultProfile() {
        return startupProfileRepository.findAll().stream()
                .findFirst()
                .map(StartupProfileDTO::fromEntity)
                .orElseGet(() -> {
                    StartupProfile p = new StartupProfile();
                    p = startupProfileRepository.save(p);
                    return StartupProfileDTO.fromEntity(p);
                });
    }

    @Transactional
    public StartupProfileDTO updateProfile(Long id, StartupProfileDTO dto) {
        StartupProfile profile = startupProfileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Perfil de startup não encontrado: " + id));

        dto.applyToEntity(profile);
        StartupProfile saved = startupProfileRepository.save(profile);
        return StartupProfileDTO.fromEntity(saved);
    }

    @Transactional
    public StartupProfileDTO updatePreferences(Long id, MatchingPreferencesDTO prefs) {
        StartupProfile profile = startupProfileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Perfil de startup não encontrado: " + id));

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
        return StartupProfileDTO.fromEntity(saved);
    }
}
