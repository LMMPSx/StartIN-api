package com.startin.api.service;

import com.startin.api.dto.dashboard.DashboardSummaryDTO;
import com.startin.api.model.MatchResult;
import com.startin.api.model.Notification;
import com.startin.api.model.Opportunity;
import com.startin.api.model.StartupProfile;
import com.startin.api.model.enums.MatchStatus;
import com.startin.api.repository.MatchResultRepository;
import com.startin.api.repository.NotificationRepository;
import com.startin.api.repository.OpportunityRepository;
import com.startin.api.repository.StartupProfileRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final StartupProfileRepository startupProfileRepository;
    private final OpportunityRepository opportunityRepository;
    private final MatchResultRepository matchResultRepository;
    private final NotificationRepository notificationRepository;

    public DashboardService(
            StartupProfileRepository startupProfileRepository,
            OpportunityRepository opportunityRepository,
            MatchResultRepository matchResultRepository,
            NotificationRepository notificationRepository) {
        this.startupProfileRepository = startupProfileRepository;
        this.opportunityRepository = opportunityRepository;
        this.matchResultRepository = matchResultRepository;
        this.notificationRepository = notificationRepository;
    }

    public DashboardSummaryDTO getSummary(Long startupId) {
        StartupProfile profile = startupId != null
                ? startupProfileRepository.findById(startupId)
                    .or(() -> startupProfileRepository.findByUserId(startupId))
                    .orElseGet(() -> startupProfileRepository.findAll().stream().findFirst().orElse(null))
                : startupProfileRepository.findAll().stream().findFirst().orElse(null);

        String name = (profile != null && profile.getName() != null && !profile.getName().isBlank())
                ? profile.getName()
                : "Minha Startup";

        DashboardSummaryDTO summary = new DashboardSummaryDTO();
        summary.setStartupName(name);
        summary.setGreeting("Olá, " + name + " 👋");

        List<Opportunity> allOpportunities = opportunityRepository.findAll();
        List<MatchResult> userMatches = profile != null
                ? matchResultRepository.findByStartupProfileIdOrderByMatchScoreDesc(profile.getId())
                : List.of();

        long savedCount = userMatches.stream()
                .filter(m -> m.getStatus() == MatchStatus.SALVO || m.getStatus() == MatchStatus.INTERESSE_ENVIADO)
                .count();
        long newMatchesCount = userMatches.stream()
                .filter(m -> m.getStatus() == MatchStatus.NOVO)
                .count();
        long trackingCount = userMatches.stream()
                .filter(m -> m.getStatus() != MatchStatus.NOVO)
                .count();
        long deadlinesCount = allOpportunities.size();

        if (userMatches.isEmpty() && !allOpportunities.isEmpty()) {
            newMatchesCount = allOpportunities.size();
        }

        long pendingActions = newMatchesCount + Math.min(deadlinesCount, 2);

        summary.setSavedOpportunitiesCount((int) (savedCount > 0 ? savedCount : userMatches.size()));
        summary.setNewMatchesCount((int) newMatchesCount);
        summary.setDeadlinesThisWeekCount((int) deadlinesCount);
        summary.setTrackingCount((int) trackingCount);
        summary.setPendingActionsCount((int) pendingActions);
        summary.setPendingActionsText(
                newMatchesCount + " matches calculados · " +
                deadlinesCount + " oportunidades ativas · " +
                trackingCount + " em acompanhamento"
        );

        List<Map<String, String>> deadlines = new ArrayList<>();
        for (Opportunity opp : allOpportunities.stream().limit(4).toList()) {
            deadlines.add(Map.of(
                    "id", String.valueOf(opp.getId()),
                    "type", opp.getType() != null ? opp.getType().name() : "EDITAL",
                    "title", opp.getTitle(),
                    "subtitle", opp.getMetaInfo() != null ? opp.getMetaInfo() : "Inscrições abertas"
            ));
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
}
