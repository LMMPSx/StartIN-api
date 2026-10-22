package com.startin.api.controller;

import com.startin.api.dto.ai.AiChatRequestDTO;
import com.startin.api.dto.ai.AiChatResponseDTO;
import com.startin.api.dto.match.MatchResultDTO;
import com.startin.api.dto.match.MatchTriggerRequestDTO;
import com.startin.api.model.StartupProfile;
import com.startin.api.repository.StartupProfileRepository;
import com.startin.api.service.AiIntegrationService;
import com.startin.api.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "Assistente IA & Motor de Matchmaking", description = "Endpoints de IA generativa, assistente virtual e scraping")
public class AiController {

    private final AiIntegrationService aiIntegrationService;
    private final MatchService matchService;
    private final StartupProfileRepository startupProfileRepository;

    public AiController(
            AiIntegrationService aiIntegrationService,
            MatchService matchService,
            StartupProfileRepository startupProfileRepository) {
        this.aiIntegrationService = aiIntegrationService;
        this.matchService = matchService;
        this.startupProfileRepository = startupProfileRepository;
    }

    @GetMapping("/status")
    @Operation(summary = "Verifica o status de conectividade com o microserviço Python de IA")
    public ResponseEntity<Map<String, Object>> getAiStatus() {
        return ResponseEntity.ok(aiIntegrationService.checkHealth());
    }

    @PostMapping("/chat")
    @Operation(summary = "Conversa com o Assistente IA do StartIn contextualizado ao perfil da startup")
    public ResponseEntity<AiChatResponseDTO> chat(@RequestBody AiChatRequestDTO request) {
        StartupProfile profile = null;
        Long sid = request.getStartupId();
        if (sid == null && request.getStartup() != null) {
            sid = request.getStartup().getId();
        }
        if (sid != null) {
            profile = startupProfileRepository.findById(sid).orElse(null);
        }
        if (profile == null) {
            profile = startupProfileRepository.findAll().stream().findFirst().orElse(null);
        }

        String reply = aiIntegrationService.chatWithAssistant(request.getMessages(), profile);
        return ResponseEntity.ok(new AiChatResponseDTO(reply));
    }

    @PostMapping("/match")
    @Operation(summary = "Executa o matchmaking completo via IA Python (incluindo scraping web) e persiste os resultados")
    public ResponseEntity<List<MatchResultDTO>> triggerMatch(@RequestBody(required = false) MatchTriggerRequestDTO request) {
        Long startupId = (request != null) ? request.getStartupId() : null;
        boolean scrape = request == null || request.getScrapeLiveWeb() == null || Boolean.TRUE.equals(request.getScrapeLiveWeb());
        return ResponseEntity.ok(matchService.recalculateMatches(startupId, scrape));
    }
}
