package com.startin.api.service;

import com.startin.api.dto.dashboard.DashboardSummaryDTO;
import com.startin.api.model.StartupProfile;
import com.startin.api.model.enums.MatchStatus;
import com.startin.api.repository.MatchResultRepository;
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

    public DashboardService(
            StartupProfileRepository startupProfileRepository,
            OpportunityRepository opportunityRepository,
            MatchResultRepository matchResultRepository) {
        this.startupProfileRepository = startupProfileRepository;
        this.opportunityRepository = opportunityRepository;
        this.matchResultRepository = matchResultRepository;
    }

    public DashboardSummaryDTO getSummary(Long startupId) {
        StartupProfile profile = startupId != null
                ? startupProfileRepository.findById(startupId).orElse(null)
                : startupProfileRepository.findAll().stream().findFirst().orElse(null);

        String name = profile != null ? profile.getName() : "Nexa Health";

        DashboardSummaryDTO summary = new DashboardSummaryDTO();
        summary.setStartupName(name);
        summary.setGreeting("Olá, " + name + " 👋");

        long savedCount = 12;
        long newMatches = 4;
        if (profile != null) {
            long realNew = matchResultRepository.countByStartupProfileIdAndStatus(profile.getId(), MatchStatus.NOVO);
            if (realNew > 0) newMatches = realNew;
        }

        summary.setSavedOpportunitiesCount((int) savedCount);
        summary.setNewMatchesCount((int) newMatches);
        summary.setDeadlinesThisWeekCount(2);
        summary.setTrackingCount(7);
        summary.setPendingActionsCount(4);
        summary.setPendingActionsText("2 novos matches · 1 prazo próximo · 1 oportunidade atualizada");

        List<Map<String, String>> deadlines = new ArrayList<>();
        deadlines.add(Map.of(
                "type", "EDITAL",
                "title", "Inovação Aberta 2026",
                "subtitle", "Inscrições encerram em 5 dias"
        ));
        deadlines.add(Map.of(
                "type", "POC",
                "title", "Gestão Inteligente",
                "subtitle", "Resposta necessária até sexta-feira"
        ));
        summary.setUpcomingDeadlines(deadlines);

        List<Map<String, String>> activities = new ArrayList<>();
        activities.add(Map.of(
                "text", "Novo match: " + name + " × Empresa Alpha",
                "time", "há 2 horas"
        ));
        activities.add(Map.of(
                "text", "Edital Inova+ teve atualização de prazo",
                "time", "ontem"
        ));
        activities.add(Map.of(
                "text", "Seu perfil foi atualizado",
                "time", "18 set, 14:10"
        ));
        summary.setRecentActivities(activities);

        return summary;
    }
}
