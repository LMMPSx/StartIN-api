package com.startin.api.service;

import com.startin.api.dto.dashboard.DashboardSummaryDTO;
import com.startin.api.dto.opportunity.OpportunityDTO;
import com.startin.api.model.Company;
import com.startin.api.model.MatchResult;
import com.startin.api.model.Notification;
import com.startin.api.model.Opportunity;
import com.startin.api.model.StartupProfile;
import com.startin.api.model.enums.MatchStatus;
import com.startin.api.repository.CompanyRepository;
import com.startin.api.repository.MatchResultRepository;
import com.startin.api.repository.NotificationRepository;
import com.startin.api.repository.OpportunityRepository;
import com.startin.api.repository.StartupProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class DashboardService {

    private final StartupProfileRepository startupProfileRepository;
    private final CompanyRepository companyRepository;
    private final OpportunityRepository opportunityRepository;
    private final MatchResultRepository matchResultRepository;
    private final NotificationRepository notificationRepository;
    private final MatchService matchService;

    public DashboardService(
            StartupProfileRepository startupProfileRepository,
            CompanyRepository companyRepository,
            OpportunityRepository opportunityRepository,
            MatchResultRepository matchResultRepository,
            NotificationRepository notificationRepository,
            MatchService matchService) {
        this.startupProfileRepository = startupProfileRepository;
        this.companyRepository = companyRepository;
        this.opportunityRepository = opportunityRepository;
        this.matchResultRepository = matchResultRepository;
        this.notificationRepository = notificationRepository;
        this.matchService = matchService;
    }

    @Transactional
    public DashboardSummaryDTO getSummary(Long startupId) {
        return getSummary(startupId, null);
    }

    @Transactional
    public DashboardSummaryDTO getSummary(Long startupId, Long userId) {
        StartupProfile profile = null;
        if (userId != null && userId > 0) {
            profile = startupProfileRepository.findByUserId(userId).orElse(null);
        }
        if (profile == null && startupId != null && startupId > 0) {
            StartupProfile found = startupProfileRepository.findById(startupId).orElse(null);
            if (found != null && (userId == null || userId <= 0 || found.getUser() == null || Objects.equals(found.getUser().getId(), userId))) {
                profile = found;
            } else {
                profile = startupProfileRepository.findByUserId(startupId).orElse(null);
            }
        }
        if (profile == null && startupId == null && userId == null) {
            profile = startupProfileRepository.findAll().stream().findFirst().orElse(null);
        }

        String name = (profile != null && profile.getName() != null && !profile.getName().isBlank())
                ? profile.getName()
                : "Minha Startup";
        String stage = profile != null ? profile.getCurrentStage() : null;

        DashboardSummaryDTO summary = new DashboardSummaryDTO();
        summary.setStartupName(name);
        summary.setGreeting("Olá, " + name + " 👋");

        List<Opportunity> allOpportunities = opportunityRepository.findAll().stream()
                .filter(o -> MatchService.isOpportunityCompatibleWithStage(o, stage))
                .toList();

        List<MatchResult> allUserMatches = profile != null
                ? matchResultRepository.findByStartupProfileIdOrderByMatchScoreDesc(profile.getId())
                : List.of();

        List<MatchResult> userMatches = allUserMatches.stream()
                .filter(m -> MatchService.isOpportunityCompatibleWithStage(m.getOpportunity(), stage))
                .toList();

        int storedCumulative = (profile != null && profile.getCumulativeOpportunitiesFound() != null)
                ? profile.getCumulativeOpportunitiesFound()
                : 0;
        int opportunitiesFoundCount = Math.max(storedCumulative, allUserMatches.size());
        if (opportunitiesFoundCount == 0 && !allOpportunities.isEmpty()) {
            opportunitiesFoundCount = allOpportunities.size();
        }

        int newMatchesLastUpdate;
        if (profile != null && storedCumulative > 0 && profile.getLastUpdateNewMatchesCount() != null) {
            newMatchesLastUpdate = profile.getLastUpdateNewMatchesCount();
        } else {
            newMatchesLastUpdate = !userMatches.isEmpty() ? userMatches.size() : allOpportunities.size();
        }

        if (profile != null && storedCumulative == 0 && opportunitiesFoundCount > 0) {
            profile.setCumulativeOpportunitiesFound(opportunitiesFoundCount);
            profile.setLastUpdateNewMatchesCount(newMatchesLastUpdate);
            startupProfileRepository.save(profile);
        }

        int companyMatchesCount = (int) companyRepository.count();

        long trackingCount = allUserMatches.stream()
                .filter(m -> m.getStatus() != null && m.getStatus() != MatchStatus.NOVO)
                .count();

        long savedCount = allUserMatches.stream()
                .filter(m -> m.getStatus() == MatchStatus.SALVO || m.getStatus() == MatchStatus.INTERESSE_ENVIADO)
                .count();

        summary.setOpportunitiesFoundCount(opportunitiesFoundCount);
        summary.setCompanyMatchesCount(companyMatchesCount);
        summary.setNewMatchesLastUpdateCount(newMatchesLastUpdate);
        summary.setSavedOpportunitiesCount((int) savedCount);
        summary.setNewMatchesCount(newMatchesLastUpdate);
        summary.setDeadlinesThisWeekCount(allOpportunities.size());
        summary.setTrackingCount((int) trackingCount);
        summary.setPendingActionsCount(newMatchesLastUpdate + (int) trackingCount);
        summary.setPendingActionsText(
                opportunitiesFoundCount + " oportunidades encontradas · " +
                companyMatchesCount + " empresas com match · " +
                newMatchesLastUpdate + " matches novos na última atualização"
        );

        List<Map<String, String>> deadlines = new ArrayList<>();
        for (Opportunity opp : allOpportunities.stream().limit(4).toList()) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("id", String.valueOf(opp.getId()));
            item.put("type", opp.getType() != null ? opp.getType().name() : "EDITAL");
            item.put("title", opp.getTitle());
            item.put("subtitle", opp.getMetaInfo() != null ? opp.getMetaInfo() : "Inscrições abertas");
            if (opp.getCompany() != null) {
                item.put("companyId", String.valueOf(opp.getCompany().getId()));
                item.put("companyName", opp.getCompany().getName());
            }
            deadlines.add(item);
        }
        summary.setUpcomingDeadlines(deadlines);

        List<Map<String, String>> activities = new ArrayList<>();
        for (MatchResult m : userMatches.stream().limit(2).toList()) {
            activities.add(Map.of(
                    "text", "Match IA (" + m.getMatchScore() + "%): " + name + " × " + m.getOpportunity().getTitle(),
                    "time", "Calculado pela IA"
            ));
        }
        for (Notification n : notificationRepository.findAllByOrderByCreatedAtDesc().stream().limit(3).toList()) {
            if (activities.size() >= 4) break;
            activities.add(Map.of(
                    "text", n.getTitle() + " — " + n.getDescription(),
                    "time", n.getTimeAgo() != null ? n.getTimeAgo() : "Recente"
            ));
        }
        summary.setRecentActivities(activities);

        return summary;
    }

    public Map<String, Object> getCompanySummary(Long companyId) {
        Company company = null;
        if (companyId != null) {
            company = companyRepository.findById(companyId)
                    .or(() -> companyRepository.findByUserId(companyId))
                    .orElse(null);
        }
        if (company == null && companyId == null) {
            company = companyRepository.findAll().stream().findFirst().orElse(null);
        }

        String companyName = (company != null && company.getName() != null && !company.getName().isBlank())
                ? company.getName()
                : "Minha Empresa";
        Long resolvedCompanyId = company != null ? company.getId() : null;

        List<Opportunity> companyOpps = resolvedCompanyId != null
                ? opportunityRepository.findByCompanyIdOrderByIdDesc(resolvedCompanyId)
                : List.of();

        List<OpportunityDTO> recentOpportunities = companyOpps.stream()
                .map(OpportunityDTO::fromEntity)
                .toList();

        List<Map<String, Object>> startupMatchesHistory = matchService.getCompanyToStartupsMatches(resolvedCompanyId);
        int matchedStartupsCount = startupMatchesHistory.size();
        int pocsPurchasesCreatedCount = companyOpps.size();

        List<Map<String, Object>> recentStartupMatches = startupMatchesHistory.stream()
                .limit(4)
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("companyId", resolvedCompanyId);
        result.put("companyName", companyName);
        result.put("greeting", "Olá, " + companyName + " 👋");
        result.put("matchedStartupsCount", matchedStartupsCount);
        result.put("pocsPurchasesCreatedCount", pocsPurchasesCreatedCount);
        result.put("bannerTitle", matchedStartupsCount + " startups com match · " + pocsPurchasesCreatedCount + " POCs / Compras lançadas");
        result.put("bannerText", "Gerencie suas POCs e Compras abertas clicando para editar ou visualize o histórico completo de startups com match.");
        result.put("recentOpportunities", recentOpportunities);
        result.put("recentStartupMatches", recentStartupMatches);
        result.put("startupMatchesHistory", startupMatchesHistory);
        return result;
    }
}
