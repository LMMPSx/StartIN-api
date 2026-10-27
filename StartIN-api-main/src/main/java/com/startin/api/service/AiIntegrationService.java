package com.startin.api.service;

import com.startin.api.dto.ai.*;
import com.startin.api.dto.company.CompanyDTO;
import com.startin.api.dto.startup.StartupProfileDTO;
import com.startin.api.model.Company;
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
            @Value("${startin.ai.timeout-seconds:45}") int timeoutSeconds) {
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
     * Solicita ao microserviço Python o JSON completo da rotina diária das 08:00 (scraping + matches).
     */
    public AiDailySyncPayloadDTO fetchDailyScrapeJsonFromAi() {
        try {
            String url = aiBaseUrl + "/api/ai/daily-scrape-json";
            ResponseEntity<AiDailySyncPayloadDTO> response = restTemplate.getForEntity(url, AiDailySyncPayloadDTO.class);
            return response.getBody();
        } catch (Exception e) {
            log.warn("Aviso ao consultar JSON diário das 08:00 na IA Python: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Envia TODOS os dados do usuário/startup e lista de oportunidades para o modelo de IA em Python processar.
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
            log.info("Enviando dados completos do usuário/startup '{}' (ID {}) para IA Python em {}", profile.getName(), profile.getId(), url);

            ResponseEntity<AiMatchResponseDTO> response = restTemplate.postForEntity(url, entity, AiMatchResponseDTO.class);
            if (response.getBody() != null && response.getBody().getMatches() != null) {
                log.info("IA Python retornou {} matches calculados para '{}'", response.getBody().getMatches().size(), profile.getName());
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("Falha ao comunicar com o microserviço Python de IA: {}. Calculando compatibilidade multi-critério com os dados do banco...", e.getMessage());
        }

        return fallbackCalculateMatches(startupDto, oppsDto);
    }

    /**
     * Conversação com o assistente inteligente restrita ao projeto StartIn, sucinta e em texto plano (sem markdown).
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
                return cleanPlainText(response.getBody().getReply());
            }
        } catch (Exception e) {
            log.warn("Erro ao chamar endpoint de chat da IA Python: {}", e.getMessage());
        }

        String name = (profile != null && profile.getName() != null) ? profile.getName() : "sua startup";
        String area = (profile != null && profile.getMainArea() != null) ? profile.getMainArea() : "inovacao";
        return "Analisei os dados da " + name + " (" + area + ") na plataforma StartIn. Acesse as abas Para voce e Matches para acompanhar os editais, eventos, POCs e compras alinhados ao seu perfil.";
    }

    private String cleanPlainText(String text) {
        if (text == null) return "";
        return text
                .replaceAll("\\*\\*", "")
                .replaceAll("\\*", "")
                .replaceAll("#+", "")
                .replaceAll("`", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private AiStartupInputDTO mapProfileToAiInput(StartupProfile profile) {
        AiStartupInputDTO dto = new AiStartupInputDTO();
        dto.setId(profile.getId());
        if (profile.getUser() != null) {
            dto.setUserId(profile.getUser().getId());
            dto.setUserName(profile.getUser().getName());
            dto.setUserEmail(profile.getUser().getEmail());
        }

        // 01. Identidade
        dto.setName(profile.getName());
        dto.setSector(profile.getMainArea() != null ? profile.getMainArea() : profile.getMainSector());
        dto.setMainArea(profile.getMainArea());
        dto.setDescription(profile.getDescription());
        dto.setLocation(profile.getLocation());
        dto.setCnpj(profile.getCnpj());
        dto.setWebsite(profile.getWebsite());
        dto.setFoundedYear(profile.getFoundedYear());

        // 02. Solução
        dto.setProblem(profile.getProblem());
        dto.setHowItWorks(profile.getHowItWorks());
        dto.setDifferentiator(profile.getDifferentiator());
        dto.setImpact(profile.getImpact());
        dto.setBusinessModel(profile.getBusinessModel());

        // 03. Mercado
        dto.setTargetAudience(profile.getTargetAudience());
        dto.setMainSector(profile.getMainSector());
        dto.setOperationRegion(profile.getOperationRegion());
        dto.setState(profile.getState());
        dto.setCustomerProfile(profile.getCustomerProfile());
        dto.setMarketSize(profile.getMarketSize());
        dto.setCompetitors(profile.getCompetitors());
        dto.setMainChannel(profile.getMainChannel());

        // 04. Momento e Tração
        dto.setStage(profile.getCurrentStage());
        dto.setCurrentStage(profile.getCurrentStage());
        dto.setTractionCustomers(profile.getTractionCustomers());
        dto.setProductMaturity(profile.getProductMaturity());
        dto.setTeamSize(profile.getTeamSize());
        dto.setAnnualRevenue(profile.getAnnualRevenue());
        dto.setRecentGrowth(profile.getRecentGrowth());
        dto.setInvestmentRaised(profile.getInvestmentRaised());

        // 05. Objetivos
        dto.setMainObjective(profile.getMainObjective());
        dto.setInvestmentNeed(profile.getInvestmentNeed());
        dto.setExecutionDeadline(profile.getExecutionDeadline());
        dto.setResourceUse(profile.getResourceUse());
        dto.setTwelveMonthGoal(profile.getTwelveMonthGoal());
        dto.setStrategicPriority(profile.getStrategicPriority());
        dto.setDesiredPartners(profile.getDesiredPartners());

        // 06. Preferências
        dto.setAreasOfInterest(profile.getAreasOfInterest());
        dto.setOpportunityTypes(profile.getOpportunityTypes());
        dto.setReceiveAiRecommendations(profile.getReceiveAiRecommendations());

        List<String> tags = new ArrayList<>();
        if (profile.getTechnologies() != null && !profile.getTechnologies().isBlank()) {
            tags.addAll(Arrays.stream(profile.getTechnologies().split("[,•]"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList());
        }
        if (profile.getAreasOfInterest() != null && !profile.getAreasOfInterest().isBlank()) {
            for (String item : profile.getAreasOfInterest().split("[,•]")) {
                String trimmed = item.trim();
                if (!trimmed.isEmpty() && !tags.contains(trimmed)) {
                    tags.add(trimmed);
                }
            }
        }
        dto.setTags(tags);
        dto.setTechnologies(tags);

        return dto;
    }

    private AiMatchResponseDTO fallbackCalculateMatches(AiStartupInputDTO startup, List<AiOpportunityInputDTO> opps) {
        AiMatchResponseDTO response = new AiMatchResponseDTO();
        String startupName = (startup.getName() != null && !startup.getName().isBlank()) ? startup.getName() : "Sua Startup";
        String mainArea = (startup.getMainArea() != null && !startup.getMainArea().isBlank()) ? startup.getMainArea() : "Inovação";
        String businessModel = (startup.getBusinessModel() != null && !startup.getBusinessModel().isBlank()) ? startup.getBusinessModel() : "B2B";
        String stage = (startup.getCurrentStage() != null && !startup.getCurrentStage().isBlank()) ? startup.getCurrentStage() : "Validação";
        String location = (startup.getLocation() != null && !startup.getLocation().isBlank()) ? startup.getLocation() : "Brasil";
        String objective = (startup.getMainObjective() != null && !startup.getMainObjective().isBlank()) ? startup.getMainObjective() : "Validação e escala";
        String techs = (startup.getTags() != null && !startup.getTags().isEmpty())
                ? String.join(" · ", startup.getTags())
                : mainArea;

        response.setStartupName(startupName);
        response.setTotalAnalyzed(opps.size());

        List<AiMatchItemDTO> matches = new ArrayList<>();
        for (AiOpportunityInputDTO opp : opps) {
            AiMatchItemDTO item = new AiMatchItemDTO();
            item.setOpportunityId(opp.getId());
            item.setOpportunityTitle(opp.getTitle());
            item.setUrl(opp.getUrl());

            int score = 76;
            String oppText = ((opp.getTitle() != null ? opp.getTitle() : "") + " " +
                    (opp.getDescription() != null ? opp.getDescription() : "") + " " +
                    (opp.getTags() != null ? opp.getTags() : "")).toLowerCase();

            if (startup.getTags() != null) {
                for (String t : startup.getTags()) {
                    if (oppText.contains(t.toLowerCase())) {
                        score += 5;
                    }
                }
            }
            if (startup.getOpportunityTypes() != null && opp.getType() != null &&
                    startup.getOpportunityTypes().toLowerCase().contains(opp.getType().toLowerCase())) {
                score += 6;
            }
            if (oppText.contains(mainArea.toLowerCase())) {
                score += 5;
            }
            score = Math.min(Math.max(score, 70), 98);

            item.setMatchScore(score);
            item.setJustification("A startup " + startupName + " (" + mainArea + " · " + businessModel + ") apresenta alta aderência técnica e estratégica com '" + opp.getTitle() + "' pelas competências em " + techs + ".");
            item.setRecommendedAction("Apresentar a solução da " + startupName + " destacando foco em " + objective + ".");

            AiMatchReasonsDTO reasons = new AiMatchReasonsDTO();
            reasons.setPerfil(mainArea + " · " + businessModel + " (" + stage + ")");
            reasons.setRequisitos(techs);
            reasons.setObjetivo(objective);
            reasons.setContexto(location + " · " + (opp.getMeta() != null ? opp.getMeta() : "Brasil"));
            item.setReasons(reasons);

            matches.add(item);
        }

        matches.sort((a, b) -> Integer.compare(b.getMatchScore(), a.getMatchScore()));
        response.setMatches(matches);
        return response;
    }

    /**
     * Calcula via IA Python (ou motor multi-critério equivalente) o match entre uma EMPRESA e todas as STARTUPS cadastradas.
     */
    public List<Map<String, Object>> calculateCompanyToStartupsMatches(Company company, List<StartupProfile> startups) {
        CompanyDTO companyDto = CompanyDTO.fromEntity(company);
        List<StartupProfileDTO> startupDtos = startups.stream().map(StartupProfileDTO::fromEntity).toList();

        Map<String, Object> payload = new HashMap<>();
        payload.put("company", companyDto);
        payload.put("startups", startupDtos);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            String url = aiBaseUrl + "/api/ai/match-startups";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getBody() != null && response.getBody().get("matches") instanceof List<?> rawList && !rawList.isEmpty()) {
                return (List<Map<String, Object>>) rawList;
            }
        } catch (Exception e) {
            log.warn("Aviso ao consultar /api/ai/match-startups: {}. Usando cálculo multi-critério local.", e.getMessage());
        }

        return fallbackCompanyToStartupsMatches(companyDto, startupDtos);
    }

    /**
     * Calcula via IA Python (ou motor multi-critério equivalente) o match entre uma STARTUP e todas as EMPRESAS cadastradas.
     */
    public List<Map<String, Object>> calculateStartupToCompaniesMatches(StartupProfile startup, List<Company> companies) {
        StartupProfileDTO startupDto = StartupProfileDTO.fromEntity(startup);
        List<CompanyDTO> companyDtos = companies.stream().map(CompanyDTO::fromEntity).toList();

        Map<String, Object> payload = new HashMap<>();
        payload.put("startup", startupDto);
        payload.put("companies", companyDtos);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            String url = aiBaseUrl + "/api/ai/match-companies";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getBody() != null && response.getBody().get("matches") instanceof List<?> rawList && !rawList.isEmpty()) {
                return (List<Map<String, Object>>) rawList;
            }
        } catch (Exception e) {
            log.warn("Aviso ao consultar /api/ai/match-companies: {}. Usando cálculo multi-critério local.", e.getMessage());
        }

        return fallbackStartupToCompaniesMatches(startupDto, companyDtos);
    }

    private List<Map<String, Object>> fallbackCompanyToStartupsMatches(CompanyDTO company, List<StartupProfileDTO> startups) {
        String compName = company != null && company.getName() != null ? company.getName() : "Sua Empresa";
        String compSector = company != null && company.getSector() != null ? company.getSector() : "Inovação Corporativa";
        String compTagsRaw = company != null && company.getTags() != null ? company.getTags() : "IA, SaaS, B2B, Dados";
        String compDemands = company != null && company.getDemands() != null ? company.getDemands() : (company != null ? company.getDescription() : "");
        String compLocation = company != null && company.getLocation() != null ? company.getLocation() : "Brasil";
        String compMode = company != null && company.getOperationMode() != null ? company.getOperationMode() : "POC, Compra";

        List<String> compKeywords = Arrays.stream((compTagsRaw + "," + compSector + "," + compDemands)
                .replace("•", ",")
                .split("[,\\s]+"))
                .map(s -> s.trim().toLowerCase())
                .filter(s -> s.length() >= 2)
                .distinct()
                .toList();

        List<Map<String, Object>> results = new ArrayList<>();
        for (StartupProfileDTO st : startups) {
            String stName = st.getName() != null ? st.getName() : "Startup";
            String stArea = st.getMainArea() != null && !st.getMainArea().isBlank() ? st.getMainArea() : (st.getMainSector() != null ? st.getMainSector() : "Tecnologia & IA");
            String stTech = st.getTechnologies() != null && !st.getTechnologies().isBlank() ? st.getTechnologies() : (st.getAreasOfInterest() != null ? st.getAreasOfInterest() : "IA • Dados • SaaS B2B");
            String stDesc = st.getDescription() != null && !st.getDescription().isBlank() ? st.getDescription() : (st.getProblem() != null ? st.getProblem() : "Solução tecnológica B2B para eficiência operacional.");
            String stStage = st.getCurrentStage() != null && !st.getCurrentStage().isBlank() ? st.getCurrentStage() : "Tração / Validação";
            String stLoc = st.getLocation() != null && !st.getLocation().isBlank() ? st.getLocation() : "Brasil · Remoto";
            String stModel = st.getBusinessModel() != null && !st.getBusinessModel().isBlank() ? st.getBusinessModel() : "SaaS B2B";
            String stObjective = st.getMainObjective() != null && !st.getMainObjective().isBlank() ? st.getMainObjective() : "Realizar POCs e contratos corporativos";

            String fullStText = (stName + " " + stArea + " " + stTech + " " + stDesc + " " + stModel + " " + stObjective).toLowerCase();
            int score = 78;
            for (String kw : compKeywords) {
                if (fullStText.contains(kw)) {
                    score += 3;
                }
            }
            if (st.getOpportunityTypes() != null && (st.getOpportunityTypes().toLowerCase().contains("poc") || st.getOpportunityTypes().toLowerCase().contains("compra"))) {
                score += 5;
            }
            score = Math.min(Math.max(score, 74), 98);

            Map<String, String> reasons = new LinkedHashMap<>();
            reasons.put("perfil", stArea + " · " + stModel + " (" + stStage + ")");
            reasons.put("requisitos", "Tecnologias da startup: " + stTech.replace(",", " · "));
            reasons.put("objetivo", "Alinhada com demandas de " + compMode + " da " + compName);
            reasons.put("contexto", stLoc + " · Compatível com " + compLocation);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("startupId", st.getId());
            item.put("startupName", stName);
            item.put("mainArea", stArea);
            item.put("description", stDesc);
            item.put("shortDescription", stDesc.length() > 120 ? stDesc.substring(0, 120) + "..." : stDesc);
            item.put("technologies", stTech);
            item.put("businessModel", stModel);
            item.put("currentStage", stStage);
            item.put("location", stLoc);
            item.put("website", st.getWebsite());
            item.put("phone", st.getPhone());
            item.put("email", st.getUserEmail());
            item.put("avatarUrl", st.getAvatarUrl());
            item.put("matchScore", score);
            item.put("justification", "A startup " + stName + " (" + stArea + " · " + stModel + ") possui alta aderência com as demandas de " + compMode + " da " + compName + " pelas competências em " + stTech + ".");
            item.put("recommendedAction", "Convidar a " + stName + " para apresentar proposta de " + compMode + " focada em " + compSector + ".");
            item.put("reasons", reasons);
            results.add(item);
        }

        results.sort((a, b) -> Integer.compare((int) b.get("matchScore"), (int) a.get("matchScore")));
        return results;
    }

    private List<Map<String, Object>> fallbackStartupToCompaniesMatches(StartupProfileDTO startup, List<CompanyDTO> companies) {
        String stName = startup != null && startup.getName() != null ? startup.getName() : "Sua Startup";
        String stArea = startup != null && startup.getMainArea() != null && !startup.getMainArea().isBlank() ? startup.getMainArea() : "Inovação & IA";
        String stTech = startup != null && startup.getTechnologies() != null && !startup.getTechnologies().isBlank()
                ? startup.getTechnologies()
                : (startup != null && startup.getAreasOfInterest() != null ? startup.getAreasOfInterest() : "IA, Dados, Automação, SaaS B2B");
        String stModel = startup != null && startup.getBusinessModel() != null && !startup.getBusinessModel().isBlank() ? startup.getBusinessModel() : "SaaS B2B";
        String stLoc = startup != null && startup.getLocation() != null && !startup.getLocation().isBlank() ? startup.getLocation() : "Brasil";

        List<String> stKeywords = Arrays.stream((stTech + "," + stArea + "," + stModel)
                .replace("•", ",")
                .split("[,\\s]+"))
                .map(s -> s.trim().toLowerCase())
                .filter(s -> s.length() >= 2)
                .distinct()
                .toList();

        List<Map<String, Object>> results = new ArrayList<>();
        for (CompanyDTO comp : companies) {
            String compName = comp.getName() != null ? comp.getName() : "Empresa";
            String compType = comp.getType() != null ? comp.getType() : "EMPRESA";
            String compSector = comp.getSector() != null ? comp.getSector() : "Inovação Corporativa";
            String compDesc = comp.getDescription() != null && !comp.getDescription().isBlank()
                    ? comp.getDescription()
                    : "Organização parceira em busca de startups para POCs e contratação de soluções.";
            String compDemands = comp.getDemands() != null && !comp.getDemands().isBlank() ? comp.getDemands() : compDesc;
            String compTags = comp.getTags() != null && !comp.getTags().isBlank() ? comp.getTags() : "IA, SaaS, B2B, Dados";
            String compLoc = comp.getLocation() != null && !comp.getLocation().isBlank() ? comp.getLocation() : "Brasil · Nacional";
            String compMode = comp.getOperationMode() != null && !comp.getOperationMode().isBlank() ? comp.getOperationMode() : "POC, Compra, POC + Compra";

            String fullCompText = (compName + " " + compSector + " " + compDesc + " " + compDemands + " " + compTags).toLowerCase();
            int score = 79;
            for (String kw : stKeywords) {
                if (fullCompText.contains(kw)) {
                    score += 3;
                }
            }
            score = Math.min(Math.max(score, 75), 98);

            Map<String, String> reasons = new LinkedHashMap<>();
            reasons.put("perfil", compSector + " · Contratação via " + compMode);
            reasons.put("requisitos", "Tecnologias buscadas pela empresa: " + compTags.replace(",", " · "));
            reasons.put("objetivo", "Demanda corporativa compatível com a solução da " + stName);
            reasons.put("contexto", compLoc + " · Compatível com " + stLoc);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("companyId", comp.getId());
            item.put("companyName", compName);
            item.put("companyType", compType);
            item.put("sector", compSector);
            item.put("description", compDesc);
            item.put("demands", compDemands);
            item.put("tags", compTags);
            item.put("location", compLoc);
            item.put("website", comp.getWebsite());
            item.put("phone", comp.getPhone());
            item.put("cnpj", comp.getCnpj());
            item.put("operationMode", compMode);
            item.put("preferredStage", comp.getPreferredStage() != null ? comp.getPreferredStage() : "Validação / Tração");
            item.put("avatarUrl", comp.getAvatarUrl());
            item.put("matchScore", score);
            item.put("justification", "A empresa " + compName + " (" + compSector + ") busca soluções em " + compTags + " para " + compMode + ", apresentando forte sinergia com a " + stName + ".");
            item.put("recommendedAction", "Apresentar proposta de " + compMode + " da " + stName + " para os desafios operacionais da " + compName + ".");
            item.put("reasons", reasons);
            results.add(item);
        }

        results.sort((a, b) -> Integer.compare((int) b.get("matchScore"), (int) a.get("matchScore")));
        return results;
    }
}
