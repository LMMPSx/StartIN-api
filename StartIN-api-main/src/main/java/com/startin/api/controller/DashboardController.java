package com.startin.api.controller;

import com.startin.api.dto.dashboard.DashboardSummaryDTO;
import com.startin.api.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Endpoints com métricas, resumo de atividade e ações pendentes")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Retorna visão geral do dashboard da startup com indicadores e próximos prazos")
    public ResponseEntity<DashboardSummaryDTO> getSummary(@RequestParam(required = false) Long startupId) {
        return ResponseEntity.ok(dashboardService.getSummary(startupId));
    }

    @GetMapping("/company-summary")
    @Operation(summary = "Retorna visão geral do dashboard corporativo da empresa (startups com match e POCs/Compras lançadas)")
    public ResponseEntity<Map<String, Object>> getCompanySummary(@RequestParam(required = false) Long companyId) {
        return ResponseEntity.ok(dashboardService.getCompanySummary(companyId));
    }
}
