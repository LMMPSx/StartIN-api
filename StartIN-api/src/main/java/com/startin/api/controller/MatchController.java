package com.startin.api.controller;

import com.startin.api.dto.match.MatchResultDTO;
import com.startin.api.dto.match.MatchTriggerRequestDTO;
import com.startin.api.dto.match.UpdateMatchStatusDTO;
import com.startin.api.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/matches")
@Tag(name = "Matches & IA", description = "Endpoints de compatibilidade calculados pelo motor de IA Python")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping
    @Operation(summary = "Lista todos os matches calculados para a startup")
    public ResponseEntity<List<MatchResultDTO>> getMatches(@RequestParam(required = false) Long startupId) {
        return ResponseEntity.ok(matchService.getMatchesForStartup(startupId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca detalhes de um match com seus 4 pilares explicativos")
    public ResponseEntity<MatchResultDTO> getById(@PathVariable Long id) {
        return matchService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/recalculate")
    @Operation(summary = "Aciona o microserviço de IA em Python para raspar editais da web e recalcular matches em tempo real")
    public ResponseEntity<List<MatchResultDTO>> recalculate(@RequestBody(required = false) MatchTriggerRequestDTO request) {
        Long startupId = (request != null) ? request.getStartupId() : null;
        boolean scrape = request == null || request.getScrapeLiveWeb() == null || Boolean.TRUE.equals(request.getScrapeLiveWeb());
        return ResponseEntity.ok(matchService.recalculateMatches(startupId, scrape));
    }

    @PostMapping("/participate")
    @Operation(summary = "Registra interesse/participação da startup em uma oportunidade")
    public ResponseEntity<MatchResultDTO> participate(@RequestBody Map<String, Object> payload) {
        Long startupId = payload.get("startupId") != null ? Long.valueOf(String.valueOf(payload.get("startupId"))) : null;
        String oppId = String.valueOf(payload.get("opportunityId"));
        return ResponseEntity.ok(matchService.participateInOpportunity(startupId, oppId));
    }

    @PostMapping("/unparticipate")
    @Operation(summary = "Remove o interesse/salvamento de uma oportunidade ou match no banco de dados")
    public ResponseEntity<Map<String, Object>> unparticipate(@RequestBody Map<String, Object> payload) {
        Long startupId = payload.get("startupId") != null ? Long.valueOf(String.valueOf(payload.get("startupId"))) : null;
        Long matchId = null;
        if (payload.get("matchId") != null) {
            try {
                matchId = Long.valueOf(String.valueOf(payload.get("matchId")));
            } catch (NumberFormatException ignored) {}
        }
        String oppId = payload.get("opportunityId") != null ? String.valueOf(payload.get("opportunityId")) : null;
        String title = payload.get("title") != null ? String.valueOf(payload.get("title")) : null;
        matchService.unparticipateInOpportunity(startupId, oppId, matchId, title);
        return ResponseEntity.ok(Map.of("removed", true));
    }

    @GetMapping("/company-startups")
    @Operation(summary = "Lista todas as startups cadastradas com o match calculado pela IA para a empresa")
    public ResponseEntity<List<Map<String, Object>>> getCompanyStartupsMatches(
            @RequestParam(required = false) Long companyId) {
        return ResponseEntity.ok(matchService.getCompanyToStartupsMatches(companyId));
    }

    @GetMapping("/startup-companies")
    @Operation(summary = "Lista todas as empresas cadastradas com o match calculado pela IA para a startup")
    public ResponseEntity<List<Map<String, Object>>> getStartupCompaniesMatches(
            @RequestParam(required = false) Long startupId) {
        return ResponseEntity.ok(matchService.getStartupToCompaniesMatches(startupId));
    }

    @PostMapping("/company-interest")
    @Operation(summary = "Registra interesse direto da startup nas POCs/Compras de uma empresa")
    public ResponseEntity<Map<String, Object>> expressCompanyInterest(@RequestBody Map<String, Object> payload) {
        Long startupId = payload.get("startupId") != null ? Long.valueOf(String.valueOf(payload.get("startupId"))) : null;
        Long companyId = payload.get("companyId") != null ? Long.valueOf(String.valueOf(payload.get("companyId"))) : null;
        return ResponseEntity.ok(matchService.expressInterestInCompany(startupId, companyId));
    }

    @PostMapping("/startup-interest")
    @Operation(summary = "Registra match/interesse direto de uma empresa em uma startup compatível")
    public ResponseEntity<Map<String, Object>> expressStartupInterest(@RequestBody Map<String, Object> payload) {
        Long companyId = payload.get("companyId") != null ? Long.valueOf(String.valueOf(payload.get("companyId"))) : null;
        Long startupId = payload.get("startupId") != null ? Long.valueOf(String.valueOf(payload.get("startupId"))) : null;
        return ResponseEntity.ok(matchService.expressInterestInStartup(companyId, startupId));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualiza o status da conexão (SALVO, INTERESSE_ENVIADO, CONTATO_INICIADO, EM_NEGOCIACAO)")
    public ResponseEntity<MatchResultDTO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMatchStatusDTO dto) {
        return ResponseEntity.ok(matchService.updateStatus(id, dto.getStatus()));
    }
}
