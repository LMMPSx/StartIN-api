package com.startin.api.service;

import com.startin.api.dto.ai.AiMatchItemDTO;
import com.startin.api.dto.ai.AiMatchResponseDTO;
import com.startin.api.dto.match.MatchResultDTO;
import com.startin.api.model.MatchResult;
import com.startin.api.model.Notification;
import com.startin.api.model.Opportunity;
import com.startin.api.model.StartupProfile;
import com.startin.api.model.enums.MatchStatus;
import com.startin.api.model.enums.OpportunityStatus;
import com.startin.api.model.enums.OpportunityType;
import com.startin.api.repository.MatchResultRepository;
import com.startin.api.repository.NotificationRepository;
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
    private final NotificationRepository notificationRepository;
    private final AiIntegrationService aiIntegrationService;

    public MatchService(
            MatchResultRepository matchResultRepository,
            StartupProfileRepository startupProfileRepository,
            OpportunityRepository opportunityRepository,
            NotificationRepository notificationRepository,
            AiIntegrationService aiIntegrationService) {
        this.matchResultRepository = matchResultRepository;
        this.startupProfileRepository = startupProfileRepository;
        this.opportunityRepository = opportunityRepository;
        this.notificationRepository = notificationRepository;
        this.aiIntegrationService = aiIntegrationService;
    }

    @Transactional
    public List<MatchResultDTO> getMatchesForStartup(Long startupId) {
        StartupProfile profile = resolveStartupProfile(startupId);
        if (profile == null) {
            return List.of();
        }

        List<MatchResult> existing = matchResultRepository.findByStartupProfileIdOrderByMatchScoreDesc(profile.getId());
        boolean hasWebScraped = opportunityRepository.findAll().stream()
                .anyMatch(o -> o.getCode() != null && o.getCode().startsWith("web-"));

        if (existing.isEmpty() || !hasWebScraped) {
            return recalculateMatches(profile.getId(), true);
        }

        return existing.stream()
                .map(MatchResultDTO::fromEntity)
                .toList();
    }

    public Optional<MatchResultDTO> getById(Long id) {
        return matchResultRepository.findById(id).map(MatchResultDTO::fromEntity);
    }

    @Transactional
    public List<MatchResultDTO> recalculateMatches(Long startupId, boolean scrapeLiveWeb) {
        StartupProfile profile = resolveStartupProfile(startupId);
        if (profile == null) {
            throw new IllegalArgumentException("Nenhum perfil de startup cadastrado para calcular matches.");
        }

        List<Opportunity> opportunities = new ArrayList<>(opportunityRepository.findAll());
        log.info("Iniciando cálculo de matches via IA Python (scrapeLiveWeb={}) para startup '{}' (ID {}) com {} oportunidades no MySQL",
                scrapeLiveWeb, profile.getName(), profile.getId(), opportunities.size());

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
                        .filter(o -> o.getTitle().equalsIgnoreCase(item.getOpportunityTitle().trim()))
                        .findFirst()
                        .orElse(null);
            }

            // Se a IA em Python trouxe uma nova oportunidade real raspada da Web que ainda não está no MySQL, salva no banco!
            if (matchedOpp == null && item.getOpportunityTitle() != null && !item.getOpportunityTitle().isBlank()) {
                Opportunity newWebOpp = new Opportunity();
                OpportunityType oppType = OpportunityType.fromString(
                        item.getOpportunityType() != null ? item.getOpportunityType() : "EDITAL"
                );
                newWebOpp.setType(oppType);
                newWebOpp.setTitle(item.getOpportunityTitle().trim());
                String desc = (item.getOpportunityDescription() != null && !item.getOpportunityDescription().isBlank())
                        ? item.getOpportunityDescription()
                        : item.getJustification();
                newWebOpp.setDescription(desc);
                newWebOpp.setShortDescription(desc != null && desc.length() > 110 ? desc.substring(0, 110) + "..." : desc);
                newWebOpp.setMatchScore(item.getMatchScore() != null ? item.getMatchScore() : 88);
                newWebOpp.setMetaInfo(item.getOpportunityMeta() != null && !item.getOpportunityMeta().isBlank()
                        ? item.getOpportunityMeta()
                        : "Inscrições abertas · Web · Brasil");
                newWebOpp.setTags(item.getOpportunityTags() != null && !item.getOpportunityTags().isBlank()
                        ? item.getOpportunityTags()
                        : "Web • Edital Real • IA");
                newWebOpp.setExternalUrl(item.getUrl());
                newWebOpp.setStatus(OpportunityStatus.PUBLICADA);

                String color = switch (oppType) {
                    case EDITAL -> "bg-[#fff2b8] text-[#795900]";
                    case POC -> "bg-[#d9f1e5] text-[#16734d]";
                    case EVENTO -> "bg-[#dff1f5] text-[#1e7080]";
                    case COMPRA -> "bg-[#ffe1c3] text-[#975524]";
                };
                newWebOpp.setColor(color);

                newWebOpp = opportunityRepository.save(newWebOpp);
                newWebOpp.setCode("web-" + newWebOpp.getId());
                matchedOpp = opportunityRepository.save(newWebOpp);
                opportunities.add(matchedOpp);
                log.info("Nova oportunidade real raspada pela IA salva no MySQL: '{}' (ID {})", matchedOpp.getTitle(), matchedOpp.getId());
            }

            if (matchedOpp != null) {
                matchedOpp.setMatchScore(item.getMatchScore());
                opportunityRepository.save(matchedOpp);

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

        savedResults.sort((a, b) -> Integer.compare(b.getMatchScore(), a.getMatchScore()));

        if (!savedResults.isEmpty()) {
            MatchResult best = savedResults.get(0);
            notificationRepository.save(new Notification(
                    null,
                    "Matches e editais da web atualizados pela IA",
                    profile.getName() + " × " + best.getOpportunity().getTitle() + " (" + best.getMatchScore() + "% de compatibilidade).",
                    "Agora",
                    "Matches",
                    false
            ));
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

    @Transactional
    public MatchResultDTO participateInOpportunity(Long startupId, String opportunityIdOrCode) {
        StartupProfile profile = resolveStartupProfile(startupId);
        if (profile == null) {
            throw new IllegalArgumentException("Perfil de startup não encontrado.");
        }

        Opportunity opp = null;
        try {
            Long oppId = Long.parseLong(opportunityIdOrCode);
            opp = opportunityRepository.findById(oppId).orElse(null);
        } catch (NumberFormatException ignored) {
            opp = opportunityRepository.findByCode(opportunityIdOrCode).orElse(null);
        }

        if (opp == null) {
            throw new IllegalArgumentException("Oportunidade não encontrada: " + opportunityIdOrCode);
        }

        final Opportunity targetOpp = opp;
        MatchResult match = matchResultRepository
                .findByStartupProfileIdAndOpportunityId(profile.getId(), targetOpp.getId())
                .orElseGet(() -> {
                    MatchResult m = new MatchResult();
                    m.setStartupProfile(profile);
                    m.setOpportunity(targetOpp);
                    m.setMatchScore(targetOpp.getMatchScore() != null ? targetOpp.getMatchScore() : 90);
                    m.setJustification("Interesse registrado diretamente pela startup " + profile.getName() + ".");
                    m.setRecommendedAction("Acompanhar retorno da organização responsável.");
                    m.setReasonProfile((profile.getMainArea() != null ? profile.getMainArea() : "Inovação") + " · " + (profile.getBusinessModel() != null ? profile.getBusinessModel() : "B2B"));
                    m.setReasonRequirements(profile.getTechnologies() != null ? profile.getTechnologies() : "Requisitos técnicos compatíveis");
                    m.setReasonObjective(profile.getMainObjective() != null ? profile.getMainObjective() : "Validação e negócios");
                    m.setReasonContext(profile.getLocation() != null ? profile.getLocation() : "Brasil");
                    return m;
                });

        match.setStatus(MatchStatus.INTERESSE_ENVIADO);
        MatchResult saved = matchResultRepository.save(match);

        notificationRepository.save(new Notification(
                null,
                "Interesse registrado em oportunidade",
                "Você registrou participação em '" + targetOpp.getTitle() + "'.",
                "Agora",
                "Oportunidades",
                false
        ));

        return MatchResultDTO.fromEntity(saved);
    }

    private StartupProfile resolveStartupProfile(Long startupId) {
        if (startupId != null) {
            Optional<StartupProfile> byId = startupProfileRepository.findById(startupId);
            if (byId.isPresent()) return byId.get();
            Optional<StartupProfile> byUser = startupProfileRepository.findByUserId(startupId);
            if (byUser.isPresent()) return byUser.get();
        }
        return startupProfileRepository.findAll().stream().findFirst().orElse(null);
    }
}
