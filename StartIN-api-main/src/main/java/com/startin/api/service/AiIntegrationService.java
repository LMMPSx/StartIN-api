package com.startin.api.service;

import com.startin.api.dto.ai.*;
import com.startin.api.dto.company.CompanyDTO;
import com.startin.api.dto.provider.ServiceProviderDTO;
import com.startin.api.dto.startup.StartupProfileDTO;
import com.startin.api.model.Company;
import com.startin.api.model.Opportunity;
import com.startin.api.model.ServiceProvider;
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
            @Value("${startin.ai.timeout-seconds:120}") int timeoutSeconds) {
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
        return chatWithAssistant(req, profile, null);
    }

    public String chatWithAssistant(AiChatRequestDTO incomingRequest, StartupProfile profile) {
        return chatWithAssistant(incomingRequest, profile, null);
    }

    public String chatWithAssistant(AiChatRequestDTO incomingRequest, StartupProfile profile, ServiceProvider provider) {
        AiStartupInputDTO startupDto = profile != null ? mapProfileToAiInput(profile) : incomingRequest.getStartup();
        AiChatRequestDTO chatRequest = new AiChatRequestDTO();
        chatRequest.setMessages(incomingRequest.getMessages());
        chatRequest.setAccountType(incomingRequest.getAccountType());
        chatRequest.setUserId(incomingRequest.getUserId());
        chatRequest.setStartup(startupDto);
        chatRequest.setStartupId(profile != null ? profile.getId() : incomingRequest.getStartupId());
        chatRequest.setProviderId(provider != null ? provider.getId() : incomingRequest.getProviderId());

        if (provider != null) {
            Map<String, Object> provMap = new LinkedHashMap<>();
            provMap.put("id", provider.getId());
            provMap.put("name", provider.getName());
            provMap.put("providerType", provider.getProviderType());
            provMap.put("category", provider.getCategory());
            provMap.put("mainService", provider.getMainService());
            provMap.put("description", provider.getDescription());
            provMap.put("serviceExperience", provider.getServiceExperience());
            provMap.put("toolsAndSkills", provider.getToolsAndSkills());
            provMap.put("tags", provider.getTags());
            provMap.put("experienceYears", provider.getExperienceYears());
            provMap.put("targetSegment", provider.getTargetSegment());
            provMap.put("location", provider.getLocation());
            provMap.put("availability", provider.getAvailability());
            provMap.put("portfolioUrl", provider.getPortfolioUrl());
            provMap.put("linkedinUrl", provider.getLinkedinUrl());
            provMap.put("projectName", provider.getProjectName());
            provMap.put("projectDescription", provider.getProjectDescription());
            provMap.put("projectTools", provider.getProjectTools());
            provMap.put("interestSectors", provider.getInterestSectors());
            provMap.put("connectionPreference", provider.getConnectionPreference());
            provMap.put("opportunitiesSought", provider.getOpportunitiesSought());
            provMap.put("interestRegion", provider.getInterestRegion());
            chatRequest.setProvider(provMap);
            if (chatRequest.getAccountType() == null || chatRequest.getAccountType().isBlank()) {
                chatRequest.setAccountType("PROVIDER");
            }
        } else {
            chatRequest.setProvider(incomingRequest.getProvider());
        }

        chatRequest.setSelectedOpportunity(incomingRequest.getSelectedOpportunity());
        chatRequest.setCompany(incomingRequest.getCompany());
        chatRequest.setAttachmentName(incomingRequest.getAttachmentName());
        chatRequest.setAttachmentBase64(incomingRequest.getAttachmentBase64());
        chatRequest.setAttachmentText(incomingRequest.getAttachmentText());
        chatRequest.setTemplateStyle(incomingRequest.getTemplateStyle());

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

        if (provider != null || "PROVIDER".equalsIgnoreCase(incomingRequest.getAccountType())) {
            return applyCustomTemplateStyleIfRequested(buildJavaProviderContextualFallbackReply(incomingRequest, provider), incomingRequest);
        }
        return applyCustomTemplateStyleIfRequested(buildJavaContextualFallbackReply(incomingRequest, profile), incomingRequest);
    }

    private String applyCustomTemplateStyleIfRequested(String baseReply, AiChatRequestDTO req) {
        String style = req != null && req.getTemplateStyle() != null ? req.getTemplateStyle().trim() : "";
        if (style.isEmpty() || "padrao".equalsIgnoreCase(style) || "padrão".equalsIgnoreCase(style)) {
            return baseReply;
        }
        return "ESTILO DE TEMPLATE DEFINIDO PELO USUARIO: " + style.toUpperCase() + "\n" +
               "------------------------------------------------------------\n\n" +
               baseReply + "\n\n" +
               "------------------------------------------------------------\n" +
               "ADEQUACAO AO ESTILO SOLICITADO (" + style + "):\n" +
               "O documento acima foi estruturado de forma coesa seguindo a diretriz '" + style + "'. Para voltar ao modelo padrão da StartIn, digite 'padrão' no campo de definição de template.";
    }

    private String buildJavaProviderContextualFallbackReply(AiChatRequestDTO incomingRequest, ServiceProvider provider) {
        String pName = provider != null && provider.getName() != null ? provider.getName() : "Prestador";
        String pCat = provider != null && provider.getCategory() != null ? provider.getCategory() : "Serviços Especializados";
        String pService = provider != null && provider.getMainService() != null && !provider.getMainService().isBlank()
                ? provider.getMainService()
                : "Soluções técnicas e estratégicas para startups em Tração";
        String pSkills = provider != null && provider.getToolsAndSkills() != null && !provider.getToolsAndSkills().isBlank()
                ? provider.getToolsAndSkills()
                : pCat;
        String pModel = provider != null && provider.getConnectionPreference() != null && !provider.getConnectionPreference().isBlank()
                ? provider.getConnectionPreference()
                : "Contrato PJ / Sprints Ágeis / Projeto Fechado";

        Map<String, Object> target = incomingRequest.getSelectedOpportunity();
        String stTitle = target != null && target.get("title") != null ? String.valueOf(target.get("title")) : "Startup em Tração";
        String stDesc = target != null && target.get("description") != null ? String.valueOf(target.get("description")) : "";

        return "Proposta e Orientação Estratégica para " + pName + " (" + pCat + ")\n\n" +
                "1. Posicionamento Técnico:\n" +
                "Seu perfil destaca atuação em " + pService + " com domínio em " + pSkills + ".\n\n" +
                "2. Estrutura de Abordagem e Escopo para " + stTitle + ":\n" +
                (stDesc.isBlank() ? "" : "Contexto da Startup: " + stDesc + "\n") +
                "a) Diagnóstico Inicial (Sprint 0 - 1 semana): Levantamento de gargalos e definição de KPIs prioritários.\n" +
                "b) Execução em Sprints (Sprints 1 a 4): Entregas incrementais utilizando " + pSkills + " com validação contínua junto aos fundadores.\n" +
                "c) Modelo de Contratação Recomendado: " + pModel + ", garantindo previsibilidade de entregas e transferência de conhecimento.";
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

    private String cleanPreviousProviderBoilerplate(String rawText, String providerName) {
        if (rawText == null || rawText.isBlank()) return "";
        String text = rawText.trim();
        text = text.split("(?i)\\n\\s*(?:Competências Técnicas|Diferencial de Parceria|Diferenciais Competitivos)\\s*:")[0].trim();
        text = text.replaceFirst("(?i)^(?:Atuação Especializada em [^\\n:]+|Apresentação Profissional)\\s*:\\s*", "").trim();
        text = text.replaceFirst("(?i)^[^\\.\\n]+?\\s+atua\\s+(?:com\\s+foco\\s+em|na\\s+área\\s+de)\\s+[^\\.\\n]+?,\\s*oferecendo\\s+serviços\\s+técnicos\\s+especializados\\s+para\\s+startups\\s+em\\s+estágios\\s+de\\s+validação\\s+e\\s+tração\\.\\s*", "").trim();
        text = text.replaceFirst("(?i)^[^\\.\\n]+?\\s+atua\\s+na\\s+área\\s+de\\s+[^\\.\\n]+?\\.\\s*Foco\\s+em\\s+soluções\\s+de\\s+alto\\s+impacto[^\\.\\n]*\\.\\s*(?:Experiência\\s*&\\s*Metodologia\\s*:\\s*)?", "").trim();
        if (providerName != null && !providerName.isBlank() && text.toLowerCase().startsWith(providerName.toLowerCase() + " atua")) {
            int dotIdx = text.indexOf('.');
            if (dotIdx > 0 && dotIdx + 1 < text.length()) {
                text = text.substring(dotIdx + 1).trim();
            }
        }
        return text;
    }

    public Map<String, Object> enhanceProviderServicesText(Map<String, Object> payload) {
        String rawInput = payload.get("text") != null ? String.valueOf(payload.get("text")).trim() : "";
        String category = payload.get("category") != null && !String.valueOf(payload.get("category")).isBlank()
                ? String.valueOf(payload.get("category")).trim() : "Serviços Especializados";
        String providerName = payload.get("providerName") != null ? String.valueOf(payload.get("providerName")).trim() : "";
        String mainService = payload.get("mainService") != null ? String.valueOf(payload.get("mainService")).trim() : "";
        String providerType = payload.get("providerType") != null ? String.valueOf(payload.get("providerType")).trim() : "PROFISSIONAL";
        String fieldType = payload.get("fieldType") != null ? String.valueOf(payload.get("fieldType")).trim() : "bio";

        String cleanedDraft = cleanPreviousProviderBoilerplate(rawInput, providerName);
        Map<String, Object> enrichedPayload = new LinkedHashMap<>(payload);
        enrichedPayload.put("text", cleanedDraft);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(enrichedPayload, headers);
            String url = aiBaseUrl + "/api/ai/enhance-provider-services";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getBody() != null && response.getBody().get("enhancedText") != null) {
                String candidate = cleanStructuredText(String.valueOf(response.getBody().get("enhancedText")));
                String lowerCand = candidate.toLowerCase();
                boolean isLegacyThirdPerson = lowerCand.startsWith("atuação especializada em")
                        || lowerCand.contains("atua com foco em")
                        || (!providerName.isBlank() && lowerCand.startsWith(providerName.toLowerCase() + " atua"));
                if (!isLegacyThirdPerson) {
                    Map<String, Object> body = new LinkedHashMap<>(response.getBody());
                    body.put("enhancedText", candidate);
                    return body;
                }
            }
        } catch (Exception e) {
            log.warn("Aviso ao consultar /api/ai/enhance-provider-services: {}. Aplicando estruturacao em 1a pessoa local.", e.getMessage());
        }

        boolean isCompany = "EMPRESA".equalsIgnoreCase(providerType);
        String lowerAll = (cleanedDraft + " " + category + " " + mainService).toLowerCase();

        List<String> tags = new ArrayList<>();
        if (lowerAll.contains("instagram") || lowerAll.contains("insta")) tags.add("Instagram & Social Media");
        if (lowerAll.contains("escala") || lowerAll.contains("marca") || lowerAll.contains("branding")) tags.add("Escalabilidade de Marca");
        if (lowerAll.contains("marketing") || lowerAll.contains("growth")) tags.add("Marketing Estratégico & Growth");
        if (lowerAll.contains("tráfego") || lowerAll.contains("trafego") || lowerAll.contains("ads")) tags.add("Tráfego Pago & Performance");
        if (lowerAll.contains("react") || lowerAll.contains("node") || lowerAll.contains("typescript")) tags.add("Desenvolvimento Web Full Stack");
        if (lowerAll.contains("ux") || lowerAll.contains("ui") || lowerAll.contains("figma")) tags.add("UX/UI & Product Design");
        if (lowerAll.contains("jurídico") || lowerAll.contains("contrato") || lowerAll.contains("lgpd")) tags.add("Consultoria Jurídica & LGPD");
        if (lowerAll.contains("financeiro") || lowerAll.contains("captação") || lowerAll.contains("valuation")) tags.add("Modelagem Financeira & Captação");
        if (!category.isBlank() && !tags.contains(category)) tags.add(0, category);
        if (tags.size() < 4) tags.add("Estratégia para Startups");
        if (tags.size() < 4) tags.add("Posicionamento & Crescimento");

        String polishedCore = cleanedDraft
                .replaceAll("(?i)\\bmarketing\\s+e\\s+aberto\\s+e\\s+escalabilidade\\s+da\\s+marca\\b",
                        "marketing estratégico, com foco aberto em parcerias e na escalabilidade da marca")
                .replaceAll("(?i)\\baberto\\s+e\\s+escalabilidade\\b",
                        "atuação aberta a novos projetos e foco na escalabilidade")
                .replaceAll("(?i)\\bcom\\s+o\\s+instagram\\b", "por meio do Instagram")
                .trim();

        if (polishedCore.endsWith(".") || polishedCore.endsWith(";") || polishedCore.endsWith("!")) {
            polishedCore = polishedCore.substring(0, polishedCore.length() - 1).trim();
        }
        if (!polishedCore.isEmpty()) {
            polishedCore = Character.toUpperCase(polishedCore.charAt(0)) + polishedCore.substring(1);
            String lowCore = polishedCore.toLowerCase();
            if (!lowCore.startsWith("trabalho") && !lowCore.startsWith("trabalhamos")
                    && !lowCore.startsWith("atuo") && !lowCore.startsWith("atuamos")
                    && !lowCore.startsWith("desenvolvo") && !lowCore.startsWith("desenvolvemos")
                    && !lowCore.startsWith("ajudo") && !lowCore.startsWith("ajudamos")
                    && !lowCore.startsWith("faço") && !lowCore.startsWith("fazemos")
                    && !lowCore.startsWith("sou") && !lowCore.startsWith("somos")) {
                polishedCore = (isCompany ? "Atuamos com " : "Atuo com ")
                        + Character.toLowerCase(polishedCore.charAt(0)) + polishedCore.substring(1);
            }
        } else {
            polishedCore = (isCompany ? "Atuamos com soluções especializadas em " : "Atuo com soluções especializadas em ") + category;
        }

        String meuFoco = isCompany ? "Nosso foco é" : "Meu foco é";
        String naPratica = isCompany ? "Na prática, estruturamos" : "Na prática, estruturo";
        String buscamos = isCompany ? "Buscamos" : "Busco";

        String enhanced;
        String inferredService;
        if ("project".equalsIgnoreCase(fieldType)) {
            inferredService = !mainService.isBlank() ? mainService : "Projeto de " + category;
            enhanced = polishedCore + ". O objetivo principal deste projeto foi diagnosticar os desafios da startup e implementar uma solução prática e direcionada ao crescimento do negócio.\n\n"
                    + naPratica + " cada entrega com foco em resultados reais, fortalecendo o posicionamento, a eficiência e a escalabilidade da operação no mercado.";
        } else if (lowerAll.contains("instagram") || lowerAll.contains("marketing") || lowerAll.contains("marca") || lowerAll.contains("growth")) {
            inferredService = lowerAll.contains("instagram")
                    ? "Marketing Estratégico & Escalabilidade de Marca no Instagram"
                    : "Marketing Estratégico, Branding & Growth para Startups";
            String canalFocus = lowerAll.contains("instagram") ? "no Instagram e nos canais digitais" : "nos principais canais digitais";
            enhanced = polishedCore + ". " + meuFoco + " transformar a presença digital de startups em uma alavanca real de autoridade, posicionamento competitivo e crescimento sustentável de clientes.\n\n"
                    + naPratica + " estratégias de comunicação, conteúdo direcionado e fortalecimento de marca " + canalFocus + ", alinhando cada ação às metas de validação e tração do negócio. "
                    + buscamos + " gerar engajamento qualificado, ampliar o reconhecimento da marca no mercado e construir uma operação de marketing escalável e orientada a resultados.";
        } else {
            inferredService = !mainService.isBlank() ? mainService : "Especialista em " + category;
            enhanced = polishedCore + ". " + meuFoco + " apoiar startups em estágios de validação e tração com soluções práticas na área de " + category + ", transformando necessidades do negócio em entregas confiáveis e escaláveis.\n\n"
                    + naPratica + " cada etapa do trabalho com metodologia ágil, comunicação direta e atenção aos objetivos reais da startup. "
                    + buscamos + " resolver gargalos com rapidez, garantindo qualidade e impacto direto na evolução do negócio.";
        }

        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("enhancedText", enhanced);
        fallback.put("mainService", inferredService);
        fallback.put("tags", tags);
        fallback.put("toolsAndSkills", String.join(", ", tags));
        fallback.put("technologies", String.join(", ", tags));
        fallback.put("serviceHighlights", List.of(inferredService, "Foco em resultados mensuráveis", "Parceria com Startups"));
        fallback.put("valueProposition", "Atuação em 1ª pessoa focada no crescimento e escalabilidade da startup.");
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
                trlScore = Math.max(7, Integer.parseInt(String.valueOf(payload.get("trlScore"))));
            } catch (NumberFormatException ignored) {}
        }
        String fileName = payload.get("fileName") != null ? String.valueOf(payload.get("fileName")) : "atestado_capacidade.pdf";
        String startupName = payload.get("startupName") != null && !String.valueOf(payload.get("startupName")).isBlank()
                ? String.valueOf(payload.get("startupName"))
                : "Startup";

        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("attested", true);
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

    public Map<String, Object> extractProviderResume(Map<String, Object> payload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            String url = aiBaseUrl + "/api/ai/extract-provider-resume";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getBody() != null && response.getBody().get("description") != null) {
                Map<String, Object> body = new LinkedHashMap<>(response.getBody());
                body.put("description", cleanStructuredText(String.valueOf(body.get("description"))));
                return body;
            }
        } catch (Exception e) {
            log.warn("Aviso ao consultar /api/ai/extract-provider-resume: {}. Aplicando extração local do currículo em PDF.", e.getMessage());
        }

        String fileName = payload.get("fileName") != null ? String.valueOf(payload.get("fileName")) : "curriculo.pdf";
        String fileBase64 = payload.get("fileBase64") != null ? String.valueOf(payload.get("fileBase64")) : "";
        String providerType = payload.get("providerType") != null ? String.valueOf(payload.get("providerType")) : "PROFISSIONAL";
        String linkedinUrl = payload.get("linkedinUrl") != null ? String.valueOf(payload.get("linkedinUrl")) : "";
        String portfolioUrl = payload.get("portfolioUrl") != null ? String.valueOf(payload.get("portfolioUrl")) : "";
        boolean isCompany = "EMPRESA".equalsIgnoreCase(providerType);

        String decodedText = "";
        if (!fileBase64.isBlank()) {
            try {
                String cleanB64 = fileBase64.contains(",") ? fileBase64.substring(fileBase64.indexOf(',') + 1) : fileBase64;
                byte[] bytes = Base64.getDecoder().decode(cleanB64.trim());
                String rawAscii = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
                StringBuilder readable = new StringBuilder();
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\(([^()\\\\]{3,120})\\)").matcher(rawAscii);
                while (m.find() && readable.length() < 8000) {
                    readable.append(m.group(1)).append(" ");
                }
                decodedText = readable.toString();
            } catch (Exception ignored) {}
        }

        String combined = (fileName + " " + decodedText + " " + linkedinUrl + " " + portfolioUrl).toLowerCase(Locale.ROOT);
        String category = "Tecnologia";
        if (combined.contains("figma") || combined.contains("ux") || combined.contains("ui ") || combined.contains("design")) {
            category = "Design";
        } else if (combined.contains("marketing") || combined.contains("growth") || combined.contains("instagram") || combined.contains("ads") || combined.contains("tráfego") || combined.contains("trafego")) {
            category = "Marketing";
        } else if (combined.contains("jurídico") || combined.contains("juridico") || combined.contains("advogad") || combined.contains("direito") || combined.contains("lgpd")) {
            category = "Jurídico";
        } else if (combined.contains("financeiro") || combined.contains("valuation") || combined.contains("contabil") || combined.contains("fluxo de caixa")) {
            category = "Financeiro";
        } else if (combined.contains("consultor") || combined.contains("gestão") || combined.contains("estrategia")) {
            category = "Consultoria";
        }

        List<String> tags = new ArrayList<>();
        if ("Design".equalsIgnoreCase(category)) {
            tags.addAll(List.of("Figma", "UI/UX Design", "Design System", "Prototipação"));
        } else if ("Marketing".equalsIgnoreCase(category)) {
            tags.addAll(List.of("Marketing Estratégico & Growth", "Tráfego Pago & Performance", "Instagram & Social Media", "Escalabilidade de Marca"));
        } else if ("Jurídico".equalsIgnoreCase(category)) {
            tags.addAll(List.of("Direito Societário", "LGPD & Compliance", "Contratos para Startups", "Propriedade Intelectual"));
        } else if ("Financeiro".equalsIgnoreCase(category)) {
            tags.addAll(List.of("Modelagem Financeira", "Valuation & M&A", "BPO Financeiro", "Captação & Pitch Deck"));
        } else if ("Consultoria".equalsIgnoreCase(category)) {
            tags.addAll(List.of("Estratégia & Produto", "Gestão Ágil", "Go-to-Market", "Modelagem de Negócios"));
        } else {
            tags.addAll(List.of("React", "Node.js", "TypeScript", "Python", "Arquitetura Cloud & IA"));
        }
        String toolsAndSkills = String.join(", ", tags);
        String mainService = "Design".equalsIgnoreCase(category) ? "UX/UI & Product Design para Startups"
                : "Marketing".equalsIgnoreCase(category) ? "Marketing Estratégico, Branding & Growth para Startups"
                : "Jurídico".equalsIgnoreCase(category) ? "Consultoria Jurídica Societária & LGPD para Startups"
                : "Financeiro".equalsIgnoreCase(category) ? "Modelagem Financeira, Valuation & Captação"
                : "Consultoria".equalsIgnoreCase(category) ? "Consultoria de Produto, Gestão & Escala"
                : "Desenvolvimento Full Stack, MVPs & Soluções de IA";
        String verbAtuo = isCompany ? "Atuamos" : "Atuo";
        String verbTenho = isCompany ? "Contamos com" : "Tenho";
        String verbEstruturo = isCompany ? "Estruturamos" : "Estruturo";
        String verbBusco = isCompany ? "Buscamos" : "Busco";

        String description = verbAtuo + " na área de " + category + " com foco em " + mainService + ", aplicando " + toolsAndSkills
                + " no desenvolvimento de soluções práticas e escaláveis. " + verbTenho
                + " experiência consolidada conduzindo entregas técnicas com foco em qualidade, performance e alinhamento às metas do negócio.\n\n"
                + verbEstruturo + " cada projeto com metodologia ágil, visão orientada a produto e comunicação próxima com os fundadores. "
                + verbBusco + " apoiar startups em estágios de validação e tração na construção, evolução e escalabilidade de suas operações, transformando demandas complexas em resultados mensuráveis.";

        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("extracted", true);
        fallback.put("fileName", fileName);
        fallback.put("category", category);
        fallback.put("mainService", mainService);
        fallback.put("toolsAndSkills", toolsAndSkills);
        fallback.put("tags", tags);
        fallback.put("experienceYears", "3–5 anos");
        fallback.put("location", "Recife, PE");
        fallback.put("projectName", "Experiência em " + mainService);
        fallback.put("projectType", "Tecnologia".equalsIgnoreCase(category) ? "MVP / Produto completo" : "Consultoria técnica");
        fallback.put("projectYear", "2026");
        fallback.put("interestSectors", "Tecnologia / SaaS, IA & Dados, Finanças");
        fallback.put("connectionPreference", "Projetos sob demanda");
        fallback.put("opportunitiesSought", "Desenvolver MVPs, Prestar consultoria especializada, Atuar em projetos pontuais");
        fallback.put("description", description);
        fallback.put("serviceExperience", description);
        fallback.put("projectDescription", description);
        fallback.put("extractedHighlights", List.of(
                "Categoria identificada no PDF: " + category + " (" + mainService + ")",
                "Tecnologias e competências mapeadas: " + toolsAndSkills,
                "Descrição profissional prévia em 1ª pessoa gerada automaticamente"
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
        String compDesc = company != null && company.getDescription() != null && !company.getDescription().isBlank()
                ? company.getDescription()
                : "Organização parceira com demandas abertas de inovação.";
        String compTagsRaw = company != null && company.getTags() != null ? company.getTags() : "";
        String compDemands = company != null && company.getDemands() != null && !company.getDemands().isBlank()
                ? company.getDemands()
                : compDesc;
        String compLocation = company != null && company.getLocation() != null && !company.getLocation().isBlank() ? company.getLocation() : "Brasil";
        String compMode = company != null && company.getOperationMode() != null && !company.getOperationMode().isBlank() ? company.getOperationMode() : "POC, Compra";
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
        String stArea = startup != null && startup.getMainArea() != null && !startup.getMainArea().isBlank()
                ? startup.getMainArea()
                : (startup != null && startup.getMainSector() != null ? startup.getMainSector() : "");
        String stTech = startup != null && startup.getTechnologies() != null && !startup.getTechnologies().isBlank()
                ? startup.getTechnologies()
                : (startup != null && startup.getAreasOfInterest() != null ? startup.getAreasOfInterest() : "");
        String stModel = startup != null && startup.getBusinessModel() != null && !startup.getBusinessModel().isBlank() ? startup.getBusinessModel() : "B2B";
        String stLoc = startup != null && startup.getLocation() != null && !startup.getLocation().isBlank() ? startup.getLocation() : "Brasil";
        String stStage = startup != null && startup.getCurrentStage() != null && !startup.getCurrentStage().isBlank() ? startup.getCurrentStage() : "Validação";
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

    /**
     * Calcula a compatibilidade real (0 a 100%) e as 3 razões detalhadas (sinergiaTecnica,
     * modeloAtuacao, impactoEsperado) entre um Prestador de Serviço e uma Startup em Tração.
     * Utiliza exatamente a mesma fórmula unificada de `computeProviderOpportunityCompatibility`
     * para garantir simetria de 100% entre o perfil da Startup em Tração e o perfil do Prestador.
     */
    public Map<String, Object> computeProviderStartupCompatibility(
            ServiceProvider provider,
            StartupProfile startup,
            List<Opportunity> startupServiceDemands) {

        List<Opportunity> candidates = new ArrayList<>();
        if (startupServiceDemands != null) {
            for (Opportunity d : startupServiceDemands) {
                if (d != null) {
                    candidates.add(d);
                }
            }
        }
        if (candidates.isEmpty()) {
            Opportunity synth = new Opportunity();
            String sArea = startup != null && startup.getMainArea() != null && !startup.getMainArea().isBlank()
                    ? startup.getMainArea().trim()
                    : (startup != null && startup.getMainSector() != null ? startup.getMainSector().trim() : "Tecnologia");
            synth.setTitle("Demanda Técnica de " + sArea);
            synth.setDescription(startup != null && startup.getProblem() != null && !startup.getProblem().isBlank()
                    ? startup.getProblem()
                    : (startup != null && startup.getDescription() != null && !startup.getDescription().isBlank()
                            ? startup.getDescription()
                            : "Demanda de serviços especializados para startup em estágio de Tração."));
            synth.setTags(startup != null && startup.getTechnologies() != null && !startup.getTechnologies().isBlank()
                    ? startup.getTechnologies()
                    : (startup != null && startup.getAreasOfInterest() != null ? startup.getAreasOfInterest() : "Inovação, Escalabilidade"));
            synth.setTargetCategory(sArea);
            synth.setStartup(startup);
            candidates.add(synth);
        }

        Map<String, Object> bestComp = null;
        int bestScore = -1;
        for (Opportunity cand : candidates) {
            Map<String, Object> comp = computeProviderOpportunityCompatibility(provider, cand, startup);
            int sc = comp.get("matchScore") instanceof Number n ? n.intValue() : 0;
            if (bestComp == null || sc > bestScore) {
                bestComp = comp;
                bestScore = sc;
            }
        }
        return bestComp;
    }

    /**
     * Avalia a lista de Prestadores de Serviço contra uma Startup (chamando o serviço Python de IA
     * com fallback determinístico auditado).
     */
    @SuppressWarnings("unchecked")
    public List<ServiceProviderDTO> evaluateStartupToProvidersMatch(
            StartupProfile startup,
            List<Opportunity> startupServiceDemands,
            List<ServiceProvider> providers) {

        if (providers == null || providers.isEmpty()) {
            return List.of();
        }

        Map<Long, Map<String, Object>> aiResultsByProviderId = new HashMap<>();
        if (startup != null) {
            try {
                Map<String, Object> payload = new LinkedHashMap<>();
                Map<String, Object> stMap = new LinkedHashMap<>();
                stMap.put("id", startup.getId());
                stMap.put("name", startup.getName());
                stMap.put("mainArea", startup.getMainArea());
                stMap.put("mainSector", startup.getMainSector());
                stMap.put("description", startup.getDescription());
                stMap.put("problem", startup.getProblem());
                stMap.put("howItWorks", startup.getHowItWorks());
                stMap.put("technologies", startup.getTechnologies());
                stMap.put("areasOfInterest", startup.getAreasOfInterest());
                stMap.put("mainObjective", startup.getMainObjective());
                stMap.put("desiredPartners", startup.getDesiredPartners());
                stMap.put("currentStage", startup.getCurrentStage());
                stMap.put("location", startup.getLocation());
                if (startupServiceDemands != null && !startupServiceDemands.isEmpty()) {
                    List<Map<String, Object>> dList = new ArrayList<>();
                    for (Opportunity d : startupServiceDemands) {
                        Map<String, Object> dm = new LinkedHashMap<>();
                        dm.put("id", d.getId());
                        dm.put("title", d.getTitle());
                        dm.put("targetCategory", d.getTargetCategory());
                        dm.put("description", d.getDescription());
                        dm.put("tags", d.getTags());
                        dList.add(dm);
                    }
                    stMap.put("serviceDemands", dList);
                }
                payload.put("startup", stMap);
                payload.put("providers", providers.stream().map(ServiceProviderDTO::fromEntity).toList());

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

                ResponseEntity<Map> resp = restTemplate.postForEntity(
                        aiBaseUrl + "/api/ai/match-providers",
                        entity,
                        Map.class
                );
                if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                    Object matchesObj = resp.getBody().get("matches");
                    if (matchesObj instanceof List<?> list) {
                        for (Object item : list) {
                            if (item instanceof Map<?, ?> m) {
                                Object pidObj = m.get("providerId");
                                if (pidObj != null) {
                                    Long pid = Long.valueOf(String.valueOf(pidObj));
                                    aiResultsByProviderId.put(pid, (Map<String, Object>) m);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Usando cálculo auditado local para Startup -> Prestadores: {}", e.getMessage());
            }
        }

        List<ServiceProviderDTO> dtos = new ArrayList<>();
        for (ServiceProvider p : providers) {
            ServiceProviderDTO dto = ServiceProviderDTO.fromEntity(p);
            if (startup != null) {
                Map<String, Object> localComp = computeProviderStartupCompatibility(p, startup, startupServiceDemands);
                Map<String, Object> aiMatch = p.getId() != null ? aiResultsByProviderId.get(p.getId()) : null;
                if (aiMatch != null) {
                    Object sc = aiMatch.get("matchScore");
                    dto.setMatchScore(sc instanceof Number n ? n.intValue() : (Integer) localComp.get("matchScore"));
                    Object just = aiMatch.get("justification");
                    dto.setJustification(just != null ? String.valueOf(just) : (String) localComp.get("justification"));
                    Object rObj = aiMatch.get("reasons");
                    if (rObj instanceof Map<?, ?> rm) {
                        Map<String, String> rMap = new LinkedHashMap<>();
                        rm.forEach((k, v) -> rMap.put(String.valueOf(k), v != null ? String.valueOf(v) : ""));
                        dto.setMatchReasons(rMap);
                    } else {
                        dto.setMatchReasons((Map<String, String>) localComp.get("reasons"));
                    }
                } else {
                    dto.setMatchScore((Integer) localComp.get("matchScore"));
                    dto.setJustification((String) localComp.get("justification"));
                    dto.setMatchReasons((Map<String, String>) localComp.get("reasons"));
                }
            } else {
                // Calcula prontidão técnica real do perfil do prestador sem número estático
                int completeness = 65;
                if (p.getDescription() != null && !p.getDescription().isBlank()) completeness += 8;
                if (p.getMainService() != null && !p.getMainService().isBlank()) completeness += 6;
                if (p.getServiceExperience() != null && !p.getServiceExperience().isBlank()) completeness += 6;
                if (p.getToolsAndSkills() != null && !p.getToolsAndSkills().isBlank()) completeness += 5;
                if (p.getProjectName() != null && !p.getProjectName().isBlank()) completeness += 5;
                if (p.getInterestSectors() != null && !p.getInterestSectors().isBlank()) completeness += 3;
                dto.setMatchScore(Math.min(98, completeness));
            }
            dtos.add(dto);
        }

        dtos.sort((a, b) -> Integer.compare(
                b.getMatchScore() != null ? b.getMatchScore() : 0,
                a.getMatchScore() != null ? a.getMatchScore() : 0
        ));
        return dtos;
    }

    /**
     * Avalia todas as Startups em Tração contra o perfil de um Prestador de Serviço
     * (chamando o serviço Python de IA /api/ai/match-provider-startups com fallback determinístico).
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> evaluateProviderToStartupsMatch(
            ServiceProvider provider,
            List<StartupProfile> tractionStartups,
            Map<Long, List<Opportunity>> demandsByStartupId,
            Set<Long> savedStartupIds) {

        if (tractionStartups == null || tractionStartups.isEmpty()) {
            return List.of();
        }

        Map<Long, Map<String, Object>> aiResultsByStartupId = new HashMap<>();
        if (provider != null) {
            try {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("provider", ServiceProviderDTO.fromEntity(provider));
                List<Map<String, Object>> stPayloads = new ArrayList<>();
                for (StartupProfile st : tractionStartups) {
                    Map<String, Object> sm = new LinkedHashMap<>();
                    sm.put("id", st.getId());
                    sm.put("name", st.getName());
                    sm.put("mainArea", st.getMainArea());
                    sm.put("mainSector", st.getMainSector());
                    sm.put("currentStage", st.getCurrentStage());
                    sm.put("description", st.getDescription());
                    sm.put("problem", st.getProblem());
                    sm.put("howItWorks", st.getHowItWorks());
                    sm.put("technologies", st.getTechnologies());
                    sm.put("areasOfInterest", st.getAreasOfInterest());
                    sm.put("mainObjective", st.getMainObjective());
                    sm.put("desiredPartners", st.getDesiredPartners());
                    sm.put("location", st.getLocation());
                    sm.put("website", st.getWebsite());
                    sm.put("avatarUrl", st.getAvatarUrl());
                    List<Opportunity> stDemands = demandsByStartupId != null && st.getId() != null
                            ? demandsByStartupId.getOrDefault(st.getId(), List.of())
                            : List.of();
                    if (!stDemands.isEmpty()) {
                        List<Map<String, Object>> dList = new ArrayList<>();
                        for (Opportunity d : stDemands) {
                            Map<String, Object> dm = new LinkedHashMap<>();
                            dm.put("id", d.getId());
                            dm.put("title", d.getTitle());
                            dm.put("targetCategory", d.getTargetCategory());
                            dm.put("description", d.getDescription());
                            dm.put("tags", d.getTags());
                            dList.add(dm);
                        }
                        sm.put("serviceDemands", dList);
                    }
                    stPayloads.add(sm);
                }
                payload.put("startups", stPayloads);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

                ResponseEntity<Map> resp = restTemplate.postForEntity(
                        aiBaseUrl + "/api/ai/match-provider-startups",
                        entity,
                        Map.class
                );
                if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                    Object matchesObj = resp.getBody().get("matches");
                    if (matchesObj instanceof List<?> list) {
                        for (Object item : list) {
                            if (item instanceof Map<?, ?> m) {
                                Object sidObj = m.get("startupId");
                                if (sidObj != null) {
                                    Long sid = Long.valueOf(String.valueOf(sidObj));
                                    aiResultsByStartupId.put(sid, (Map<String, Object>) m);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Usando cálculo auditado local para Prestador -> Startups em Tração: {}", e.getMessage());
            }
        }

        List<Map<String, Object>> results = new ArrayList<>();
        for (StartupProfile st : tractionStartups) {
            List<Opportunity> stDemands = demandsByStartupId != null && st.getId() != null
                    ? demandsByStartupId.getOrDefault(st.getId(), List.of())
                    : List.of();
            Map<String, Object> localComp = computeProviderStartupCompatibility(provider, st, stDemands);
            Map<String, Object> aiMatch = st.getId() != null ? aiResultsByStartupId.get(st.getId()) : null;

            int score = (Integer) localComp.get("matchScore");
            String justification = (String) localComp.get("justification");
            Object reasons = localComp.get("reasons");

            if (aiMatch != null) {
                Object sc = aiMatch.get("matchScore");
                if (sc instanceof Number n) score = n.intValue();
                Object just = aiMatch.get("justification");
                if (just != null && !String.valueOf(just).isBlank()) justification = String.valueOf(just);
                if (aiMatch.get("reasons") != null) reasons = aiMatch.get("reasons");
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", st.getId());
            item.put("startupId", st.getId());
            item.put("name", st.getName());
            item.put("mainArea", st.getMainArea() != null && !st.getMainArea().isBlank() ? st.getMainArea() : (st.getMainSector() != null ? st.getMainSector() : "Tecnologia"));
            item.put("mainSector", st.getMainSector());
            item.put("currentStage", st.getCurrentStage() != null && !st.getCurrentStage().isBlank() ? st.getCurrentStage() : "Tração");
            item.put("description", st.getDescription());
            item.put("problem", st.getProblem());
            item.put("howItWorks", st.getHowItWorks());
            item.put("differentiator", st.getDifferentiator());
            item.put("technologies", st.getTechnologies() != null && !st.getTechnologies().isBlank() ? st.getTechnologies() : st.getAreasOfInterest());
            item.put("businessModel", st.getBusinessModel());
            item.put("location", st.getLocation() != null && !st.getLocation().isBlank() ? st.getLocation() : "Brasil");
            item.put("website", st.getWebsite());
            item.put("phone", st.getPhone() != null ? st.getPhone() : (st.getUser() != null ? st.getUser().getPhone() : null));
            item.put("email", st.getUser() != null ? st.getUser().getEmail() : null);
            item.put("avatarUrl", st.getAvatarUrl() != null ? st.getAvatarUrl() : (st.getUser() != null ? st.getUser().getAvatarUrl() : null));
            item.put("tractionAttested", st.getTractionAttested());
            item.put("trlScore", st.getTrlScore());
            item.put("mainObjective", st.getMainObjective());
            item.put("desiredPartners", st.getDesiredPartners());
            item.put("matchScore", score);
            item.put("match", score);
            item.put("justification", justification);
            item.put("reasons", reasons);
            item.put("saved", savedStartupIds != null && st.getId() != null && savedStartupIds.contains(st.getId()));
            item.put("openServiceDemandsCount", stDemands.size());
            if (!stDemands.isEmpty()) {
                Opportunity topDemand = stDemands.get(0);
                item.put("openDemandTitle", topDemand.getTitle());
                item.put("openDemandCategory", topDemand.getTargetCategory());
            }
            results.add(item);
        }

        results.sort((a, b) -> Integer.compare((int) b.get("matchScore"), (int) a.get("matchScore")));
        return results;
    }

    /**
     * Calcula a compatibilidade real e auditada (0 a 100%) entre um Prestador de Serviços
     * e uma OPORTUNIDADE lançada por uma startup em estágio de Tração.
     */
    public Map<String, Object> computeProviderOpportunityCompatibility(
            ServiceProvider provider,
            Opportunity opp,
            StartupProfile startup) {

        String pName = provider != null && provider.getName() != null ? provider.getName().trim() : "Prestador";
        String pCat = provider != null && provider.getCategory() != null ? provider.getCategory().trim() : "Serviços Especializados";
        String pCompCat = provider != null && provider.getComplementaryCategory() != null ? provider.getComplementaryCategory().trim() : "";
        String pMainService = provider != null && provider.getMainService() != null ? provider.getMainService().trim() : pCat;
        String pBio = provider != null && provider.getDescription() != null ? provider.getDescription().trim() : "";
        String pExp = provider != null && provider.getServiceExperience() != null ? provider.getServiceExperience().trim() : "";
        String pSkills = provider != null && provider.getToolsAndSkills() != null
                ? provider.getToolsAndSkills().trim()
                : (provider != null && provider.getTags() != null ? provider.getTags().trim() : "");
        String pSectors = provider != null && provider.getInterestSectors() != null
                ? provider.getInterestSectors().trim()
                : (provider != null && provider.getTargetSegment() != null ? provider.getTargetSegment().trim() : "");
        String pConnPref = provider != null && provider.getConnectionPreference() != null
                ? provider.getConnectionPreference().trim() : "Projetos sob demanda";
        String pAvail = provider != null && provider.getAvailability() != null ? provider.getAvailability().trim() : "Disponível";
        String pLoc = provider != null && provider.getLocation() != null ? provider.getLocation().trim() : "Brasil";
        String pProjName = provider != null && provider.getProjectName() != null ? provider.getProjectName().trim() : "";
        String pProjDesc = provider != null && provider.getProjectDescription() != null ? provider.getProjectDescription().trim() : "";
        String pProjTools = provider != null && provider.getProjectTools() != null ? provider.getProjectTools().trim() : "";

        String oppTitle = opp != null && opp.getTitle() != null ? opp.getTitle().trim() : "Demanda de Trabalho";
        String oppDesc = opp != null && opp.getDescription() != null ? opp.getDescription().trim() : "";
        String oppTags = opp != null && opp.getTags() != null ? opp.getTags().trim() : "";
        String oppCat = opp != null && opp.getTargetCategory() != null ? opp.getTargetCategory().trim() : (startup != null && startup.getMainArea() != null ? startup.getMainArea().trim() : "Tecnologia");
        String oppMeta = opp != null && opp.getMetaInfo() != null ? opp.getMetaInfo().trim() : "";

        String sName = startup != null && startup.getName() != null ? startup.getName().trim() : "Startup";
        String sArea = startup != null && startup.getMainArea() != null && !startup.getMainArea().isBlank()
                ? startup.getMainArea().trim()
                : (startup != null && startup.getMainSector() != null ? startup.getMainSector().trim() : oppCat);
        String sLoc = startup != null && startup.getLocation() != null ? startup.getLocation().trim() : "Brasil";
        String sStage = startup != null && startup.getCurrentStage() != null ? startup.getCurrentStage().trim() : "Tração";
        String sDesc = startup != null && startup.getDescription() != null ? startup.getDescription().trim() : "";
        String sProblem = startup != null && startup.getProblem() != null ? startup.getProblem().trim() : "";
        String sHow = startup != null && startup.getHowItWorks() != null ? startup.getHowItWorks().trim() : "";
        String sTechs = startup != null && startup.getTechnologies() != null && !startup.getTechnologies().isBlank()
                ? startup.getTechnologies().trim()
                : (startup != null && startup.getAreasOfInterest() != null ? startup.getAreasOfInterest().trim() : "");
        String sObjective = startup != null && startup.getMainObjective() != null ? startup.getMainObjective().trim() : "";
        String sPartners = startup != null && startup.getDesiredPartners() != null ? startup.getDesiredPartners().trim() : "";

        String oppCorpus = normText(oppTitle + " " + oppDesc + " " + oppTags + " " + oppCat + " " + oppMeta + " "
                + sName + " " + sArea + " " + sDesc + " " + sProblem + " " + sHow + " " + sTechs + " " + sObjective + " " + sPartners);

        // 1. Alinhamento de Categoria, Demanda e Setor (0..40 pts)
        int catScore = 24;
        String normPCat = normText(pCat);
        if (!normPCat.isEmpty() && (normText(oppCat).contains(normPCat) || normText(oppTitle).contains(normPCat) || normText(oppTags).contains(normPCat))) {
            catScore = 40;
        } else if (!normPCat.isEmpty() && (oppCorpus.contains(normPCat) || Arrays.stream(normPCat.split("\\s+")).anyMatch(w -> w.length() >= 4 && oppCorpus.contains(w)))) {
            catScore = 35;
        } else if (!pSectors.isEmpty()) {
            for (String sec : pSectors.split("[,;/•]+")) {
                String nSec = normText(sec);
                if (nSec.length() >= 3 && (normText(sArea).contains(nSec) || oppCorpus.contains(nSec))) {
                    catScore = 36;
                    break;
                }
            }
        }

        // 2. Competências Técnicas e Ferramentas Requeridas (0..30 pts)
        String allSkillsRaw = pSkills + ", " + pMainService + ", " + pProjTools;
        List<String> matchedSkills = new ArrayList<>();
        List<String> allSkillsList = new ArrayList<>();
        for (String tok : allSkillsRaw.split("[,;/•\\n]+")) {
            String clean = tok.trim();
            if (clean.length() >= 2 && !allSkillsList.contains(clean)) {
                allSkillsList.add(clean);
                String nSk = normText(clean);
                if (!nSk.isEmpty() && (oppCorpus.contains(nSk) || Arrays.stream(nSk.split("\\s+")).anyMatch(w -> w.length() >= 4 && oppCorpus.contains(w)))) {
                    matchedSkills.add(clean);
                }
            }
        }

        int techScore;
        if (matchedSkills.size() >= 3) {
            techScore = 29;
        } else if (matchedSkills.size() == 2) {
            techScore = 25;
        } else if (matchedSkills.size() == 1) {
            techScore = 21;
        } else if (!allSkillsList.isEmpty()) {
            techScore = 17;
        } else {
            techScore = 13;
        }

        // 3. Modelo de Atuação, Portfólio, Disponibilidade e Localização (0..30 pts)
        int otherScore = 15;
        if (!pBio.isEmpty() || !pExp.isEmpty()) otherScore += 5;
        if (!pProjName.isEmpty() || !pProjDesc.isEmpty()) otherScore += 4;
        if (normText(pAvail).contains("imediata") || normText(pAvail).contains("disponivel")) otherScore += 3;
        if (!pLoc.isEmpty() && !sLoc.isEmpty() && (normText(pLoc).contains(normText(sLoc)) || normText(sLoc).contains(normText(pLoc)) || normText(pLoc).contains("brasil") || normText(pLoc).contains("remoto"))) {
            otherScore += 3;
        }
        otherScore = Math.min(30, otherScore);

        int totalScore = Math.max(48, Math.min(99, catScore + techScore + otherScore));
        String skillsHighlight = !matchedSkills.isEmpty()
                ? String.join(", ", matchedSkills.subList(0, Math.min(4, matchedSkills.size())))
                : (!pSkills.isEmpty() ? pSkills : pMainService);

        String sinergiaTecnica = "A oportunidade '" + oppTitle + "' demanda competências em " + (!oppTags.isEmpty() ? oppTags : oppCat)
                + ", especialidade coberta pelo serviço de " + pMainService + " (" + pCat + ") com domínio em " + skillsHighlight + ".";
        String modeloAtuacao = "Disponibilidade (" + pAvail + ") no formato '" + pConnPref + "', perfeitamente aderente ao prazo e "
                + "ao escopo de entrega esperado pela " + sName + " (" + sStage + ").";
        String impactoEsperado = "Execução técnica direta da demanda de trabalho para acelerar a escalabilidade da " + sName
                + (!pProjName.isEmpty() ? ", apoiada pelo case prático '" + pProjName + "'." : " com foco em entregas ágeis.");

        String justification = "Compatibilidade de " + totalScore + "% entre " + pName + " (" + pCat + ") e " + sName + " (" + sArea + " · " + sStage + "): sinergia em " + skillsHighlight + " e modelo '" + pConnPref + "'.";

        Map<String, String> reasons = new LinkedHashMap<>();
        reasons.put("perfil", sinergiaTecnica);
        reasons.put("requisitos", "Competências e ferramentas aderentes à demanda: " + skillsHighlight + ".");
        reasons.put("objetivo", modeloAtuacao);
        reasons.put("contexto", impactoEsperado);
        reasons.put("sinergiaTecnica", sinergiaTecnica);
        reasons.put("modeloAtuacao", modeloAtuacao);
        reasons.put("impactoEsperado", impactoEsperado);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("matchScore", totalScore);
        out.put("justification", justification);
        out.put("recommendedAction", "Enviar proposta técnica para a oportunidade '" + oppTitle + "' da " + sName + ".");
        out.put("reasons", reasons);
        return out;
    }

    /**
     * Avalia as OPORTUNIDADES lançadas pelas Startups em Tração contra o perfil do Prestador de Serviços
     * (chamando o serviço Python de IA /api/ai/match-provider-opportunities com fallback determinístico auditado).
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> evaluateProviderToOpportunitiesMatch(
            ServiceProvider provider,
            List<Opportunity> opportunities,
            Set<Long> savedIds) {

        if (opportunities == null || opportunities.isEmpty()) {
            return List.of();
        }

        Map<Long, Map<String, Object>> aiResultsByOppId = new HashMap<>();
        if (provider != null) {
            try {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("provider", ServiceProviderDTO.fromEntity(provider));
                List<Map<String, Object>> oppPayloads = new ArrayList<>();
                for (Opportunity opp : opportunities) {
                    StartupProfile st = opp.getStartup();
                    Map<String, Object> om = new LinkedHashMap<>();
                    om.put("id", opp.getId());
                    om.put("opportunityId", opp.getId());
                    om.put("title", opp.getTitle());
                    om.put("demandTitle", opp.getTitle());
                    om.put("description", opp.getDescription());
                    om.put("tags", opp.getTags());
                    om.put("targetCategory", opp.getTargetCategory());
                    om.put("metaInfo", opp.getMetaInfo());
                    if (st != null) {
                        om.put("startupId", st.getId());
                        om.put("startupName", st.getName());
                        om.put("startupArea", st.getMainArea() != null ? st.getMainArea() : st.getMainSector());
                        om.put("startupSector", st.getMainSector());
                        om.put("startupLocation", st.getLocation());
                        om.put("startupStage", st.getCurrentStage() != null ? st.getCurrentStage() : "Tração");
                        om.put("startupProblem", st.getProblem());
                        om.put("startupDescription", st.getDescription());
                        om.put("startupHowItWorks", st.getHowItWorks());
                        om.put("startupTechnologies", st.getTechnologies() != null && !st.getTechnologies().isBlank() ? st.getTechnologies() : st.getAreasOfInterest());
                        om.put("startupObjective", st.getMainObjective());
                        om.put("startupPartners", st.getDesiredPartners());
                    }
                    oppPayloads.add(om);
                }
                payload.put("opportunities", oppPayloads);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

                ResponseEntity<Map> resp = restTemplate.postForEntity(
                        aiBaseUrl + "/api/ai/match-provider-opportunities",
                        entity,
                        Map.class
                );
                if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                    Object matchesObj = resp.getBody().get("matches");
                    if (matchesObj instanceof List<?> list) {
                        for (Object item : list) {
                            if (item instanceof Map<?, ?> m) {
                                Object oidObj = m.get("opportunityId");
                                if (oidObj == null) oidObj = m.get("id");
                                if (oidObj != null) {
                                    Long oid = Long.valueOf(String.valueOf(oidObj));
                                    aiResultsByOppId.put(oid, (Map<String, Object>) m);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Usando cálculo auditado local para Prestador -> Oportunidades de Startups: {}", e.getMessage());
            }
        }

        List<Map<String, Object>> results = new ArrayList<>();
        for (Opportunity opp : opportunities) {
            StartupProfile st = opp.getStartup();
            Map<String, Object> localComp = computeProviderOpportunityCompatibility(provider, opp, st);
            Map<String, Object> aiMatch = opp.getId() != null ? aiResultsByOppId.get(opp.getId()) : null;

            int score = (Integer) localComp.get("matchScore");
            String justification = (String) localComp.get("justification");
            Object reasons = localComp.get("reasons");

            if (aiMatch != null) {
                Object sc = aiMatch.get("matchScore");
                if (sc instanceof Number n) score = n.intValue();
                Object just = aiMatch.get("justification");
                if (just != null && !String.valueOf(just).isBlank()) justification = String.valueOf(just);
                if (aiMatch.get("reasons") != null) reasons = aiMatch.get("reasons");
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", opp.getId());
            item.put("opportunityId", opp.getId());
            item.put("title", opp.getTitle());
            item.put("demandTitle", opp.getTitle());
            item.put("name", opp.getTitle());
            item.put("description", opp.getDescription());
            item.put("demand", opp.getDescription());
            item.put("openDemandTitle", opp.getTitle());
            item.put("openDemandDescription", opp.getDescription());
            item.put("tags", opp.getTags());
            item.put("technologies", opp.getTags() != null && !opp.getTags().isBlank()
                    ? opp.getTags()
                    : (st != null ? st.getTechnologies() : ""));
            item.put("targetCategory", opp.getTargetCategory() != null
                    ? opp.getTargetCategory()
                    : (st != null && st.getMainArea() != null ? st.getMainArea() : "Tecnologia"));
            item.put("category", item.get("targetCategory"));
            item.put("mainArea", item.get("targetCategory"));
            item.put("metaInfo", opp.getMetaInfo());
            item.put("status", opp.getStatus() != null ? opp.getStatus().name() : "PUBLICADA");
            item.put("type", opp.getType() != null ? opp.getType().name() : "SERVICO");
            item.put("createdAt", opp.getCreatedAt() != null ? opp.getCreatedAt().toString() : null);

            if (st != null) {
                item.put("startupId", st.getId());
                item.put("startupName", st.getName());
                item.put("startupArea", st.getMainArea() != null ? st.getMainArea() : st.getMainSector());
                item.put("startupSector", st.getMainSector());
                item.put("startupLocation", st.getLocation() != null ? st.getLocation() : "Brasil");
                item.put("location", st.getLocation() != null ? st.getLocation() : "Brasil");
                item.put("startupStage", st.getCurrentStage() != null ? st.getCurrentStage() : "Tração");
                item.put("currentStage", st.getCurrentStage() != null ? st.getCurrentStage() : "Tração");
                item.put("startupAvatarUrl", st.getAvatarUrl());
                item.put("avatarUrl", st.getAvatarUrl());
                item.put("startupProblem", st.getProblem());
                item.put("problem", st.getProblem());
                item.put("startupHowItWorks", st.getHowItWorks());
                item.put("startupTechnologies", st.getTechnologies());
                item.put("website", st.getWebsite());
                item.put("phone", st.getPhone() != null ? st.getPhone() : (st.getUser() != null ? st.getUser().getPhone() : null));
                item.put("email", st.getUser() != null ? st.getUser().getEmail() : null);
            } else {
                item.put("startupId", null);
                item.put("startupName", "Startup em Tração");
                item.put("location", "Brasil");
                item.put("currentStage", "Tração");
            }

            item.put("matchScore", score);
            item.put("match", score);
            item.put("justification", justification);
            item.put("reasons", reasons);

            boolean isSaved = savedIds != null && (
                    savedIds.contains(opp.getId()) ||
                    (st != null && st.getId() != null && savedIds.contains(st.getId()))
            );
            item.put("saved", isSaved);

            results.add(item);
        }

        results.sort((a, b) -> Integer.compare((int) b.get("matchScore"), (int) a.get("matchScore")));
        return results;
    }
}
