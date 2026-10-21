package com.startin.api.controller;

import com.startin.api.dto.dashboard.DashboardSummaryDTO;
import com.startin.api.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Endpoints com métricas, resumo de atividade e ações pendentes")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Retorna visão geral do dashboard com indicadores e próximos prazos")
    public ResponseEntity<DashboardSummaryDTO> getSummary(@RequestParam(required = false) Long startupId) {
        return ResponseEntity.ok(dashboardService.getSummary(startupId));
    }
}
