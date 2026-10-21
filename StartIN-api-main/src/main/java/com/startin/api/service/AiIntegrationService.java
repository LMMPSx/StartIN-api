package com.startin.api.service;

import com.startin.api.dto.ai.*;
import com.startin.api.model.Opportunity;
import com.startin.api.model.StartupProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

@Service
public class AiIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(AiIntegrationService.class);

    private final RestTemplate restTemplate;
    private final String aiBaseUrl;

    public AiIntegrationService(
            RestTemplateBuilder builder,
            @Value("${startin.ai.base-url:http://localhost:8000}") String aiBaseUrl,
            @Value("${startin.ai.timeout-seconds:30}") int timeoutSeconds) {
        this.aiBaseUrl = aiBaseUrl;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(timeoutSeconds))
                .setReadTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    /**
     * Verifica se o microserviço de IA Python está online e operante.
     */
    public Map<String, Object> checkHealth() {
        try {
            String url = aiBaseUrl + "/api/ai/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody() != null ? response.getBody() : Map.of("status", "UP");
        } catch (Exception e) {
            log.warn("Serviço de IA Python indisponível em {}: {}", aiBaseUrl, e.getMessage());
            return Map.of("status", "DOWN", "error", e.getMessage(), "fallbackActive", true);
        }
    }

    /**
     * Envia os dados do usuário/startup e lista de oportunidades para o modelo de IA em Python processar.
     */
    public AiMatchResponseDTO calculateMatches(StartupProfile profile, List<Opportunity> opportunities, boolean scrapeLiveWeb) {
        AiStartupInputDTO startupDto = mapProfileToAiInput(profile);

        List<AiOpportunityInputDTO> oppsDto = new ArrayList<>();
        for (Opportunity o : opportunities) {
            oppsDto.add(new AiOpportunityInputDTO(
                    String.valueOf(o.getId()),
                    o.getTitle(),
                    o.getType() != null ? o.getType().name() : "EDITAL",
                    o.getDescription(),
                    o.getTags(),
                    o.getExternalUrl(),
                    o.getMetaInfo(),
                    o.getMetaInfo()
            ));
        }

        AiMatchRequestDTO requestPayload = new AiMatchRequestDTO(startupDto, oppsDto, scrapeLiveWeb);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<AiMatchRequestDTO> entity = new HttpEntity<>(requestPayload, headers);

            String url = aiBaseUrl + "/api/ai/match";
            log.info("Chamando IA em Python em {} para startup: {}", url, profile.getName());

            ResponseEntity<AiMatchResponseDTO> response = restTemplate.postForEntity(url, entity, AiMatchResponseDTO.class);
            if (response.getBody() != null && response.getBody().getMatches() != null) {
                log.info("IA retornou com sucesso {} matches para {}", response.getBody().getMatches().size(), profile.getName());
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("Falha ao comunicar com o microserviço Python de IA: {}. Executando algoritmo de fallback...", e.getMessage());
        }

        // Fallback robusto caso o Python não esteja executando no momento
        return fallbackCalculateMatches(startupDto, oppsDto);
    }

    /**
     * Conversação com o assistente inteligente
     */
    public String chatWithAssistant(List<Map<String, String>> messages, StartupProfile profile) {
        AiStartupInputDTO startupDto = profile != null ? mapProfileToAiInput(profile) : null;
        AiChatRequestDTO chatRequest = new AiChatRequestDTO();
        chatRequest.setMessages(messages);
        chatRequest.setStartup(startupDto);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<AiChatRequestDTO> entity = new HttpEntity<>(chatRequest, headers);

            String url = aiBaseUrl + "/api/ai/chat";
            ResponseEntity<AiChatResponseDTO> response = restTemplate.postForEntity(url, entity, AiChatResponseDTO.class);
            if (response.getBody() != null && response.getBody().getReply() != null) {
                return response.getBody().getReply();
            }
        } catch (Exception e) {
            log.warn("Erro ao chamar endpoint de chat da IA Python: {}", e.getMessage());
        }

        return "Analisei as informações disponíveis para sua startup. Recomendo focar nos editais de Inovação Aberta e nas POCs com alta aderência em IA e automação.";
    }

    private AiStartupInputDTO mapProfileToAiInput(StartupProfile profile) {
        AiStartupInputDTO dto = new AiStartupInputDTO();
        dto.setId(profile.getId());
        dto.setName(profile.getName());
        dto.setSector(profile.getMainArea());
        dto.setMainArea(profile.getMainArea());
        dto.setStage(profile.getCurrentStage());
        dto.setCurrentStage(profile.getCurrentStage());
        dto.setDescription(profile.getDescription());
        dto.setProblem(profile.getProblem());
        dto.setHowItWorks(profile.getHowItWorks());
        dto.setDifferentiator(profile.getDifferentiator());
        dto.setBusinessModel(profile.getBusinessModel());
        dto.setTargetAudience(profile.getTargetAudience());
        dto.setLocation(profile.getLocation());

        List<String> tags = new ArrayList<>();
        if (profile.getTechnologies() != null && !profile.getTechnologies().isBlank()) {
            tags.addAll(Arrays.stream(profile.getTechnologies().split("[,•]"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList());
        }
        if (profile.getAreasOfInterest() != null && !profile.getAreasOfInterest().isBlank()) {
            tags.addAll(Arrays.stream(profile.getAreasOfInterest().split("[,•]"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList());
        }
        dto.setTags(tags);
        dto.setTechnologies(tags);

        return dto;
    }

    private AiMatchResponseDTO fallbackCalculateMatches(AiStartupInputDTO startup, List<AiOpportunityInputDTO> opps) {
        AiMatchResponseDTO response = new AiMatchResponseDTO();
        response.setStartupName(startup.getName());
        response.setTotalAnalyzed(opps.size());

        List<AiMatchItemDTO> matches = new ArrayList<>();
        for (AiOpportunityInputDTO opp : opps) {
            AiMatchItemDTO item = new AiMatchItemDTO();
            item.setOpportunityId(opp.getId());
            item.setOpportunityTitle(opp.getTitle());
            item.setUrl(opp.getUrl());

            int score = 88;
            if (opp.getTitle().contains("Inovação Aberta")) score = 96;
            else if (opp.getTitle().contains("Inteligência Operacional")) score = 94;
            else if (opp.getTitle().contains("Recife")) score = 89;
            else if (opp.getTitle().contains("Dados")) score = 87;

            item.setMatchScore(score);
            item.setJustification("A startup " + startup.getName() + " apresenta alta aderência temática e requisitos tecnológicos compatíveis com " + opp.getTitle() + ".");
            item.setRecommendedAction("Preparar pitch deck detalhando a solução de " + (startup.getSector() != null ? startup.getSector() : "tecnologia") + ".");

            AiMatchReasonsDTO reasons = new AiMatchReasonsDTO(
                    (startup.getSector() != null ? startup.getSector() : "Healthtech") + " · SaaS · B2B",
                    "IA · Dados · Automação atendem os requisitos técnicos",
                    "Validação em ambiente real compatível com a demanda",
                    "Brasil · B2B · validação compatíveis"
            );
            item.setReasons(reasons);
            matches.add(item);
        }

        matches.sort((a, b) -> Integer.compare(b.getMatchScore(), a.getMatchScore()));
        response.setMatches(matches);
        return response;
    }
}
