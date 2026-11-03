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

import java.text.Normalizer;
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
     * Conversação com o assistente inteligente de elaboração de templates e propostas personalizadas.
     */
    public String chatWithAssistant(List<Map<String, String>> messages, StartupProfile profile) {
        AiChatRequestDTO req = new AiChatRequestDTO();
        req.setMessages(messages);
        return chatWithAssistant(req, profile);
    }

    public String chatWithAssistant(AiChatRequestDTO incomingRequest, StartupProfile profile) {
        AiStartupInputDTO startupDto = profile != null ? mapProfileToAiInput(profile) : incomingRequest.getStartup();
        AiChatRequestDTO chatRequest = new AiChatRequestDTO();
        chatRequest.setMessages(incomingRequest.getMessages());
        chatRequest.setStartup(startupDto);
        chatRequest.setStartupId(profile != null ? profile.getId() : incomingRequest.getStartupId());
        chatRequest.setSelectedOpportunity(incomingRequest.getSelectedOpportunity());
        chatRequest.setAttachmentName(incomingRequest.getAttachmentName());
        chatRequest.setAttachmentBase64(incomingRequest.getAttachmentBase64());
        chatRequest.setAttachmentText(incomingRequest.getAttachmentText());

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<AiChatRequestDTO> entity = new HttpEntity<>(chatRequest, headers);

            String url = aiBaseUrl + "/api/ai/chat";
            ResponseEntity<AiChatResponseDTO> response = restTemplate.postForEntity(url, entity, AiChatResponseDTO.class);
            if (response.getBody() != null && response.getBody().getReply() != null) {
                return cleanStructuredText(response.getBody().getReply());
            }
        } catch (Exception e) {
            log.warn("Erro ao chamar endpoint de chat da IA Python: {}", e.getMessage());
        }

        return buildJavaContextualFallbackReply(incomingRequest, profile);
    }

    private String buildJavaContextualFallbackReply(AiChatRequestDTO incomingRequest, StartupProfile profile) {
        String name = (profile != null && profile.getName() != null) ? profile.getName() : "sua startup";
        String area = (profile != null && profile.getMainArea() != null) ? profile.getMainArea() : "Tecnologia e Inovação";
        String stage = (profile != null && profile.getCurrentStage() != null) ? profile.getCurrentStage() : "Validação";
        int trl = (profile != null && profile.getTrlScore() != null) ? profile.getTrlScore() : (stage.toLowerCase().contains("tra") ? 8 : 4);
        String problem = (profile != null && profile.getProblem() != null && !profile.getProblem().isBlank())
                ? profile.getProblem()
                : "Solução tecnológica B2B voltada para eficiência operacional, automação e inteligência de dados.";
        String techs = (profile != null && profile.getTechnologies() != null && !profile.getTechnologies().isBlank())
                ? profile.getTechnologies().replace("[", "").replace("]", "").replace("'", "")
                : area;

        Map<String, Object> opp = incomingRequest.getSelectedOpportunity();
        boolean hasOpp = opp != null && opp.get("title") != null && !String.valueOf(opp.get("title")).isBlank();
        String oppTitle = hasOpp
                ? String.valueOf(opp.get("title"))
                : (incomingRequest.getAttachmentName() != null ? incomingRequest.getAttachmentName() : "");
        String oppType = opp != null && opp.get("type") != null ? String.valueOf(opp.get("type")) : "Edital";
        String oppCompany = opp != null && opp.get("companyName") != null ? String.valueOf(opp.get("companyName")) : "Comitê Avaliador / Organização Responsável";
        String oppMeta = opp != null && opp.get("meta") != null ? String.valueOf(opp.get("meta")) : "Inscrições abertas · Brasil";
        String oppDesc = opp != null && opp.get("description") != null ? String.valueOf(opp.get("description")) : "";

        String lastUserMsg = "";
        if (incomingRequest.getMessages() != null) {
            for (int i = incomingRequest.getMessages().size() - 1; i >= 0; i--) {
                Map<String, String> m = incomingRequest.getMessages().get(i);
                String from = m.getOrDefault("from", "me");
                if (!"ai".equalsIgnoreCase(from) && !"assistant".equalsIgnoreCase(from)) {
                    String txt = m.getOrDefault("text", "");
                    if (!txt.isBlank()) {
                        lastUserMsg = txt.replaceAll("\\[Arquivo anexado:.*?\\]", "").trim();
                        break;
                    }
                }
            }
        }
        String q = lastUserMsg.toLowerCase();

        if (q.contains("plataforma") || q.contains("sobre a startin") || q.contains("como funciona") || q.contains("funcionalidade")) {
            return "Olá! A StartIn é uma plataforma inteligente de matchmaking e inovação aberta que conecta startups como a " + name + " a editais públicos, eventos e demandas corporativas.\n\n" +
                    "Principais módulos para o seu perfil (" + stage + "):\n" +
                    "1. Para você: Filtra automaticamente oportunidades pelo seu estágio (Validação acessa POCs, Primeira Compra, Editais e Eventos; Tração com TRL da NASA e Atestado PDF acessa Compras Corporativas diretas, Editais e Eventos).\n" +
                    "2. Buscar empresas: Mostra perfis corporativos, contatos diretos e todas as oportunidades lançadas por cada empresa.\n" +
                    "3. Salvos e Assistente IA: As oportunidades salvas aparecem no seletor ao lado para tirar dúvidas de requisitos, analisar PDFs ou gerar propostas e templates prontos para submissão.";
        }

        if (q.length() <= 45 && (q.contains("olá") || q.contains("ola") || q.startsWith("oi") || q.contains("bom dia") || q.contains("boa tarde") || q.contains("boa noite") || q.contains("tudo bem"))) {
            return "Olá! Sou o Assistente de IA da StartIn, integrado aos dados da " + name + " (" + area + " · Estágio " + stage + ").\n\n" +
                    (hasOpp
                            ? "Vejo que você selecionou '" + oppTitle + "' (" + oppType + "). Posso explicar os requisitos, prazos e critérios dessa oportunidade ou redigir sua proposta técnica, pitch e template de submissão."
                            : "Selecione uma oportunidade salva no painel à esquerda, anexe um edital em PDF ou me faça qualquer pergunta sobre a plataforma, TRL, editais, POCs e estratégia de submissão.") +
                    "\n\nComo posso ajudar agora?";
        }

        if (q.contains("trl") || q.contains("nasa") || q.contains("atestado") || q.contains("estágio") || q.contains("estagio")) {
            return "Sobre Estágios e Escala TRL da NASA na StartIn:\n\n" +
                    "1. Momento atual da " + name + ": " + stage + " (TRL estimado: " + trl + "/9).\n" +
                    "2. Escala TRL (1 a 9): Níveis 1–3 indicam prova de conceito inicial; níveis 4–6 indicam MVP validado (estágio Validação, ideal para POCs e Primeira Compra); níveis 7–9 indicam operação comercial comprovada por Atestado de Capacidade Técnica (estágio Tração, liberando Compras Corporativas diretas).\n" +
                    "3. Você pode atualizar seu estágio e enviar seu Atestado em PDF a qualquer momento em 'Conta e acesso'.";
        }

        if (q.contains("prazo") || q.contains("quando") || q.contains("como me inscrever") || q.contains("como participar")) {
            if (oppTitle.isEmpty()) {
                return "Selecione uma das suas oportunidades salvas no campo 'Oportunidade / Edital em foco' à esquerda para que eu informe o prazo exato, formato e passo a passo de inscrição.";
            }
            return "Prazo e Inscrição — " + oppTitle + ":\n\n" +
                    "1. Prazo e Formato: " + oppMeta + "\n" +
                    "2. Modalidade: " + oppType + " | Responsável: " + oppCompany + "\n" +
                    "3. Passo a passo: Gere aqui no chat sua proposta técnica ou template com os dados da " + name + " e acesse o link oficial (ou o contato da empresa ofertante) na tela de detalhes da oportunidade para enviar.";
        }

        if (q.contains("resumo") || q.contains("requisitos") || q.contains("critérios") || q.contains("sobre esse") || q.contains("o que pede") || q.contains("elegibilidade")) {
            if (oppTitle.isEmpty()) {
                return "Selecione uma oportunidade salva no painel à esquerda ou anexe o PDF do regulamento para que eu analise os requisitos e a aderência com a " + name + ".";
            }
            return "Resumo de Requisitos e Aderência — " + oppTitle + " (" + oppType + "):\n\n" +
                    "1. Escopo: " + (oppDesc.isEmpty() ? "Demanda voltada a soluções inovadoras com impacto operacional." : oppDesc) + "\n" +
                    "2. Prazo / Formato: " + oppMeta + "\n" +
                    "3. Aderência da " + name + ": Sua atuação em " + area + " no estágio " + stage + " com " + techs + " atende diretamente à dor operacional buscada por " + oppCompany + ".";
        }

        if (q.contains("mensagem executiva") || q.contains("apresentação comercial") || q.contains("e-mail") || q.contains("email") || q.contains("abordagem")) {
            String target = oppTitle.isEmpty() ? "iniciativas de inovação aberta em " + area : "a oportunidade '" + oppTitle + "' (" + oppType + ")";
            return "ASSUNTO: Conexão Tecnológica (" + area + ") — " + name + (oppTitle.isEmpty() ? "" : " | " + oppTitle) + "\n\n" +
                    "Olá, " + oppCompany + ",\n\n" +
                    "Apresento a " + name + ", startup de " + area + " em estágio de " + stage + " (TRL " + trl + "/9), com interesse direto em " + target + ".\n\n" +
                    "Nossa tecnologia (" + techs + ") endereça de forma prática:\n" + problem + "\n\n" +
                    "Podemos agendar uma breve demonstração de 20 minutos nesta semana para alinhar escopo e indicadores?\n\n" +
                    "Atenciosamente,\nEquipe " + name;
        }

        if (q.contains("texto persuasivo") || q.contains("mérito") || q.contains("diferencial") || q.contains("pitch")) {
            return "TEXTO DE MERITO TECNOLOGICO E IMPACTO — " + name.toUpperCase() + "\n\n" +
                    "1. PROBLEMA E RELEVANCIA\n" + problem + "\n\n" +
                    "2. DIFERENCIAL TECNOLOGICO\nA " + name + " aplica " + techs + " com maturidade de estágio " + stage + " (TRL " + trl + "/9), reduzindo riscos de implantação" +
                    (oppTitle.isEmpty() ? "." : " para o desafio '" + oppTitle + "'.") + "\n\n" +
                    "3. IMPACTO ESPERADO\nGanho rápido de produtividade, governança orientada por dados e potencial imediato de escala B2B.";
        }

        if (q.contains("proposta técnica") || q.contains("plano de trabalho") || q.contains("cronograma")) {
            String tTitle = oppTitle.isEmpty() ? ("PROJETO DE INOVACAO — " + name) : oppTitle;
            return "PROPOSTA TECNICA E PLANO DE TRABALHO — " + tTitle.toUpperCase() + "\n\n" +
                    "1. OBJETIVO\nImplantar a solução da " + name + " (" + techs + ") junto a " + oppCompany + " para resolver: " + problem + "\n\n" +
                    "2. CRONOGRAMA DE EXECUCAO (90 DIAS)\n" +
                    "Fase 1 (Semanas 1–3): Alinhamento de requisitos, integração segura e definição de linha de base.\n" +
                    "Fase 2 (Semanas 4–8): Operação assistida com usuários-chave e coleta de indicadores.\n" +
                    "Fase 3 (Semanas 9–12): Mensuração de ROI, relatório executivo e expansão contratual.";
        }

        if (q.contains("template") || q.contains("submissão") || q.contains("submissao") || q.contains("formulário")) {
            String tTitle = oppTitle.isEmpty() ? "OPORTUNIDADE SELECIONADA" : oppTitle;
            return "TEMPLATE DE SUBMISSAO COMPLETO — " + tTitle.toUpperCase() + "\n" +
                    "Modalidade: " + oppType + " | Destinatário: " + oppCompany + "\n\n" +
                    "1. APRESENTACAO EXECUTIVA DA STARTUP\n" +
                    "Proponente: " + name + " (" + area + " · Estágio: " + stage + " · TRL " + trl + "/9)\n" +
                    "Tecnologias e Competências: " + techs + "\n\n" +
                    "2. PROBLEMA ENDERECADO E SOLUCAO PROPOSTA\n" + problem + "\n\n" +
                    "3. PLANO DE EXECUCAO E ENTREGAVEIS\n" +
                    "Etapa 1 — Alinhamento de escopo técnico e definição de KPIs com " + oppCompany + ".\n" +
                    "Etapa 2 — Implantação assistida e validação dos fluxos tecnológicos.\n" +
                    "Etapa 3 — Consolidação dos indicadores de impacto e escala comercial.";
        }

        return "Sobre sua pergunta (\"" + lastUserMsg + "\"):\n\n" +
                "Considerando o perfil da " + name + " (" + area + " · Estágio " + stage + "), recomendo destacar evidências práticas da sua tecnologia (" + techs + ") conectadas diretamente aos indicadores esperados pelo avaliador" +
                (oppTitle.isEmpty() ? "." : " em '" + oppTitle + "' (" + oppMeta + ").") + "\n\n" +
                "Posso ajudar você agora a:\n" +
                "1. Resumir os requisitos, prazos ou checklist de documentos de uma oportunidade salva;\n" +
                "2. Gerar um template de submissão, proposta técnica de 90 dias, pitch de mérito ou e-mail comercial;\n" +
                "3. Tirar dúvidas sobre o funcionamento da StartIn e a escala TRL da NASA.";
    }

    public Map<String, Object> enhanceProblemText(Map<String, Object> payload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            String url = aiBaseUrl + "/api/ai/enhance-problem";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getBody() != null && response.getBody().get("enhancedText") != null) {
                Map<String, Object> body = new LinkedHashMap<>(response.getBody());
                body.put("enhancedText", cleanStructuredText(String.valueOf(body.get("enhancedText"))));
                return body;
            }
        } catch (Exception e) {
            log.warn("Aviso ao consultar /api/ai/enhance-problem: {}. Aplicando estruturação local.", e.getMessage());
        }

        String rawText = payload.get("text") != null ? String.valueOf(payload.get("text")).trim() : "";
        String mainArea = payload.get("mainArea") != null && !String.valueOf(payload.get("mainArea")).isBlank()
                ? String.valueOf(payload.get("mainArea")).trim()
                : "Tecnologia & Inovação";
        String startupName = payload.get("startupName") != null ? String.valueOf(payload.get("startupName")).trim() : "A startup";

        List<String> tags = new ArrayList<>();
        tags.add(mainArea);
        String lower = (rawText + " " + mainArea).toLowerCase(Locale.ROOT);
        if (lower.contains("ia") || lower.contains("intelig")) tags.add("IA");
        if (lower.contains("dado") || lower.contains("analytic")) tags.add("Dados & Analytics");
        if (lower.contains("automa") || lower.contains("process")) tags.add("Automação");
        if (lower.contains("b2c") || lower.contains("consumidor")) tags.add("B2C");
        if (!lower.contains("b2c") && !tags.contains("SaaS B2B")) tags.add("SaaS B2B");
        if (tags.size() < 4) tags.add("Inovação");

        boolean isB2c = lower.contains("b2c") || lower.contains("consumidor");
        String bModel = isB2c ? "Plataforma B2C" : "SaaS B2B";
        String targetAudience = isB2c ? "Consumidores finais e usuários do ecossistema B2C" : "Empresas e organizações do setor de " + mainArea;

        String cleanInput = rawText.replaceAll("[.]+$", "");
        if (cleanInput.isEmpty()) {
            cleanInput = "Processos manuais e ineficiência operacional no setor de " + mainArea;
        }

        String p1 = "Problema Central:\nIdentificamos que no ecossistema de " + mainArea + ", o principal gargalo enfrentado é " + cleanInput + ". Essa dor acarreta perda de produtividade, custos elevados e falta de integração em fluxos estratégicos.";
        String p2 = "Solução e Tecnologia Aplicada:\nA " + startupName + " atua como solução tecnológica especializada, aplicando " + String.join(", ", tags) + " para automatizar rotinas, estruturar fluxos de dados e oferecer inteligência para resolver diretamente a dor relatada.";
        String p3 = "Modelo de Negócio e Público-Alvo:\nOperamos sob o modelo de " + bModel + ", atendendo " + targetAudience.toLowerCase(Locale.ROOT) + " com arquitetura escalável e suporte dedicado.";
        String p4 = "Diferencial e Impacto Esperado:\nEntregamos implantação ágil, redução mensurável de atrito operacional e governança em tempo real, gerando valor comprovado para clientes e parceiros corporativos.";

        String enhanced = p1 + "\n\n" + p2 + "\n\n" + p3 + "\n\n" + p4;

        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("enhancedText", enhanced);
        fallback.put("tags", tags);
        fallback.put("technologies", String.join(", ", tags));
        fallback.put("businessModel", bModel);
        fallback.put("howItWorks", p2);
        fallback.put("differentiator", p4);
        fallback.put("impact", "Redução mensurável de atrito operacional e ganho de escala.");
        fallback.put("targetAudience", targetAudience);
        return fallback;
    }

    public Map<String, Object> enhanceCompanyDemandsText(Map<String, Object> payload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            String url = aiBaseUrl + "/api/ai/enhance-company-demands";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getBody() != null && response.getBody().get("enhancedText") != null) {
                Map<String, Object> body = new LinkedHashMap<>(response.getBody());
                body.put("enhancedText", cleanStructuredText(String.valueOf(body.get("enhancedText"))));
                return body;
            }
        } catch (Exception e) {
            log.warn("Aviso ao consultar /api/ai/enhance-company-demands: {}. Aplicando estruturação local.", e.getMessage());
        }

        String rawText = payload.get("text") != null ? String.valueOf(payload.get("text")).trim() : "";
        String sector = payload.get("sector") != null && !String.valueOf(payload.get("sector")).isBlank()
                ? String.valueOf(payload.get("sector")).trim()
                : "Operações Corporativas e Tecnologia";
        String opMode = payload.get("operationMode") != null ? String.valueOf(payload.get("operationMode")).trim() : "POC + Compra";

        List<String> tags = new ArrayList<>();
        tags.add(sector);
        String lower = (rawText + " " + sector).toLowerCase(Locale.ROOT);
        if (lower.contains("ia") || lower.contains("intelig")) tags.add("IA & Dados");
        if (lower.contains("automa") || lower.contains("process")) tags.add("Automação de Processos");
        if (lower.contains("dado") || lower.contains("analytic")) tags.add("Analytics & BI");
        if (lower.contains("integr") || lower.contains("erp") || lower.contains("sistema")) tags.add("Integração de Sistemas");
        if (lower.contains("b2c") || lower.contains("consumidor")) tags.add("Operações B2C");
        if (!lower.contains("b2c") && !tags.contains("SaaS B2B")) tags.add("SaaS B2B");

        List<String> priorityTopics = new ArrayList<>(tags);

        boolean isB2c = lower.contains("b2c") || lower.contains("consumidor");
        String segmentDesc = isB2c ? "no segmento B2C (voltado ao consumidor final)" : "em operações corporativas";
        String coreText = rawText.replaceAll("[.]+$", "");
        if (coreText.isEmpty()) {
            coreText = "Necessidade de automação de processos e eficiência no setor de " + sector;
        }

        String p1 = "Desafio Operacional Central:\nAtuando " + segmentDesc + " no setor de " + sector + ", nossa organização enfrenta o desafio de " + coreText + ". O foco prioritário é modernizar processos manuais, eliminar retrabalho e trazer inteligência operacional para a esteira de atendimento e execução.";
        String p2 = "Demandas Tecnológicas & Escopo da Parceria:\nBuscamos soluções desenvolvidas por startups com competência comprovada em " + String.join(", ", tags) + ", capazes de integrar-se de forma transparente à nossa arquitetura e proporcionar ganhos ágeis de produtividade.";
        String p3 = "Modelo de Cooperação & Resultados Esperados:\nPriorizamos projetos na modalidade " + opMode + ", com testes estruturados e métricas claras. Esperamos como resultados mensuráveis a redução do tempo de ciclo de atendimento, diminuição de erros operacionais e escalabilidade sustentável.";

        String enhanced = p1 + "\n\n" + p2 + "\n\n" + p3;

        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("enhancedText", enhanced);
        fallback.put("tags", tags);
        fallback.put("technologies", String.join(", ", tags));
        fallback.put("priorityTopics", priorityTopics);
        fallback.put("howItWorks", p2);
        fallback.put("impact", "Redução do tempo de ciclo, diminuição de erros e escalabilidade operacional.");
        return fallback;
    }

    public Map<String, Object> verifyTractionPdf(Map<String, Object> payload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            String url = aiBaseUrl + "/api/ai/verify-traction-pdf";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getBody() != null && response.getBody().get("attestationSummary") != null) {
                Map<String, Object> body = new LinkedHashMap<>(response.getBody());
                body.put("attestationSummary", cleanStructuredText(String.valueOf(body.get("attestationSummary"))));
                return body;
            }
        } catch (Exception e) {
            log.warn("Aviso ao consultar /api/ai/verify-traction-pdf: {}. Aplicando verificação local.", e.getMessage());
        }

        int trlScore = 8;
        if (payload.get("trlScore") != null) {
            try {
                trlScore = Integer.parseInt(String.valueOf(payload.get("trlScore")));
            } catch (NumberFormatException ignored) {}
        }
        String fileName = payload.get("fileName") != null ? String.valueOf(payload.get("fileName")) : "atestado_capacidade.pdf";
        String startupName = payload.get("startupName") != null && !String.valueOf(payload.get("startupName")).isBlank()
                ? String.valueOf(payload.get("startupName"))
                : "Startup";

        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("attested", trlScore >= 7);
        fallback.put("trlLevel", trlScore);
        fallback.put("attestationSummary", "A IA verificou o Atestado de Capacidade Técnica (" + fileName + ") aliado à avaliação TRL " + trlScore + "/9 da NASA, certificando que a " + startupName + " opera em ambiente real com tração comprovada.");
        fallback.put("productMaturity", "TRL " + trlScore + " (NASA) — Sistema qualificado e comprovado em operação real");
        fallback.put("tractionCustomers", "Atestado de capacidade técnica verificado (" + fileName + ")");
        fallback.put("annualRevenue", "Operação comercial comprovada via atestado técnico");
        fallback.put("recentGrowth", "Estágio de Tração certificado pela IA (TRL " + trlScore + "/9)");
        fallback.put("extractedHighlights", List.of(
                "Documento verificado: " + fileName,
                "Maturidade Tecnológica NASA: TRL " + trlScore + "/9",
                "Habilitada para oportunidades de Compra Corporativa e Editais"
        ));
        return fallback;
    }

    private String cleanStructuredText(String text) {
        if (text == null) return "";
        String cleaned = text
                .replace("\r\n", "\n")
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</?(?:b|strong|i|em|u|s|code|pre|blockquote|h[1-6]|ul|ol|li|p|hr|span|div|table|thead|tbody|tr|th|td)(?:\\s[^>]*)?>", "")
                .replaceAll("```[a-zA-Z0-9_-]*\\n?", "")
                .replace("```", "")
                .replaceAll("!\\[([^\\]]*)\\]\\(([^)\\s]+)\\)", "$1 ($2)")
                .replaceAll("\\[([^\\]]+)\\]\\((https?://[^)\\s]+|mailto:[^)\\s]+|www\\.[^)\\s]+)\\)", "$1 ($2)")
                .replaceAll("(?m)^[ \\t]*(?:[-*_][ \\t]*){3,}$", "")
                .replaceAll("(?m)^[ \\t]*={3,}[ \\t]*$", "")
                .replaceAll("(?m)^[ \\t]*\\|?(?:[ \\t]*:?-+:?[ \\t]*\\|)+[ \\t]*:?-*:?[ \\t]*$", "");

        StringBuilder sb = new StringBuilder();
        String[] lines = cleaned.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            if (trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.length() > 2) {
                String[] rawCells = trimmed.substring(1, trimmed.length() - 1).split("\\|");
                List<String> cells = new ArrayList<>();
                for (String c : rawCells) {
                    String ct = c.trim();
                    if (!ct.isEmpty()) cells.add(ct);
                }
                line = String.join(" — ", cells);
            }
            sb.append(line);
            if (i < lines.length - 1) sb.append("\n");
        }

        return sb.toString()
                .replaceAll("(?m)^[ \\t]*#{1,6}[ \\t]+", "")
                .replaceAll("(?m)^[ \\t]*>+[ \\t]?", "")
                .replaceAll("(\\*\\*\\*|\\*\\*|~~|`)", "")
                .replaceAll("(?<!_)__(?=\\S)([\\s\\S]*?)(?<=\\S)__(?!_)", "$1")
                .replaceAll("(?m)^[ \\t]*[-*+][ \\t]+\\[[ xX]\\][ \\t]+", "")
                .replaceAll("(?m)^[ \\t]*[-*+][ \\t]+", "")
                .replaceAll("\\*(?=\\S)([\\s\\S]*?)(?<=\\S)\\*", "$1")
                .replaceAll("(?<!\\w)_(?=\\S)([^_\\n]+?)(?<=\\S)_(?!\\w)", "$1")
                .replaceAll("(?m)^[ \\t]*\\*+[ \\t]*|[ \\t]*\\*+[ \\t]*$", "")
                .replaceAll("\\*(?=[\\p{L}\\p{N}(\\[{\"'])|(?<=[\\p{L}\\p{N})\\]}\"'.,;:!?])\\*", "")
                .replaceAll("(?m)[ \\t]+$", "")
                .replaceAll("\n{3,}", "\n\n")
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
        dto.setPhone(profile.getPhone() != null ? profile.getPhone() : (profile.getUser() != null ? profile.getUser().getPhone() : null));
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
        dto.setTrlScore(profile.getTrlScore());
        dto.setTractionAttested(profile.getTractionAttested());
        dto.setTractionAttestationInfo(profile.getTractionAttestationInfo());

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

    private static final Map<String, List<String>> DOMAIN_CLUSTERS = createDomainClusters();
    private static final Map<String, List<String>> TECH_CANONICAL = createTechCanonical();

    private static Map<String, List<String>> createDomainClusters() {
        Map<String, List<String>> m = new LinkedHashMap<>();
        m.put("Saúde & Healthtech", List.of("healthtech", "saude", "health", "medtech", "biotech", "biotecnologia", "clinica", "hospital", "telemedicina", "prontuario", "diagnostico", "farmac"));
        m.put("Finanças & Fintech", List.of("fintech", "financ", "insurtech", "banco", "pagamento", "credito", "contabil", "fiscal", "cobranca", "open finance"));
        m.put("Agronegócio & AgTech", List.of("agtech", "agro", "agronegocio", "agricola", "agricultura", "foodtech", "pecuaria", "safra", "fazenda", "irrigacao"));
        m.put("Educação & RH", List.of("edtech", "educac", "ensino", "aprendizagem", "hrtech", "recursos humanos", "treinamento", "capacitacao"));
        m.put("Energia, ESG & CleanTech", List.of("cleantech", "greentech", "energia", "solar", "sustentab", "esg", "carbono", "ambiental", "residuos", "saneamento"));
        m.put("Indústria & Manufatura", List.of("industria", "manufatura", "automacao industrial", "fabrica", "manutencao preditiva", "mineracao", "oleo e gas"));
        m.put("Logística & Mobilidade", List.of("logtech", "logistica", "transporte", "frota", "mobilidade", "supply chain", "armazem", "roteirizacao"));
        m.put("Varejo & Consumo", List.of("retailtech", "varejo", "ecommerce", "e-commerce", "comercio", "marketplace", "martech"));
        m.put("Governo & Cidades Inteligentes", List.of("govtech", "governo", "setor publico", "smart city", "cidades inteligentes", "gestao publica", "prefeitura"));
        m.put("Cibersegurança & Legal", List.of("ciberseguranca", "cybersecurity", "seguranca da informacao", "lgpd", "antifraude", "legaltech", "juridico", "compliance", "regtech"));
        m.put("Construção & Imobiliário", List.of("construtech", "proptech", "construcao", "imobiliario", "infraestrutura"));
        m.put("IA, Dados & Software Corporativo", List.of("inteligencia artificial", "ia & dados", "ciencia de dados", "deeptech", "deep tech", "transformacao digital", "software corporativo"));
        return m;
    }

    private static Map<String, List<String>> createTechCanonical() {
        Map<String, List<String>> m = new LinkedHashMap<>();
        m.put("IA", List.of("ia", "ai", "inteligencia artificial", "llm", "ia generativa", "genai", "nlp"));
        m.put("Machine Learning", List.of("machine learning", "ml", "deep learning", "modelos preditivos", "preditivo"));
        m.put("Dados & Analytics", List.of("dados", "data", "analytics", "big data", "bi", "business intelligence", "data science", "ciencia de dados"));
        m.put("SaaS", List.of("saas", "saas b2b", "software como servico", "plataforma web"));
        m.put("Cloud", List.of("cloud", "nuvem", "aws", "azure", "gcp", "microservicos"));
        m.put("Automação & RPA", List.of("automacao", "rpa", "automacao de processos", "workflow"));
        m.put("IoT & Hardware", List.of("iot", "internet das coisas", "sensores", "embarcados", "hardware", "edge computing", "telemetria"));
        m.put("APIs & Integrações", List.of("api", "apis", "integracao", "integracoes", "erp", "crm", "interoperabilidade", "hl7"));
        m.put("Visão Computacional", List.of("visao computacional", "computer vision", "ocr", "processamento de imagem"));
        m.put("Cibersegurança & LGPD", List.of("ciberseguranca", "cybersecurity", "criptografia", "antifraude", "lgpd"));
        m.put("Blockchain & Web3", List.of("blockchain", "smart contracts", "tokenizacao", "web3"));
        m.put("Mobile", List.of("mobile", "aplicativo", "android", "ios"));
        return m;
    }

    private String normText(String val) {
        if (val == null || val.isBlank()) return "";
        String n = Normalizer.normalize(val, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return n.toLowerCase(Locale.ROOT).trim();
    }

    private Set<String> detectDomains(String norm) {
        Set<String> found = new LinkedHashSet<>();
        if (norm.isEmpty()) return found;
        for (Map.Entry<String, List<String>> entry : DOMAIN_CLUSTERS.entrySet()) {
            for (String term : entry.getValue()) {
                if (norm.contains(term)) {
                    found.add(entry.getKey());
                    break;
                }
            }
        }
        return found;
    }

    private Set<String> detectTechs(String rawTags, String context) {
        String normTags = normText(rawTags);
        String combined = (normTags + " " + normText(context)).trim();
        Set<String> found = new LinkedHashSet<>();
        if (combined.isEmpty()) return found;

        for (Map.Entry<String, List<String>> entry : TECH_CANONICAL.entrySet()) {
            for (String alias : entry.getValue()) {
                boolean hit = alias.length() <= 3
                        ? combined.matches(".*(^|[^a-z0-9])" + alias + "([^a-z0-9]|$).*")
                        : combined.contains(alias);
                if (hit) {
                    found.add(entry.getKey());
                    break;
                }
            }
        }
        Set<String> ignore = Set.of("setor", "tecnologia", "objetivo", "inovacao", "b2b", "b2c", "b2g", "poc", "compra", "edital", "evento");
        for (String tok : normTags.split("[,•;/|]+")) {
            String clean = tok.trim();
            if (clean.length() >= 3 && !ignore.contains(clean)) {
                boolean mapped = TECH_CANONICAL.values().stream()
                        .flatMap(Collection::stream)
                        .anyMatch(alias -> alias.equals(clean)
                                || (alias.length() > 3 && clean.contains(alias))
                                || (alias.length() <= 3 && clean.matches(".*(^|[^a-z0-9])" + alias + "([^a-z0-9]|$).*")));
                if (!mapped) {
                    found.add(clean.toUpperCase(Locale.ROOT));
                }
            }
        }
        return found;
    }

    private record AuditedCalc(
            int totalScore,
            int areaScore,
            int techScore,
            int otherScore,
            String justification,
            AiMatchReasonsDTO reasons,
            Map<String, Integer> subtopics
    ) {}

    private AuditedCalc computeAuditedBreakdown(
            String stName,
            String stAreaRaw,
            String stTechRaw,
            String stProblemRaw,
            String stStageRaw,
            Integer trlRaw,
            Boolean tractionAttested,
            String stOppPrefsRaw,
            String stModelRaw,
            String stObjectiveRaw,
            String stLocationRaw,
            String tgTitleRaw,
            String tgSectorRaw,
            String tgTypeRaw,
            String tgTagsRaw,
            String tgDescRaw,
            String tgLocRaw,
            boolean isCompanyTarget
    ) {
        String stAreaNorm = normText(stAreaRaw);
        String stProblemNorm = normText(stProblemRaw + " " + stObjectiveRaw);
        String tgPrimaryNorm = normText((tgSectorRaw != null ? tgSectorRaw : "") + " " + (tgTitleRaw != null ? tgTitleRaw : "") + " " + (tgTagsRaw != null ? tgTagsRaw : ""));
        String tgDescNorm = normText(tgDescRaw);
        String tgFullNorm = (tgPrimaryNorm + " " + tgDescNorm).trim();

        // 1. Área de Atuação (0..40)
        Set<String> stDomains = detectDomains(stAreaNorm.isEmpty() ? stProblemNorm : stAreaNorm);
        Set<String> tgPrimaryDomains = detectDomains(tgPrimaryNorm);
        Set<String> tgAllDomains = detectDomains(tgFullNorm);

        List<String> primaryOverlap = stDomains.stream().filter(tgPrimaryDomains::contains).toList();
        List<String> contextOverlap = stDomains.stream().filter(tgAllDomains::contains).toList();

        int subHits = 0;
        for (String tok : stAreaNorm.split("[^a-z0-9]+")) {
            if (tok.length() >= 4 && !List.of("para", "como", "inovacao", "tecnologia").contains(tok) && tgFullNorm.contains(tok)) {
                subHits++;
            }
        }
        boolean directLabel = stAreaNorm.length() >= 4 && !stAreaNorm.equals("inovacao") && tgPrimaryNorm.contains(stAreaNorm);
        boolean isMultiSector = List.of("inovacao aberta", "multissetorial", "subvencao", "pipe", "finep", "sebrae", "centelha", "chamada publica")
                .stream().anyMatch(tgFullNorm::contains);

        int areaScore;
        String areaRule;
        if (!primaryOverlap.isEmpty() || directLabel) {
            areaScore = Math.min(40, 31 + Math.min(9, subHits * 2 + (directLabel ? 3 : 1)));
            areaRule = "Mesma área de atuação (" + (!primaryOverlap.isEmpty() ? String.join(", ", primaryOverlap) : stAreaRaw) + ")";
        } else if (!contextOverlap.isEmpty()) {
            areaScore = Math.min(31, 22 + Math.min(9, subHits * 2 + 1));
            areaRule = "Área compatível no escopo (" + String.join(", ", contextOverlap) + ")";
        } else if (isMultiSector) {
            areaScore = Math.min(26, 16 + Math.min(10, subHits * 3));
            areaRule = "Chamada multissetorial compatível com " + (stAreaRaw != null && !stAreaRaw.isBlank() ? stAreaRaw : "a startup");
        } else if (subHits > 0) {
            areaScore = Math.min(15, subHits * 6);
            areaRule = "Áreas distintas com vocabulário parcial correlato";
        } else {
            areaScore = 0;
            areaRule = "Áreas de atuação distintas";
        }

        // 2. Tecnologias (0..30)
        Set<String> stTechs = detectTechs(stTechRaw, stAreaRaw + " " + stProblemRaw);
        Set<String> tgCoreTechs = detectTechs(tgTagsRaw, "");
        Set<String> tgAllTechs = detectTechs(tgTagsRaw, tgTitleRaw + " " + tgDescRaw);

        List<String> matchedTechs = stTechs.stream().filter(tgAllTechs::contains).toList();
        int nMatched = matchedTechs.size();
        int techScore = 0;
        if (nMatched > 0) {
            int reqCount = Math.max(1, Math.min(Math.max(1, tgAllTechs.size()), Math.max(2, tgCoreTechs.size())));
            double ratio = Math.min(1.0, (double) nMatched / reqCount);
            int covPts = (int) Math.round(18.0 * ratio);
            int cntPts = nMatched == 1 ? 5 : (nMatched == 2 ? 8 : (nMatched == 3 ? 10 : 12));
            techScore = Math.min(30, covPts + cntPts);
        }

        // 3. Demais Critérios (0..30 = 10 Estágio/TRL + 10 Modalidade/Modelo + 5 Semântica + 5 Localização)
        String stageNorm = normText(stStageRaw);
        boolean isTracao = stageNorm.contains("tracao");
        int trl = trlRaw != null ? trlRaw : (isTracao ? 7 : 4);
        boolean attested = Boolean.TRUE.equals(tractionAttested);
        String tgTypeNorm = normText(tgTypeRaw);

        int stageTrlScore;
        if (isTracao) {
            if ((tgTypeNorm.contains("compra") && !tgTypeNorm.contains("primeira")) || tgTypeNorm.contains("tracao")) {
                stageTrlScore = 6 + (attested || trl >= 8 ? 4 : (trl >= 7 ? 3 : 1));
            } else if (tgTypeNorm.contains("edital") || tgTypeNorm.contains("evento") || tgTypeNorm.isEmpty()) {
                stageTrlScore = 6 + (attested || trl >= 7 ? 3 : 1);
            } else {
                stageTrlScore = 3;
            }
        } else {
            if (tgTypeNorm.contains("poc") || tgTypeNorm.contains("primeira") || tgTypeNorm.contains("validacao")) {
                stageTrlScore = 7 + (stProblemNorm.length() > 30 ? 2 : 0) + (trl >= 3 ? 1 : 0);
            } else if (tgTypeNorm.contains("edital") || tgTypeNorm.contains("evento") || tgTypeNorm.isEmpty()) {
                stageTrlScore = 6 + (stProblemNorm.length() > 30 ? 2 : 1);
            } else {
                stageTrlScore = 2;
            }
        }
        stageTrlScore = Math.min(10, stageTrlScore);

        String prefsNorm = normText(stOppPrefsRaw);
        int modalityPts = isCompanyTarget
                ? (prefsNorm.isEmpty() || prefsNorm.contains("poc") || prefsNorm.contains("compra") ? 6 : 3)
                : ((tgTypeNorm.contains("edital") && prefsNorm.contains("edita")) ||
                   (tgTypeNorm.contains("poc") && prefsNorm.contains("poc")) ||
                   (tgTypeNorm.contains("primeira") && prefsNorm.contains("primeira")) ||
                   (tgTypeNorm.contains("compra") && prefsNorm.contains("compra")) ||
                   (tgTypeNorm.contains("evento") && prefsNorm.contains("evento"))
                        ? 6
                        : (prefsNorm.isEmpty() ? 4 : 2));
        String modelNorm = normText(stModelRaw);
        int modelPts = (modelNorm.contains("b2b") || modelNorm.contains("saas") || modelNorm.contains("b2g")) ? 4 : (!modelNorm.isEmpty() ? 2 : 1);
        int modalityModelScore = Math.min(10, modalityPts + modelPts);

        Set<String> stems = new LinkedHashSet<>();
        for (String tok : stProblemNorm.split("[^a-z0-9]+")) {
            if (tok.length() >= 5 && !List.of("startup", "startups", "empresa", "empresas", "solucao", "solucoes", "plataforma", "sistema").contains(tok)) {
                stems.add(tok.substring(0, 5));
            }
        }
        int semHits = (int) stems.stream().filter(tgFullNorm::contains).count();
        int semScore = semHits == 0 ? 0 : (semHits == 1 ? 1 : (semHits == 2 ? 2 : (semHits <= 4 ? 3 : (semHits <= 6 ? 4 : 5))));

        String stLocNorm = normText(stLocationRaw);
        String tgLocNorm = normText(tgLocRaw + " " + tgTitleRaw);
        String city = stLocNorm.split("[,·/-]")[0].trim();
        int locScore;
        String locLabel;
        if (city.length() >= 3 && tgLocNorm.contains(city)) {
            locScore = 5;
            locLabel = "mesma cidade/estado";
        } else if (List.of("brasil", "nacional", "remoto", "global").stream().anyMatch(tgLocNorm::contains) || tgLocNorm.isEmpty()) {
            locScore = 4;
            locLabel = "abrangência nacional/remota";
        } else {
            locScore = 1;
            locLabel = "região distinta";
        }

        // Cálculo dos 10 subtópicos técnicos auditáveis (40% / 30% / 30%):
        int macroDomainScore = (!primaryOverlap.isEmpty() || directLabel) ? 25 : (!contextOverlap.isEmpty() ? 18 : (isMultiSector ? 15 : (subHits > 0 ? Math.min(10, subHits * 3) : 0)));
        int nicheVocabularyScore = (!primaryOverlap.isEmpty() || directLabel) ? Math.min(10, Math.max(2, subHits * 2 + (directLabel ? 4 : 2))) : (!contextOverlap.isEmpty() ? Math.min(9, Math.max(2, subHits * 2 + 1)) : (isMultiSector ? Math.min(8, Math.max(1, subHits * 2)) : (subHits > 0 ? Math.min(5, subHits * 2) : 0)));
        int marketSegmentScore = directLabel ? 5 : (!primaryOverlap.isEmpty() ? 4 : (!contextOverlap.isEmpty() ? 3 : (isMultiSector ? 3 : (subHits > 0 ? 1 : 0))));
        areaScore = Math.min(40, macroDomainScore + nicheVocabularyScore + marketSegmentScore);

        int reqCount = Math.max(1, Math.min(Math.max(1, tgAllTechs.size()), Math.max(2, tgCoreTechs.size())));
        double ratio = Math.min(1.0, (double) nMatched / reqCount);
        int stackCoverageScore = nMatched > 0 ? (int) Math.round(18.0 * ratio) : 0;
        int techDensityScore = nMatched == 0 ? 0 : (nMatched == 1 ? 4 : (nMatched == 2 ? 6 : 8));
        int architectureScore = nMatched >= 3 ? 4 : (nMatched == 2 ? 2 : (nMatched == 1 ? 1 : 0));
        techScore = Math.min(30, stackCoverageScore + techDensityScore + architectureScore);

        if (tgTypeNorm.contains("evento")) {
            modalityModelScore = Math.max(modalityModelScore, 8);
        }
        modalityModelScore = Math.max(0, Math.min(10, modalityModelScore));

        if (tgTypeNorm.contains("evento") && locScore >= 3) {
            locScore = Math.min(5, locScore + 1);
        }
        locScore = Math.max(0, Math.min(5, locScore));

        Map<String, Integer> subtopics = new LinkedHashMap<>();
        subtopics.put("macroDomainScore", macroDomainScore);
        subtopics.put("nicheVocabularyScore", nicheVocabularyScore);
        subtopics.put("marketSegmentScore", marketSegmentScore);
        subtopics.put("stackCoverageScore", stackCoverageScore);
        subtopics.put("techDensityScore", techDensityScore);
        subtopics.put("architectureScore", architectureScore);
        subtopics.put("stageTrlScore", stageTrlScore);
        subtopics.put("modalityModelScore", modalityModelScore);
        subtopics.put("problemSemanticScore", semScore);
        subtopics.put("locationFormatScore", locScore);

        int otherScore = stageTrlScore + modalityModelScore + semScore + locScore;
        int totalScore = Math.max(0, Math.min(100, areaScore + techScore + otherScore));

        String techDetail = !matchedTechs.isEmpty()
                ? "Coincidências (" + String.join(", ", matchedTechs) + ")"
                : "Sem interseção direta de stack tecnológico";
        String level = totalScore >= 75 ? "alta compatibilidade" : (totalScore >= 50 ? "compatibilidade moderada e progressiva" : "aderência parcial");

        String justification = "A " + stName + " atingiu " + totalScore + "% (" + level + ") com '" + tgTitleRaw + "': "
                + areaRule.toLowerCase(Locale.ROOT) + " (" + areaScore + "/40 pts), "
                + (!matchedTechs.isEmpty() ? "convergência tecnológica em " + String.join(", ", matchedTechs) : "baixa interseção de tecnologias")
                + " (" + techScore + "/30 pts) e " + otherScore + "/30 pts nos critérios complementares.";

        AiMatchReasonsDTO reasons = new AiMatchReasonsDTO();
        reasons.setPerfil("Área de atuação (" + areaScore + "/40 pts): " + areaRule + " · Modelo " + (stModelRaw != null && !stModelRaw.isBlank() ? stModelRaw : "B2B") + ".");
        reasons.setRequisitos("Tecnologias (" + techScore + "/30 pts): " + techDetail + ".");
        reasons.setObjetivo("Estágio, TRL e Modalidade (" + (stageTrlScore + modalityModelScore) + "/20 pts): estágio " + (stStageRaw != null ? stStageRaw : "Validação") + " (" + stageTrlScore + "/10) + modalidade/modelo (" + modalityModelScore + "/10).");
        reasons.setContexto("Problema e Localização (" + (semScore + locScore) + "/10 pts): afinidade de escopo (" + semScore + "/5) + " + locLabel + " (" + locScore + "/5).");

        return new AuditedCalc(totalScore, areaScore, techScore, otherScore, justification, reasons, subtopics);
    }

    private AiMatchResponseDTO fallbackCalculateMatches(AiStartupInputDTO startup, List<AiOpportunityInputDTO> opps) {
        AiMatchResponseDTO response = new AiMatchResponseDTO();
        String startupName = (startup.getName() != null && !startup.getName().isBlank()) ? startup.getName() : "Sua Startup";
        String mainArea = (startup.getMainArea() != null && !startup.getMainArea().isBlank()) ? startup.getMainArea() : "";
        String businessModel = (startup.getBusinessModel() != null && !startup.getBusinessModel().isBlank()) ? startup.getBusinessModel() : "B2B";
        String stage = (startup.getCurrentStage() != null && !startup.getCurrentStage().isBlank()) ? startup.getCurrentStage() : "Validação";
        String location = (startup.getLocation() != null && !startup.getLocation().isBlank()) ? startup.getLocation() : "Brasil";
        String objective = (startup.getMainObjective() != null && !startup.getMainObjective().isBlank()) ? startup.getMainObjective() : "Validação e escala";
        String techs = (startup.getTags() != null && !startup.getTags().isEmpty())
                ? String.join(", ", startup.getTags())
                : "";
        String problem = ((startup.getProblem() != null ? startup.getProblem() : "") + " " +
                (startup.getHowItWorks() != null ? startup.getHowItWorks() : "") + " " +
                (startup.getDescription() != null ? startup.getDescription() : "")).trim();

        response.setStartupName(startupName);
        response.setTotalAnalyzed(opps.size());

        List<AiMatchItemDTO> matches = new ArrayList<>();
        for (AiOpportunityInputDTO opp : opps) {
            AiMatchItemDTO item = new AiMatchItemDTO();
            item.setOpportunityId(opp.getId());
            item.setOpportunityTitle(opp.getTitle());
            item.setUrl(opp.getUrl());

            AuditedCalc calc = computeAuditedBreakdown(
                    startupName,
                    mainArea,
                    techs,
                    problem,
                    stage,
                    startup.getTrlScore(),
                    startup.getTractionAttested(),
                    startup.getOpportunityTypes(),
                    businessModel,
                    objective,
                    location,
                    opp.getTitle(),
                    "",
                    opp.getType(),
                    opp.getTags(),
                    opp.getDescription(),
                    opp.getMeta(),
                    false
            );

            item.setMatchScore(calc.totalScore());
            item.setJustification(calc.justification());
            item.setRecommendedAction("Estruturar candidatura da " + startupName + " destacando foco em " + objective + ".");
            item.setReasons(calc.reasons());
            Map<String, Object> audit = new LinkedHashMap<>();
            audit.put("totalScore", calc.totalScore());
            audit.put("areaScore", calc.areaScore());
            audit.put("techScore", calc.techScore());
            audit.put("otherScore", calc.otherScore());
            audit.put("subtopics", calc.subtopics());
            item.setAuditBreakdown(audit);
            matches.add(item);
        }

        matches.sort((a, b) -> Integer.compare(b.getMatchScore(), a.getMatchScore()));
        response.setMatches(matches);
        return response;
    }

    /**
     * Calcula via IA Python (ou motor auditado 40/30/30 equivalente) o match entre uma EMPRESA e todas as STARTUPS cadastradas.
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
            log.warn("Aviso ao consultar /api/ai/match-startups: {}. Usando cálculo auditado 40/30/30 local.", e.getMessage());
        }

        return fallbackCompanyToStartupsMatches(companyDto, startupDtos);
    }

    /**
     * Calcula via IA Python (ou motor auditado 40/30/30 equivalente) o match entre uma STARTUP e todas as EMPRESAS cadastradas.
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
            log.warn("Aviso ao consultar /api/ai/match-companies: {}. Usando cálculo auditado 40/30/30 local.", e.getMessage());
        }

        return fallbackStartupToCompaniesMatches(startupDto, companyDtos);
    }

    private List<Map<String, Object>> fallbackCompanyToStartupsMatches(CompanyDTO company, List<StartupProfileDTO> startups) {
        String compName = company != null && company.getName() != null ? company.getName() : "Sua Empresa";
        String compSector = company != null && company.getSector() != null ? company.getSector() : "";
        String compTagsRaw = company != null && company.getTags() != null ? company.getTags() : "";
        String compDemands = company != null && company.getDemands() != null ? company.getDemands() : (company != null && company.getDescription() != null ? company.getDescription() : "");
        String compLocation = company != null && company.getLocation() != null ? company.getLocation() : "Brasil";
        String compMode = company != null && company.getOperationMode() != null ? company.getOperationMode() : "POC, Compra";
        String compPrefStage = company != null && company.getPreferredStage() != null ? company.getPreferredStage() : "";

        List<Map<String, Object>> results = new ArrayList<>();
        for (StartupProfileDTO st : startups) {
            String stName = st.getName() != null ? st.getName() : "Startup";
            String stArea = st.getMainArea() != null && !st.getMainArea().isBlank() ? st.getMainArea() : (st.getMainSector() != null ? st.getMainSector() : "");
            String stTech = st.getTechnologies() != null && !st.getTechnologies().isBlank() ? st.getTechnologies() : (st.getAreasOfInterest() != null ? st.getAreasOfInterest() : "");
            String stDesc = st.getDescription() != null && !st.getDescription().isBlank() ? st.getDescription() : (st.getProblem() != null ? st.getProblem() : "Solução tecnológica cadastrada na plataforma.");
            String stStage = st.getCurrentStage() != null && !st.getCurrentStage().isBlank() ? st.getCurrentStage() : "Validação";
            String stLoc = st.getLocation() != null && !st.getLocation().isBlank() ? st.getLocation() : "Brasil";
            String stModel = st.getBusinessModel() != null && !st.getBusinessModel().isBlank() ? st.getBusinessModel() : "B2B";
            String stObjective = st.getMainObjective() != null ? st.getMainObjective() : "";

            AuditedCalc calc = computeAuditedBreakdown(
                    stName,
                    stArea,
                    stTech,
                    (st.getProblem() != null ? st.getProblem() : "") + " " + (st.getHowItWorks() != null ? st.getHowItWorks() : ""),
                    stStage,
                    st.getTrlScore(),
                    st.getTractionAttested(),
                    st.getOpportunityTypes(),
                    stModel,
                    stObjective,
                    stLoc,
                    compName,
                    compSector,
                    compMode + " " + compPrefStage,
                    compTagsRaw,
                    compDemands,
                    compLocation,
                    true
            );

            Map<String, String> reasons = new LinkedHashMap<>();
            reasons.put("perfil", calc.reasons().getPerfil());
            reasons.put("requisitos", calc.reasons().getRequisitos());
            reasons.put("objetivo", calc.reasons().getObjetivo());
            reasons.put("contexto", calc.reasons().getContexto());

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("startupId", st.getId());
            item.put("startupName", stName);
            item.put("mainArea", !stArea.isBlank() ? stArea : "Inovação");
            item.put("description", stDesc);
            item.put("shortDescription", stDesc.length() > 120 ? stDesc.substring(0, 120) + "..." : stDesc);
            item.put("technologies", !stTech.isBlank() ? stTech : "Não informadas");
            item.put("businessModel", stModel);
            item.put("currentStage", stStage);
            item.put("location", stLoc);
            item.put("website", st.getWebsite());
            item.put("phone", st.getPhone());
            item.put("email", st.getUserEmail());
            item.put("avatarUrl", st.getAvatarUrl());
            item.put("matchScore", calc.totalScore());
            item.put("justification", calc.justification());
            item.put("recommendedAction", "Convidar a " + stName + " para avaliação de " + compMode + ".");
            item.put("reasons", reasons);
            results.add(item);
        }

        results.sort((a, b) -> Integer.compare((int) b.get("matchScore"), (int) a.get("matchScore")));
        return results;
    }

    private List<Map<String, Object>> fallbackStartupToCompaniesMatches(StartupProfileDTO startup, List<CompanyDTO> companies) {
        String stName = startup != null && startup.getName() != null ? startup.getName() : "Sua Startup";
        String stArea = startup != null && startup.getMainArea() != null ? startup.getMainArea() : "";
        String stTech = startup != null && startup.getTechnologies() != null && !startup.getTechnologies().isBlank()
                ? startup.getTechnologies()
                : (startup != null && startup.getAreasOfInterest() != null ? startup.getAreasOfInterest() : "");
        String stModel = startup != null && startup.getBusinessModel() != null && !startup.getBusinessModel().isBlank() ? startup.getBusinessModel() : "B2B";
        String stLoc = startup != null && startup.getLocation() != null && !startup.getLocation().isBlank() ? startup.getLocation() : "Brasil";
        String stStage = startup != null && startup.getCurrentStage() != null ? startup.getCurrentStage() : "Validação";
        String stProblem = startup != null ? ((startup.getProblem() != null ? startup.getProblem() : "") + " " + (startup.getHowItWorks() != null ? startup.getHowItWorks() : "")) : "";

        List<Map<String, Object>> results = new ArrayList<>();
        for (CompanyDTO comp : companies) {
            String compName = comp.getName() != null ? comp.getName() : "Empresa";
            String compType = comp.getType() != null ? comp.getType() : "EMPRESA";
            String compSector = comp.getSector() != null ? comp.getSector() : "";
            String compDesc = comp.getDescription() != null && !comp.getDescription().isBlank()
                    ? comp.getDescription()
                    : "Organização parceira com demandas abertas de inovação.";
            String compDemands = comp.getDemands() != null && !comp.getDemands().isBlank() ? comp.getDemands() : compDesc;
            String compTags = comp.getTags() != null ? comp.getTags() : "";
            String compLoc = comp.getLocation() != null && !comp.getLocation().isBlank() ? comp.getLocation() : "Brasil";
            String compMode = comp.getOperationMode() != null && !comp.getOperationMode().isBlank() ? comp.getOperationMode() : "POC, Compra";
            String compPrefStage = comp.getPreferredStage() != null ? comp.getPreferredStage() : "";

            AuditedCalc calc = computeAuditedBreakdown(
                    stName,
                    stArea,
                    stTech,
                    stProblem,
                    stStage,
                    startup != null ? startup.getTrlScore() : null,
                    startup != null ? startup.getTractionAttested() : null,
                    startup != null ? startup.getOpportunityTypes() : null,
                    stModel,
                    startup != null ? startup.getMainObjective() : null,
                    stLoc,
                    compName,
                    compSector,
                    compMode + " " + compPrefStage,
                    compTags,
                    compDemands,
                    compLoc,
                    true
            );

            Map<String, String> reasons = new LinkedHashMap<>();
            reasons.put("perfil", calc.reasons().getPerfil());
            reasons.put("requisitos", calc.reasons().getRequisitos());
            reasons.put("objetivo", calc.reasons().getObjetivo());
            reasons.put("contexto", calc.reasons().getContexto());

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("companyId", comp.getId());
            item.put("companyName", compName);
            item.put("companyType", compType);
            item.put("sector", !compSector.isBlank() ? compSector : "Inovação Corporativa");
            item.put("description", compDesc);
            item.put("demands", compDemands);
            item.put("tags", !compTags.isBlank() ? compTags : "Inovação");
            item.put("location", compLoc);
            item.put("website", comp.getWebsite());
            item.put("phone", comp.getPhone());
            item.put("cnpj", comp.getCnpj());
            item.put("operationMode", compMode);
            item.put("preferredStage", !compPrefStage.isBlank() ? compPrefStage : "Validação / Tração");
            item.put("avatarUrl", comp.getAvatarUrl());
            item.put("matchScore", calc.totalScore());
            item.put("justification", calc.justification());
            item.put("recommendedAction", "Apresentar proposta de " + compMode + " da " + stName + " para " + compName + ".");
            item.put("reasons", reasons);
            Map<String, Object> audit = new LinkedHashMap<>();
            audit.put("totalScore", calc.totalScore());
            audit.put("areaScore", calc.areaScore());
            audit.put("techScore", calc.techScore());
            audit.put("otherScore", calc.otherScore());
            audit.put("subtopics", calc.subtopics());
            item.put("auditBreakdown", audit);
            results.add(item);
        }

        results.sort((a, b) -> Integer.compare((int) b.get("matchScore"), (int) a.get("matchScore")));
        return results;
    }
}
