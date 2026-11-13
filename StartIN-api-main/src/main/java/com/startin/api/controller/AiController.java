package com.startin.api.controller;

import com.startin.api.dto.ai.AiChatRequestDTO;
import com.startin.api.dto.ai.AiChatResponseDTO;
import com.startin.api.dto.ai.AiDailySyncPayloadDTO;
import com.startin.api.dto.match.MatchResultDTO;
import com.startin.api.dto.match.MatchTriggerRequestDTO;
import com.startin.api.model.ServiceProvider;
import com.startin.api.model.StartupProfile;
import com.startin.api.repository.ServiceProviderRepository;
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
    private final ServiceProviderRepository serviceProviderRepository;

    public AiController(
            AiIntegrationService aiIntegrationService,
            MatchService matchService,
            StartupProfileRepository startupProfileRepository,
            ServiceProviderRepository serviceProviderRepository) {
        this.aiIntegrationService = aiIntegrationService;
        this.matchService = matchService;
        this.startupProfileRepository = startupProfileRepository;
        this.serviceProviderRepository = serviceProviderRepository;
    }

    @GetMapping("/status")
    @Operation(summary = "Verifica o status de conectividade com o microserviço Python de IA")
    public ResponseEntity<Map<String, Object>> getAiStatus() {
        return ResponseEntity.ok(aiIntegrationService.checkHealth());
    }

    @PostMapping("/chat")
    @Operation(summary = "Conversa com o Assistente IA gerador de templates e propostas (com suporte a Startup, Prestador, oportunidade selecionada e anexos)")
    public ResponseEntity<AiChatResponseDTO> chat(@RequestBody AiChatRequestDTO request) {
        boolean isProviderRole = "PROVIDER".equalsIgnoreCase(request.getAccountType())
                || request.getProviderId() != null
                || (request.getProvider() != null && !request.getProvider().isEmpty());

        ServiceProvider provider = null;
        if (isProviderRole) {
            if (request.getProviderId() != null) {
                provider = serviceProviderRepository.findById(request.getProviderId()).orElse(null);
            }
            if (provider == null && request.getUserId() != null) {
                provider = serviceProviderRepository.findByUserId(request.getUserId()).orElse(null);
            }
            if (provider == null) {
                provider = serviceProviderRepository.findAll().stream().findFirst().orElse(null);
            }
        }

        StartupProfile profile = null;
        if (!isProviderRole) {
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
            if (profile == null && request.getUserId() != null) {
                profile = startupProfileRepository.findByUserId(request.getUserId()).orElse(null);
            }
            if (profile == null) {
                profile = startupProfileRepository.findAll().stream().findFirst().orElse(null);
            }
        }

        String reply = aiIntegrationService.chatWithAssistant(request, profile, provider);
        return ResponseEntity.ok(new AiChatResponseDTO(reply));
    }

    @PostMapping("/enhance-problem")
    @Operation(summary = "Melhora e estrutura o texto de 'Problema que resolve' com IA e extrai tags de matchmaking")
    public ResponseEntity<Map<String, Object>> enhanceProblem(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(aiIntegrationService.enhanceProblemText(payload));
    }

    @PostMapping("/enhance-company-demands")
    @Operation(summary = "Melhora e estrutura o texto de desafios e demandas operacionais da empresa com IA e extrai tópicos")
    public ResponseEntity<Map<String, Object>> enhanceCompanyDemands(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(aiIntegrationService.enhanceCompanyDemandsText(payload));
    }

    @PostMapping("/enhance-provider-services")
    @Operation(summary = "Melhora e estrutura a descrição dos serviços do prestador com IA e extrai competências")
    public ResponseEntity<Map<String, Object>> enhanceProviderServices(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(aiIntegrationService.enhanceProviderServicesText(payload));
    }

    @PostMapping("/verify-traction-pdf")
    @Operation(summary = "Verifica o PDF de Atestado de Capacidade Técnica junto com a maturidade TRL da NASA")
    public ResponseEntity<Map<String, Object>> verifyTractionPdf(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(aiIntegrationService.verifyTractionPdf(payload));
    }

    @PostMapping("/extract-provider-resume")
    @Operation(summary = "Lê o currículo em PDF do prestador de serviços e extrai categoria, serviço principal, tecnologias e descrição prévia em 1ª pessoa")
    public ResponseEntity<Map<String, Object>> extractProviderResume(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(aiIntegrationService.extractProviderResume(payload));
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
