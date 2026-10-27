package com.startin.api.service;

import com.startin.api.dto.ai.AiDailySyncPayloadDTO;
import com.startin.api.dto.ai.AiMatchItemDTO;
import com.startin.api.dto.ai.AiMatchResponseDTO;
import com.startin.api.dto.match.MatchResultDTO;
import com.startin.api.model.Company;
import com.startin.api.model.MatchResult;
import com.startin.api.model.Notification;
import com.startin.api.model.Opportunity;
import com.startin.api.model.StartupProfile;
import com.startin.api.model.enums.MatchStatus;
import com.startin.api.model.enums.OpportunityStatus;
import com.startin.api.model.enums.OpportunityType;
import com.startin.api.repository.CompanyRepository;
import com.startin.api.repository.MatchResultRepository;
import com.startin.api.repository.NotificationRepository;
import com.startin.api.repository.OpportunityRepository;
import com.startin.api.repository.StartupProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class MatchService {

    private static final Logger log = LoggerFactory.getLogger(MatchService.class);

    private final MatchResultRepository matchResultRepository;
    private final StartupProfileRepository startupProfileRepository;
    private final OpportunityRepository opportunityRepository;
    private final CompanyRepository companyRepository;
    private final NotificationRepository notificationRepository;
    private final AiIntegrationService aiIntegrationService;

    public MatchService(
            MatchResultRepository matchResultRepository,
            StartupProfileRepository startupProfileRepository,
            OpportunityRepository opportunityRepository,
            CompanyRepository companyRepository,
            NotificationRepository notificationRepository,
            AiIntegrationService aiIntegrationService) {
        this.matchResultRepository = matchResultRepository;
        this.startupProfileRepository = startupProfileRepository;
        this.opportunityRepository = opportunityRepository;
        this.companyRepository = companyRepository;
        this.notificationRepository = notificationRepository;
        this.aiIntegrationService = aiIntegrationService;
    }

    /**
     * Rotina agendada que roda TODOS OS DIAS ÀS 08:00 DA MANHÃ (horário de Brasília).
     * Solicita o JSON de scraping + matches à IA Python e persiste tudo no MySQL.
     */
    @Scheduled(cron = "0 0 8 * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void scheduledEightAmScrapeAndMatch() {
        log.info("[Agendador 08:00] Executando rotina diária de scraping e cálculo de matches via IA...");
        AiDailySyncPayloadDTO dailyJson = aiIntegrationService.fetchDailyScrapeJsonFromAi();
        if (dailyJson != null) {
            persistDailyAiJson(dailyJson);
        } else {
            for (StartupProfile profile : startupProfileRepository.findAll()) {
                recalculateMatches(profile.getId(), true);
            }
        }
    }

    /**
     * Persiste no banco de dados MySQL o JSON retornado pela IA (oportunidades raspadas às 08:00 + matches calculados).
     */
    @Transactional
    public Map<String, Object> persistDailyAiJson(AiDailySyncPayloadDTO payload) {
        int savedOppsCount = 0;
        int savedMatchesCount = 0;

        List<Opportunity> existingOpps = new ArrayList<>(opportunityRepository.findAll());

        // 1. Salvar oportunidades raspadas no JSON
        if (payload.getScrapedOpportunities() != null) {
            for (AiDailySyncPayloadDTO.ScrapedOpportunityDTO scraped : payload.getScrapedOpportunities()) {
                if (scraped.getTitle() == null || scraped.getTitle().isBlank()) {
                    continue;
                }
                Opportunity found = existingOpps.stream()
                        .filter(o -> o.getTitle().equalsIgnoreCase(scraped.getTitle().trim()))
                        .findFirst()
                        .orElse(null);

                if (found == null) {
                    Opportunity newOpp = new Opportunity();
                    OpportunityType oppType = OpportunityType.fromString(
                            scraped.getType() != null ? scraped.getType() : "EDITAL"
                    );
                    newOpp.setType(oppType);
                    newOpp.setTitle(scraped.getTitle().trim());
                    String desc = scraped.getDescription() != null ? scraped.getDescription() : scraped.getShortDescription();
                    newOpp.setDescription(desc != null ? desc : "Oportunidade capturada na rotina diária das 08:00.");
                    newOpp.setShortDescription(desc != null && desc.length() > 110 ? desc.substring(0, 110) + "..." : desc);
                    newOpp.setMatchScore(90);
                    String meta = scraped.getMeta() != null && !scraped.getMeta().isBlank()
                            ? scraped.getMeta()
                            : (scraped.getDeadline() != null && !scraped.getDeadline().isBlank() ? scraped.getDeadline() : "Inscrições abertas · Brasil");
                    newOpp.setMetaInfo(meta);
                    newOpp.setTags(scraped.getTags() != null && !scraped.getTags().isBlank() ? scraped.getTags() : "Web • Edital Real • IA");
                    newOpp.setExternalUrl(scraped.getExternalUrl() != null ? scraped.getExternalUrl() : scraped.getUrl());
                    newOpp.setStatus(OpportunityStatus.PUBLICADA);
                    newOpp.setColor(resolveColorForType(oppType));

                    newOpp = opportunityRepository.save(newOpp);
                    newOpp.setCode("web-" + newOpp.getId());
                    newOpp = opportunityRepository.save(newOpp);
                    existingOpps.add(newOpp);
                    savedOppsCount++;
                }
            }
        }

        // 2. Salvar matches calculados para cada startup no JSON
        if (payload.getStartupMatches() != null) {
            for (AiDailySyncPayloadDTO.StartupMatchesBatchDTO batch : payload.getStartupMatches()) {
                StartupProfile profile = resolveStartupProfile(batch.getStartupId());
                if (profile == null) {
                    continue;
                }
                List<MatchResult> savedForStartup = persistMatchItemsForProfile(profile, batch.getMatches(), existingOpps);
                savedMatchesCount += savedForStartup.size();
            }
        }

        notificationRepository.save(new Notification(
                null,
                "Atualização diária das 08:00 concluída",
                "A IA realizou o scraping de oportunidades e atualizou os matches no banco de dados.",
                "Hoje, 08:00",
                "Matches",
                false
        ));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("status", "PERSISTED_IN_MYSQL");
        summary.put("timestamp", LocalDateTime.now().toString());
        summary.put("newOpportunitiesSaved", savedOppsCount);
        summary.put("matchesUpdated", savedMatchesCount);
        return summary;
    }

    @Transactional
    public List<MatchResultDTO> getMatchesForStartup(Long startupId) {
        StartupProfile profile = resolveStartupProfile(startupId);
        if (profile == null) {
            return List.of();
        }

        List<MatchResult> existing = matchResultRepository.findByStartupProfileIdOrderByMatchScoreDesc(profile.getId());
        if (existing.isEmpty()) {
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
        log.info("Calculando matches via IA Python (scrapeLiveWeb={}) para startup '{}' (ID {}) com {} oportunidades no MySQL",
                scrapeLiveWeb, profile.getName(), profile.getId(), opportunities.size());

        AiMatchResponseDTO aiResponse = aiIntegrationService.calculateMatches(profile, opportunities, scrapeLiveWeb);
        List<MatchResult> savedResults = persistMatchItemsForProfile(profile, aiResponse.getMatches(), opportunities);

        if (!savedResults.isEmpty()) {
            MatchResult best = savedResults.get(0);
            notificationRepository.save(new Notification(
                    null,
                    "Novo match encontrado",
                    "Uma oportunidade de " + best.getOpportunity().getType().name() + " teve " + best.getMatchScore() + "% de compatibilidade com sua startup.",
                    "Hoje, 08:00",
                    "Matches",
                    false
            ));
        }

        return savedResults.stream().map(MatchResultDTO::fromEntity).toList();
    }

    private List<MatchResult> persistMatchItemsForProfile(
            StartupProfile profile,
            List<AiMatchItemDTO> items,
            List<Opportunity> opportunities) {

        List<MatchResult> savedResults = new ArrayList<>();
        if (items == null) {
            return savedResults;
        }

        for (AiMatchItemDTO item : items) {
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
                        : "Inscrições abertas · Brasil · Remoto");
                newWebOpp.setTags(item.getOpportunityTags() != null && !item.getOpportunityTags().isBlank()
                        ? item.getOpportunityTags()
                        : "setor • tecnologia • objetivo");
                newWebOpp.setExternalUrl(item.getUrl());
                newWebOpp.setStatus(OpportunityStatus.PUBLICADA);
                newWebOpp.setColor(resolveColorForType(oppType));

                newWebOpp = opportunityRepository.save(newWebOpp);
                newWebOpp.setCode("web-" + newWebOpp.getId());
                matchedOpp = opportunityRepository.save(newWebOpp);
                opportunities.add(matchedOpp);
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
        return savedResults;
    }

    private String resolveColorForType(OpportunityType oppType) {
        return switch (oppType) {
            case EDITAL -> "bg-[#fff2b8] text-[#795900]";
            case POC -> "bg-[#d9f1e5] text-[#16734d]";
            case EVENTO -> "bg-[#dff1f5] text-[#1e7080]";
            case COMPRA -> "bg-[#ffe1c3] text-[#975524]";
        };
    }

    @Transactional
    public MatchResultDTO updateStatus(Long matchId, String newStatus) {
        Optional<MatchResult> byMatchId = matchResultRepository.findById(matchId);
        MatchResult match = byMatchId.orElseGet(() ->
                matchResultRepository.findAll().stream()
                        .filter(m -> m.getOpportunity() != null && Objects.equals(m.getOpportunity().getId(), matchId))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Match não encontrado: " + matchId))
        );

        match.setStatus(MatchStatus.fromString(newStatus));
        MatchResult saved = matchResultRepository.save(match);
        return MatchResultDTO.fromEntity(saved);
    }

    @Transactional
    public void unparticipateInOpportunity(Long startupId, String opportunityIdOrCode, Long matchId, String title) {
        if (matchId != null) {
            Optional<MatchResult> byMatchId = matchResultRepository.findById(matchId);
            if (byMatchId.isPresent()) {
                MatchResult m = byMatchId.get();
                m.setStatus(MatchStatus.NOVO);
                matchResultRepository.save(m);
            }
        }

        StartupProfile profile = resolveStartupProfile(startupId);
        Long targetStartupId = profile != null ? profile.getId() : null;

        List<MatchResult> allMatches = targetStartupId != null
                ? matchResultRepository.findByStartupProfileIdOrderByMatchScoreDesc(targetStartupId)
                : matchResultRepository.findAll();

        for (MatchResult m : allMatches) {
            boolean matchesId = false;
            if (opportunityIdOrCode != null && !opportunityIdOrCode.isBlank()) {
                if (String.valueOf(m.getId()).equals(opportunityIdOrCode)) {
                    matchesId = true;
                }
                if (m.getOpportunity() != null) {
                    if (String.valueOf(m.getOpportunity().getId()).equals(opportunityIdOrCode) ||
                        (m.getOpportunity().getCode() != null && m.getOpportunity().getCode().equalsIgnoreCase(opportunityIdOrCode))) {
                        matchesId = true;
                    }
                }
            }
            if (title != null && !title.isBlank() && m.getOpportunity() != null &&
                m.getOpportunity().getTitle() != null &&
                m.getOpportunity().getTitle().equalsIgnoreCase(title.trim())) {
                matchesId = true;
            }

            if (matchesId && m.getStatus() != MatchStatus.NOVO) {
                m.setStatus(MatchStatus.NOVO);
                matchResultRepository.save(m);
            }
        }
    }

    public List<Map<String, Object>> getCompanyToStartupsMatches(Long companyId) {
        Company company = resolveCompany(companyId);
        List<StartupProfile> startups = startupProfileRepository.findAll();
        return aiIntegrationService.calculateCompanyToStartupsMatches(company, startups);
    }

    public List<Map<String, Object>> getStartupToCompaniesMatches(Long startupId) {
        StartupProfile startup = resolveStartupProfile(startupId);
        List<Company> companies = companyRepository.findAll();
        return aiIntegrationService.calculateStartupToCompaniesMatches(startup, companies);
    }

    private Company resolveCompany(Long companyId) {
        if (companyId != null) {
            Optional<Company> byId = companyRepository.findById(companyId);
            if (byId.isPresent()) return byId.get();
            Optional<Company> byUser = companyRepository.findByUserId(companyId);
            if (byUser.isPresent()) return byUser.get();
        }
        return companyRepository.findAll().stream().findFirst().orElse(null);
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
