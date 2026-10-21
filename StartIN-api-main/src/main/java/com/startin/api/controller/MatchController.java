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
    public ResponseEntity<List<MatchResultDTO>> getMatches(@RequestParam(defaultValue = "1") Long startupId) {
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
    @Operation(summary = "Aciona o microserviço de IA em Python para recalcular matches em tempo real")
    public ResponseEntity<List<MatchResultDTO>> recalculate(@RequestBody(required = false) MatchTriggerRequestDTO request) {
        Long startupId = (request != null && request.getStartupId() != null) ? request.getStartupId() : 1L;
        boolean scrape = request != null && Boolean.TRUE.equals(request.getScrapeLiveWeb());
        return ResponseEntity.ok(matchService.recalculateMatches(startupId, scrape));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualiza o status da conexão (SALVO, INTERESSE_ENVIADO, CONTATO_INICIADO, EM_NEGOCIACAO)")
    public ResponseEntity<MatchResultDTO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMatchStatusDTO dto) {
        return ResponseEntity.ok(matchService.updateStatus(id, dto.getStatus()));
    }
}
