package com.startin.api.service;

import com.startin.api.dto.ai.AiMatchItemDTO;
import com.startin.api.dto.ai.AiMatchResponseDTO;
import com.startin.api.dto.match.MatchResultDTO;
import com.startin.api.model.MatchResult;
import com.startin.api.model.Opportunity;
import com.startin.api.model.StartupProfile;
import com.startin.api.model.enums.MatchStatus;
import com.startin.api.repository.MatchResultRepository;
import com.startin.api.repository.OpportunityRepository;
import com.startin.api.repository.StartupProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MatchService {

    private static final Logger log = LoggerFactory.getLogger(MatchService.class);

    private final MatchResultRepository matchResultRepository;
    private final StartupProfileRepository startupProfileRepository;
    private final OpportunityRepository opportunityRepository;
    private final AiIntegrationService aiIntegrationService;

    public MatchService(
            MatchResultRepository matchResultRepository,
            StartupProfileRepository startupProfileRepository,
            OpportunityRepository opportunityRepository,
            AiIntegrationService aiIntegrationService) {
        this.matchResultRepository = matchResultRepository;
        this.startupProfileRepository = startupProfileRepository;
        this.opportunityRepository = opportunityRepository;
        this.aiIntegrationService = aiIntegrationService;
    }

    public List<MatchResultDTO> getMatchesForStartup(Long startupId) {
        return matchResultRepository.findByStartupProfileIdOrderByMatchScoreDesc(startupId)
                .stream()
                .map(MatchResultDTO::fromEntity)
                .toList();
    }

    public Optional<MatchResultDTO> getById(Long id) {
        return matchResultRepository.findById(id).map(MatchResultDTO::fromEntity);
    }

    @Transactional
    public List<MatchResultDTO> recalculateMatches(Long startupId, boolean scrapeLiveWeb) {
        StartupProfile profile = startupProfileRepository.findById(startupId)
                .orElseThrow(() -> new IllegalArgumentException("Startup não encontrada: " + startupId));

        List<Opportunity> opportunities = opportunityRepository.findAll();
        log.info("Iniciando cálculo de matches para startup '{}' com {} oportunidades disponíveis", profile.getName(), opportunities.size());

        // Integração direta com o microserviço de IA em Python
        AiMatchResponseDTO aiResponse = aiIntegrationService.calculateMatches(profile, opportunities, scrapeLiveWeb);

        List<MatchResult> savedResults = new ArrayList<>();

        for (AiMatchItemDTO item : aiResponse.getMatches()) {
            Opportunity matchedOpp = null;
            if (item.getOpportunityId() != null && !item.getOpportunityId().isBlank()) {
                try {
                    Long oppId = Long.parseLong(item.getOpportunityId());
                    matchedOpp = opportunityRepository.findById(oppId).orElse(null);
                } catch (NumberFormatException ignored) {
                    matchedOpp = opportunityRepository.findByCode(item.getOpportunityId()).orElse(null);
                }
            }

            if (matchedOpp == null && item.getOpportunityTitle() != null) {
                matchedOpp = opportunities.stream()
                        .filter(o -> o.getTitle().equalsIgnoreCase(item.getOpportunityTitle()) ||
                                     o.getTitle().contains(item.getOpportunityTitle()))
                        .findFirst()
                        .orElse(null);
            }

            if (matchedOpp != null) {
                // Atualiza o score na oportunidade
                matchedOpp.setMatchScore(item.getMatchScore());
                opportunityRepository.save(matchedOpp);

                // Busca ou cria o registro do match
                Optional<MatchResult> existing = matchResultRepository.findByStartupProfileIdAndOpportunityId(profile.getId(), matchedOpp.getId());
                MatchResult match = existing.orElseGet(MatchResult::new);
                match.setStartupProfile(profile);
                match.setOpportunity(matchedOpp);
                match.setMatchScore(item.getMatchScore());
                match.setJustification(item.getJustification());
                match.setRecommendedAction(item.getRecommendedAction());

                if (item.getReasons() != null) {
                    match.setReasonProfile(item.getReasons().getPerfil());
                    match.setReasonRequirements(item.getReasons().getRequisitos());
                    match.setReasonObjective(item.getReasons().getObjetivo());
                    match.setReasonContext(item.getReasons().getContexto());
                }

                if (match.getStatus() == null) {
                    match.setStatus(MatchStatus.NOVO);
                }

                savedResults.add(matchResultRepository.save(match));
            }
        }

        return savedResults.stream().map(MatchResultDTO::fromEntity).toList();
    }

    @Transactional
    public MatchResultDTO updateStatus(Long matchId, String newStatus) {
        MatchResult match = matchResultRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match não encontrado: " + matchId));

        match.setStatus(MatchStatus.fromString(newStatus));
        MatchResult saved = matchResultRepository.save(match);
        return MatchResultDTO.fromEntity(saved);
    }
}
