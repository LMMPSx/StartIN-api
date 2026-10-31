package com.startin.api.controller;

import com.startin.api.dto.ai.AiChatRequestDTO;
import com.startin.api.dto.ai.AiChatResponseDTO;
import com.startin.api.dto.ai.AiDailySyncPayloadDTO;
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
@Tag(name = "Assistente IA & Motor de Matchmaking", description = "Endpoints de IA generativa, assistente virtual e rotina diária das 08:00")
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
    @Operation(summary = "Conversa com o Assistente IA gerador de templates e propostas (com suporte a oportunidade selecionada e anexos)")
    public ResponseEntity<AiChatResponseDTO> chat(@RequestBody AiChatRequestDTO request) {
        StartupProfile profile = null;
        Long sid = request.getStartupId();
        if (sid == null && request.getStartup() != null) {
            sid = request.getStartup().getId();
        }
        if (sid != null) {
            final Long lookupId = sid;
            profile = startupProfileRepository.findById(lookupId)
                    .or(() -> startupProfileRepository.findByUserId(lookupId))
                    .orElse(null);
        }
        if (profile == null) {
            profile = startupProfileRepository.findAll().stream().findFirst().orElse(null);
        }

        String reply = aiIntegrationService.chatWithAssistant(request, profile);
        return ResponseEntity.ok(new AiChatResponseDTO(reply));
    }

    @PostMapping("/enhance-problem")
    @Operation(summary = "Melhora e estrutura o texto de 'Problema que resolve' com IA e extrai tags de matchmaking")
    public ResponseEntity<Map<String, Object>> enhanceProblem(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(aiIntegrationService.enhanceProblemText(payload));
    }

    @PostMapping("/verify-traction-pdf")
    @Operation(summary = "Verifica o PDF de Atestado de Capacidade Técnica junto com a maturidade TRL da NASA")
    public ResponseEntity<Map<String, Object>> verifyTractionPdf(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(aiIntegrationService.verifyTractionPdf(payload));
    }

    @PostMapping("/daily-sync")
    @Operation(summary = "Recebe o JSON diário das 08:00 da manhã gerado pela IA Python (scraping + matches) e persiste no MySQL")
    public ResponseEntity<Map<String, Object>> receiveDailyAiJson(@RequestBody AiDailySyncPayloadDTO payload) {
        Map<String, Object> result = matchService.persistDailyAiJson(payload);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/match")
    @Operation(summary = "Executa o matchmaking via IA Python e persiste os resultados no MySQL")
    public ResponseEntity<List<MatchResultDTO>> triggerMatch(@RequestBody(required = false) MatchTriggerRequestDTO request) {
        Long startupId = (request != null) ? request.getStartupId() : null;
        boolean scrape = request == null || request.getScrapeLiveWeb() == null || Boolean.TRUE.equals(request.getScrapeLiveWeb());
        return ResponseEntity.ok(matchService.recalculateMatches(startupId, scrape));
    }
}
