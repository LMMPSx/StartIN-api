package com.startin.api.config;

import com.startin.api.model.*;
import com.startin.api.model.enums.*;
import com.startin.api.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final StartupProfileRepository startupProfileRepository;
    private final CompanyRepository companyRepository;
    private final OpportunityRepository opportunityRepository;
    private final MatchResultRepository matchResultRepository;
    private final NotificationRepository notificationRepository;

    public DataInitializer(
            UserRepository userRepository,
            StartupProfileRepository startupProfileRepository,
            CompanyRepository companyRepository,
            OpportunityRepository opportunityRepository,
            MatchResultRepository matchResultRepository,
            NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.startupProfileRepository = startupProfileRepository;
        this.companyRepository = companyRepository;
        this.opportunityRepository = opportunityRepository;
        this.matchResultRepository = matchResultRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Base de dados já inicializada.");
            return;
        }

        log.info("Populando base de dados inicial com dados de exemplo da StartIn...");

        // 1. Criar Usuário Padrão
        User user = new User();
        user.setEmail("eduarda@exemplo.com");
        user.setPassword("123456");
        user.setName("Eduarda");
        user.setCnpj("12.345.678/0001-90");
        user.setRole(UserRole.USER);
        user = userRepository.save(user);

        // 2. Criar Perfil da Startup (Nexa Health)
        StartupProfile profile = new StartupProfile();
        profile.setUser(user);
        profile.setName("Nexa Health");
        profile.setMainArea("Healthtech");
        profile.setDescription("Plataforma de IA para apoiar a gestão e análise de dados em saúde.");
        profile.setLocation("Recife, PE");
        profile.setCnpj("12.345.678/0001-90");
        profile.setWebsite("https://nexahealth.com.br");
        profile.setFoundedYear("2024");

        profile.setProblem("Gestão ineficiente e decisões baseadas em dados fragmentados.");
        profile.setHowItWorks("Plataforma integrada com algoritmos de IA preditiva para hospitais e clínicas.");
        profile.setDifferentiator("Integração em tempo real e compliance regulatório estrito com dados de saúde.");
        profile.setImpact("Aumento de 40% na velocidade do diagnóstico operacional e redução de custos.");
        profile.setTechnologies("IA, Dados, Automação, SaaS B2B");
        profile.setBusinessModel("SaaS B2B");

        profile.setTargetAudience("Empresas de saúde e operações B2B");
        profile.setMainSector("Saúde");
        profile.setOperationRegion("Brasil");
        profile.setState("Pernambuco");
        profile.setCustomerProfile("Médias e grandes empresas");
        profile.setMarketSize("R$ 2 bilhões");
        profile.setCompetitors("Sistemas legados de gestão e prontuários isolados");
        profile.setMainChannel("Vendas consultivas");

        profile.setCurrentStage("Tração");
        profile.setTractionCustomers("12 clientes ativos");
        profile.setProductMaturity("Produto validado");
        profile.setTeamSize("8 pessoas");
        profile.setAnnualRevenue("R$ 240 mil");
        profile.setRecentGrowth("+80% no último ano");
        profile.setInvestmentRaised("R$ 500 mil");

        profile.setMainObjective("Expandir para novos mercados");
        profile.setInvestmentNeed("R$ 1 milhão");
        profile.setExecutionDeadline("12 meses");
        profile.setResourceUse("Produto, equipe e expansão comercial");
        profile.setTwelveMonthGoal("Dobrar a base de clientes");
        profile.setStrategicPriority("Escala comercial");
        profile.setDesiredPartners("Fusões, canais e clientes");

        profile.setAreasOfInterest("IA, Saúde, Dados, Automação");
        profile.setOpportunityTypes("POC, Compra, Editais");
        profile.setReceiveAiRecommendations(true);
        profile = startupProfileRepository.save(profile);

        // 3. Criar Organizações / Empresas
        Company empresaAlpha = new Company();
        empresaAlpha.setName("Empresa Alpha");
        empresaAlpha.setType(CompanyType.EMPRESA);
        empresaAlpha.setDescription("Grande conglomerado em busca de inovação aberta e automação de processos.");
        empresaAlpha.setTags("IA, SaaS, B2B, Dados");
        empresaAlpha = companyRepository.save(empresaAlpha);

        // 4. Criar Oportunidades Fiéis ao Frontend
        Opportunity edital = new Opportunity(
                null,
                "edital",
                OpportunityType.EDITAL,
                "Edital Inovação Aberta 2026",
                "Apoio financeiro para startups com soluções B2B.",
                "Programa de inovação aberta para startups com soluções em dados e automação por IA. A oportunidade busca aplicações com potencial de validação em ambiente real.",
                96,
                "Prazo 15/10/2026 · Brasil · Remoto",
                "setor • tecnologia • objetivo",
                "https://inovastartups.com.br/",
                "bg-[#fff2b8] text-[#795900]",
                OpportunityStatus.PUBLICADA
        );

        Opportunity poc = new Opportunity(
                null,
                "poc",
                OpportunityType.POC,
                "POC — Inteligência Operacional",
                "Empresa procura solução de IA para operação.",
                "Empresa Alpha busca startup para automatizar processos internos e testar inteligência operacional em um ambiente real.",
                94,
                "Resposta até sexta · Brasil · Híbrido",
                "setor • tecnologia • objetivo",
                "https://empresa-alpha.com/poc",
                "bg-[#d9f1e5] text-[#16734d]",
                OpportunityStatus.PUBLICADA
        );
        poc.setCompany(empresaAlpha);

        Opportunity evento = new Opportunity(
                null,
                "evento",
                OpportunityType.EVENTO,
                "Startup Connect Recife",
                "Networking com empresas e ecossistema local.",
                "Evento para conectar startups, corporações e investidores em uma agenda de networking e negócios.",
                89,
                "22/10/2026 · Recife · Presencial",
                "localização • objetivo",
                "https://startupconnect.com.br",
                "bg-[#dff1f5] text-[#1e7080]",
                OpportunityStatus.PUBLICADA
        );

        Opportunity compra = new Opportunity(
                null,
                "compra",
                OpportunityType.COMPRA,
                "Compra — Dados & Analytics",
                "Demanda por plataforma de análise.",
                "Demanda empresarial por plataforma de análise de dados para apoiar decisões operacionais.",
                87,
                "Aberta · Brasil · Remoto",
                "tecnologia • solução",
                "https://compras.startin.com.br",
                "bg-[#ffe1c3] text-[#975524]",
                OpportunityStatus.PUBLICADA
        );

        opportunityRepository.saveAll(List.of(edital, poc, evento, compra));

        // 5. Criar Matches Pré-calculados
        MatchResult matchPoc = new MatchResult(
                null,
                profile,
                poc,
                94,
                "A startup atua no mesmo contexto da demanda e possui histórico de aplicação em análise preditiva.",
                "Submeter proposta de POC detalhando metodologia de teste em ambiente real.",
                "Healthtech · SaaS · B2B - Alinhamento com setor corporativo",
                "IA · Dados · Automação atendem os requisitos técnicos",
                "POC para gestão operacional compatível com solução",
                "Brasil · B2B · validação compatíveis",
                MatchStatus.NOVO
        );

        MatchResult matchEdital = new MatchResult(
                null,
                profile,
                edital,
                96,
                "Edital estratégico de subvenção e investimento direto para soluções de IA aplicada.",
                "Estruturar documentação jurídica e plano de trabalho para envio antes do prazo.",
                "Healthtech e IA aplicada - 100% elegível",
                "Requisitos de maturidade técnica e equipe atendidos",
                "Apoio financeiro alinhado à necessidade de captação de R$ 1 milhão",
                "Nacional · Remoto · Inscrições abertas",
                MatchStatus.NOVO
        );

        matchResultRepository.saveAll(List.of(matchPoc, matchEdital));

        // 6. Criar Notificações
        Notification n1 = new Notification(null, "Novo match encontrado", "Uma oportunidade de POC teve 94% de compatibilidade com sua startup.", "Hoje, 09:42", "Matches", false);
        Notification n2 = new Notification(null, "Novo edital recomendado", "A StartIn encontrou um edital alinhado ao seu perfil de atuação.", "Hoje, 08:15", "Oportunidades", false);
        Notification n3 = new Notification(null, "Evento próximo do seu perfil", "Tech & Innovation Week pode gerar conexões relevantes para sua startup.", "Ontem, 17:30", "Oportunidades", false);
        Notification n4 = new Notification(null, "Seu perfil foi atualizado", "As preferências de matching foram aplicadas às novas recomendações.", "18 set, 14:10", "Sistema", true);

        notificationRepository.saveAll(List.of(n1, n2, n3, n4));

        log.info("Inicialização de dados do StartIn concluída com sucesso!");
    }
}
