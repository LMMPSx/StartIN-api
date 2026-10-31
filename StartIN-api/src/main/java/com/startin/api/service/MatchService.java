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
                if (!savedForStartup.isEmpty()) {
                    MatchResult top = savedForStartup.get(0);
                    Long stUserId = profile.getUser() != null ? profile.getUser().getId() : null;
                    String oppIdStr = top.getOpportunity() != null ? String.valueOf(top.getOpportunity().getId()) : null;
                    notificationRepository.save(Notification.forStartup(
                            profile.getId(),
                            stUserId,
                            "Atualização diária de matches concluída",
                            "A IA atualizou suas recomendações (melhor aderência: " + top.getMatchScore() + "% em '" + top.getOpportunity().getTitle() + "').",
                            "Matches",
                            oppIdStr != null ? "opportunity" : "for-you",
                            oppIdStr
                    ));
                }
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("status", "PERSISTED_IN_MYSQL");
        summary.put("timestamp", LocalDateTime.now().toString());
        summary.put("newOpportunitiesSaved", savedOppsCount);
        summary.put("matchesUpdated", savedMatchesCount);
        return summary;
    }

    public static boolean isOpportunityCompatibleWithStage(Opportunity opp, String stage) {
        if (opp == null || opp.getType() == null || stage == null || stage.isBlank()) {
            return true;
        }
        String norm = stage.trim().toLowerCase()
                .replace("ç", "c")
                .replace("ã", "a")
                .replace("á", "a");
        OpportunityType type = opp.getType();
        if (norm.contains("tracao")) {
            return type != OpportunityType.POC && type != OpportunityType.PRIMEIRA_COMPRA;
        }
        if (norm.contains("validacao")) {
            return type != OpportunityType.COMPRA;
        }
        return true;
    }

    @Transactional
    public List<MatchResultDTO> getMatchesForStartup(Long startupId) {
        StartupProfile profile = resolveStartupProfile(startupId);
        if (profile == null) {
            return List.of();
        }

        List<MatchResult> existing = matchResultRepository.findByStartupProfileIdOrderByMatchScoreDesc(profile.getId());
        long compatibleInDb = opportunityRepository.findAll().stream()
                .filter(o -> isOpportunityCompatibleWithStage(o, profile.getCurrentStage()))
                .count();
        long compatibleExisting = existing.stream()
                .filter(m -> isOpportunityCompatibleWithStage(m.getOpportunity(), profile.getCurrentStage()))
                .count();
        boolean hasLegacyUnauditedRows = existing.stream()
                .anyMatch(m -> m.getReasonProfile() == null || !m.getReasonProfile().contains("/40"));

        if (existing.isEmpty() || compatibleExisting < compatibleInDb || hasLegacyUnauditedRows) {
            return recalculateMatches(profile.getId(), existing.isEmpty());
        }

        return existing.stream()
                .filter(m -> isOpportunityCompatibleWithStage(m.getOpportunity(), profile.getCurrentStage()))
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

        List<Opportunity> opportunities = new ArrayList<>(
                opportunityRepository.findAll().stream()
                        .filter(o -> isOpportunityCompatibleWithStage(o, profile.getCurrentStage()))
                        .toList()
        );
        log.info("Calculando matches via IA Python (scrapeLiveWeb={}) para startup '{}' (ID {}) com {} oportunidades compatíveis no MySQL",
                scrapeLiveWeb, profile.getName(), profile.getId(), opportunities.size());

        AiMatchResponseDTO aiResponse = aiIntegrationService.calculateMatches(profile, opportunities, scrapeLiveWeb);
        List<MatchResult> savedResults = persistMatchItemsForProfile(profile, aiResponse.getMatches(), opportunities);

        if (!savedResults.isEmpty()) {
            MatchResult best = savedResults.get(0);
            Long stUserId = profile.getUser() != null ? profile.getUser().getId() : null;
            String oppIdStr = best.getOpportunity() != null ? String.valueOf(best.getOpportunity().getId()) : null;
            notificationRepository.save(Notification.forStartup(
                    profile.getId(),
                    stUserId,
                    "Novo match encontrado: " + best.getOpportunity().getTitle(),
                    "Uma oportunidade de " + best.getOpportunity().getType().name() + " teve " + best.getMatchScore() + "% de compatibilidade com sua startup.",
                    "Matches",
                    oppIdStr != null ? "opportunity" : "for-you",
                    oppIdStr
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

        int existingMatchesCount = matchResultRepository.findByStartupProfileIdOrderByMatchScoreDesc(profile.getId()).size();
        int baselineCumulative = Math.max(
                profile.getCumulativeOpportunitiesFound() != null ? profile.getCumulativeOpportunitiesFound() : 0,
                existingMatchesCount
        );
        int newlyCreatedInThisRun = 0;

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

            if (matchedOpp != null && isOpportunityCompatibleWithStage(matchedOpp, profile.getCurrentStage())) {
                Optional<MatchResult> existing = matchResultRepository.findByStartupProfileIdAndOpportunityId(profile.getId(), matchedOpp.getId());
                if (existing.isEmpty()) {
                    newlyCreatedInThisRun++;
                }
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

        profile.setCumulativeOpportunitiesFound(baselineCumulative + newlyCreatedInThisRun);
        profile.setLastUpdateNewMatchesCount(newlyCreatedInThisRun);
        startupProfileRepository.save(profile);

        savedResults.sort((a, b) -> Integer.compare(b.getMatchScore(), a.getMatchScore()));
        return savedResults;
    }

    private String resolveColorForType(OpportunityType oppType) {
        return switch (oppType) {
            case EDITAL -> "bg-[#fff2b8] text-[#795900]";
            case POC -> "bg-[#d9f1e5] text-[#16734d]";
            case PRIMEIRA_COMPRA -> "bg-[#fef3c7] text-[#92400e]";
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
        if (targetStartupId == null) {
            return;
        }

        List<MatchResult> allMatches = matchResultRepository.findByStartupProfileIdOrderByMatchScoreDesc(targetStartupId);

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
        List<Map<String, Object>> rawMatches = new ArrayList<>(
                aiIntegrationService.calculateCompanyToStartupsMatches(company, startups)
        );

        Map<Long, List<MatchResult>> matchesByStartupId = new HashMap<>();
        if (company != null && company.getId() != null) {
            List<MatchResult> companyOppMatches = matchResultRepository.findByOpportunityCompanyIdOrderByMatchScoreDesc(company.getId());
            for (MatchResult mr : companyOppMatches) {
                if (mr.getStartupProfile() != null && mr.getStartupProfile().getId() != null) {
                    matchesByStartupId
                            .computeIfAbsent(mr.getStartupProfile().getId(), k -> new ArrayList<>())
                            .add(mr);
                }
            }
        }

        List<Map<String, Object>> enriched = new ArrayList<>();
        for (Map<String, Object> rawItem : rawMatches) {
            Map<String, Object> item = new LinkedHashMap<>(rawItem);
            Long stId = null;
            if (item.get("startupId") != null) {
                try {
                    stId = Long.valueOf(String.valueOf(item.get("startupId")));
                } catch (NumberFormatException ignored) {}
            }

            List<MatchResult> companyRows = stId != null ? matchesByStartupId.getOrDefault(stId, List.of()) : List.of();
            List<Map<String, Object>> interestedOpps = new ArrayList<>();
            for (MatchResult mr : companyRows) {
                if (mr.getStatus() != null && mr.getStatus() != MatchStatus.NOVO && mr.getOpportunity() != null) {
                    Map<String, Object> oppInfo = new LinkedHashMap<>();
                    oppInfo.put("opportunityId", mr.getOpportunity().getId());
                    oppInfo.put("title", mr.getOpportunity().getTitle());
                    oppInfo.put("type", mr.getOpportunity().getType() != null ? mr.getOpportunity().getType().name() : "POC");
                    oppInfo.put("status", mr.getStatus().name());
                    oppInfo.put("matchScore", mr.getMatchScore());
                    interestedOpps.add(oppInfo);
                }
            }

            item.put("interestedInCompanyPoc", !interestedOpps.isEmpty());
            item.put("interestedOpportunities", interestedOpps);
            item.put("matchedCompanyOpportunitiesCount", companyRows.size());
            enriched.add(item);
        }

        enriched.sort((a, b) -> {
            boolean intA = Boolean.TRUE.equals(a.get("interestedInCompanyPoc"));
            boolean intB = Boolean.TRUE.equals(b.get("interestedInCompanyPoc"));
            if (intA != intB) {
                return intB ? 1 : -1;
            }
            int scoreA = a.get("matchScore") instanceof Number nA ? nA.intValue() : 0;
            int scoreB = b.get("matchScore") instanceof Number nB ? nB.intValue() : 0;
            return Integer.compare(scoreB, scoreA);
        });

        return enriched;
    }

    @Transactional
    public Map<String, Object> expressInterestInCompany(Long startupId, Long companyId) {
        StartupProfile profile = resolveStartupProfile(startupId);
        Company company = resolveCompany(companyId);
        if (profile == null || company == null) {
            throw new IllegalArgumentException("Startup ou Empresa não encontrada.");
        }

        List<Opportunity> companyOpps = opportunityRepository.findByCompanyIdOrderByIdDesc(company.getId());
        List<Opportunity> compatibleOpps = companyOpps.stream()
                .filter(o -> isOpportunityCompatibleWithStage(o, profile.getCurrentStage()))
                .toList();

        List<Opportunity> targets = !compatibleOpps.isEmpty() ? compatibleOpps : companyOpps;
        int registeredCount = 0;
        for (Opportunity opp : targets) {
            participateInOpportunity(profile.getId(), String.valueOf(opp.getId()));
            registeredCount++;
        }

        if (registeredCount == 0) {
            Long stUserId = profile.getUser() != null ? profile.getUser().getId() : null;
            notificationRepository.save(Notification.forStartup(
                    profile.getId(),
                    stUserId,
                    "Interesse enviado para " + company.getName(),
                    "Você manifestou interesse em participar de oportunidades de POC / Compra da " + company.getName() + ".",
                    "Matches",
                    "company-detail",
                    String.valueOf(company.getId())
            ));
        }

        // Notificação para o perfil da Empresa sobre a startup que deu match / enviou interesse
        Long compUserId = company.getUser() != null ? company.getUser().getId() : null;
        notificationRepository.save(Notification.forCompany(
                company.getId(),
                compUserId,
                "Startup interessada: " + profile.getName(),
                "A startup " + profile.getName() + " manifestou interesse em conectar para POC / Compra com sua empresa.",
                "Matches",
                "startup-detail",
                String.valueOf(profile.getId())
        ));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("registered", true);
        res.put("companyId", company.getId());
        res.put("companyName", company.getName());
        res.put("opportunitiesLinked", registeredCount);
        return res;
    }

    @Transactional
    public Map<String, Object> expressInterestInStartup(Long companyId, Long startupId) {
        Company company = resolveCompany(companyId);
        StartupProfile profile = resolveStartupProfile(startupId);
        if (company == null || profile == null) {
            throw new IllegalArgumentException("Empresa ou Startup não encontrada.");
        }

        Long stUserId = profile.getUser() != null ? profile.getUser().getId() : null;
        Long compUserId = company.getUser() != null ? company.getUser().getId() : null;

        // Notifica a Startup que uma Empresa fez match / demonstrou interesse nela
        notificationRepository.save(Notification.forStartup(
                profile.getId(),
                stUserId,
                company.getName() + " fez match com sua startup!",
                "A empresa " + company.getName() + " analisou sua solução e manifestou interesse para POC / Compra.",
                "Matches",
                "company-detail",
                String.valueOf(company.getId())
        ));

        // Registra confirmação nas notificações da própria Empresa
        notificationRepository.save(Notification.forCompany(
                company.getId(),
                compUserId,
                "Interesse enviado para " + profile.getName(),
                "Sua empresa registrou interesse de conexão corporativa com a startup " + profile.getName() + ".",
                "Matches",
                "startup-detail",
                String.valueOf(profile.getId())
        ));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("registered", true);
        res.put("companyId", company.getId());
        res.put("companyName", company.getName());
        res.put("startupId", profile.getId());
        res.put("startupName", profile.getName());
        return res;
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
                    AiMatchResponseDTO singleCalc = aiIntegrationService.calculateMatches(profile, List.of(targetOpp), false);
                    AiMatchItemDTO calcItem = (singleCalc.getMatches() != null && !singleCalc.getMatches().isEmpty())
                            ? singleCalc.getMatches().get(0)
                            : null;

                    MatchResult m = new MatchResult();
                    m.setStartupProfile(profile);
                    m.setOpportunity(targetOpp);
                    m.setMatchScore(calcItem != null && calcItem.getMatchScore() != null ? calcItem.getMatchScore() : 45);
                    m.setJustification(calcItem != null && calcItem.getJustification() != null
                            ? calcItem.getJustification()
                            : "Interesse registrado diretamente pela startup " + profile.getName() + ".");
                    m.setRecommendedAction(calcItem != null && calcItem.getRecommendedAction() != null
                            ? calcItem.getRecommendedAction()
                            : "Acompanhar retorno da organização responsável.");
                    if (calcItem != null && calcItem.getReasons() != null) {
                        m.setReasonProfile(calcItem.getReasons().getPerfil());
                        m.setReasonRequirements(calcItem.getReasons().getRequisitos());
                        m.setReasonObjective(calcItem.getReasons().getObjetivo());
                        m.setReasonContext(calcItem.getReasons().getContexto());
                    } else {
                        m.setReasonProfile((profile.getMainArea() != null ? profile.getMainArea() : "Inovação") + " · " + (profile.getBusinessModel() != null ? profile.getBusinessModel() : "B2B"));
                        m.setReasonRequirements(profile.getTechnologies() != null ? profile.getTechnologies() : "Requisitos técnicos avaliados");
                        m.setReasonObjective(profile.getMainObjective() != null ? profile.getMainObjective() : "Validação e negócios");
                        m.setReasonContext(profile.getLocation() != null ? profile.getLocation() : "Brasil");
                    }
                    return m;
                });

        match.setStatus(MatchStatus.INTERESSE_ENVIADO);
        MatchResult saved = matchResultRepository.save(match);

        Long stUserId = profile.getUser() != null ? profile.getUser().getId() : null;
        notificationRepository.save(Notification.forStartup(
                profile.getId(),
                stUserId,
                "Interesse salvo: " + targetOpp.getTitle(),
                "Oportunidade salva na sua lista. Clique para revisar os detalhes ou gerar proposta na IA.",
                "Oportunidades",
                "opportunity",
                String.valueOf(targetOpp.getId())
        ));

        // Se a oportunidade pertence a uma Empresa, notifica o perfil da Empresa com redirecionamento direto à Startup!
        if (targetOpp.getCompany() != null) {
            Company offeringComp = targetOpp.getCompany();
            Long compUserId = offeringComp.getUser() != null ? offeringComp.getUser().getId() : null;
            notificationRepository.save(Notification.forCompany(
                    offeringComp.getId(),
                    compUserId,
                    "Novo match em '" + targetOpp.getTitle() + "'",
                    "A startup " + profile.getName() + " salvou interesse em participar desta oportunidade.",
                    "Matches",
                    "startup-detail",
                    String.valueOf(profile.getId())
            ));
        }

        return MatchResultDTO.fromEntity(saved);
    }

    private StartupProfile resolveStartupProfile(Long startupId) {
        if (startupId != null) {
            Optional<StartupProfile> byId = startupProfileRepository.findById(startupId);
            if (byId.isPresent()) return byId.get();
            Optional<StartupProfile> byUser = startupProfileRepository.findByUserId(startupId);
            return byUser.orElse(null);
        }
        return startupProfileRepository.findAll().stream().findFirst().orElse(null);
    }
}
