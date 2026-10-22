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
    private final NotificationRepository notificationRepository;

    public DataInitializer(
            UserRepository userRepository,
            StartupProfileRepository startupProfileRepository,
            CompanyRepository companyRepository,
            OpportunityRepository opportunityRepository,
            NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.startupProfileRepository = startupProfileRepository;
        this.companyRepository = companyRepository;
        this.opportunityRepository = opportunityRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void run(String... args) {
        // 1. Garantir usuário de teste admin / 0000
        User adminUser = userRepository.findByEmail("admin").orElseGet(() -> {
            log.info("Criando usuário de teste: email=admin | password=0000");
            User u = new User();
            u.setEmail("admin");
            u.setPassword("0000");
            u.setName("Administrador");
            u.setCnpj("00.000.000/0001-00");
            u.setRole(UserRole.ADMIN);
            return userRepository.save(u);
        });

        startupProfileRepository.findByUserId(adminUser.getId()).orElseGet(() -> {
            StartupProfile profile = new StartupProfile();
            profile.setUser(adminUser);
            profile.setName("Startup Admin");
            profile.setCnpj(adminUser.getCnpj());
            profile.setReceiveAiRecommendations(true);
            return startupProfileRepository.save(profile);
        });

        // 2. Garantir catálogo inicial de oportunidades
        if (opportunityRepository.count() > 0) {
            log.info("Catálogo de oportunidades no MySQL já inicializado ({} oportunidades).", opportunityRepository.count());
            return;
        }

        log.info("Inicializando catálogo de empresas e oportunidades no MySQL para o matchmaking da IA...");

        Company empresaAlpha = new Company();
        empresaAlpha.setName("Empresa Alpha");
        empresaAlpha.setType(CompanyType.EMPRESA);
        empresaAlpha.setDescription("Grande conglomerado em busca de inovação aberta, inteligência operacional e automação de processos.");
        empresaAlpha.setTags("IA, SaaS, B2B, Dados, Automação");
        empresaAlpha = companyRepository.save(empresaAlpha);

        Opportunity edital = new Opportunity(
                null,
                "edital",
                OpportunityType.EDITAL,
                "Edital Inovação Aberta 2026",
                "Apoio financeiro para startups com soluções B2B.",
                "Programa de inovação aberta para startups com soluções em dados, tecnologia aplicada e automação por IA. A oportunidade busca aplicações com potencial de validação em ambiente real e escalabilidade nacional.",
                90,
                "Prazo 15/10/2026 · Brasil · Remoto",
                "IA • Dados • Automação • SaaS B2B",
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
                "Empresa Alpha busca startup para automatizar processos internos e testar inteligência operacional em um ambiente real com possibilidade de contrato corporativo.",
                88,
                "Resposta até sexta · Brasil · Híbrido",
                "IA • Operações • Automação • B2B",
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
                "Evento para conectar startups, corporações e investidores em uma agenda de networking, rodadas de negócios e apresentação de pitches.",
                85,
                "22/10/2026 · Recife · Presencial",
                "Networking • Investimento • B2B • Ecossistema",
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
                "Demanda empresarial por contratação direta de plataforma de análise de dados e indicadores para apoiar decisões operacionais e estratégicas.",
                86,
                "Aberta · Brasil · Remoto",
                "Dados • Analytics • SaaS • Gestão",
                "https://compras.startin.com.br",
                "bg-[#ffe1c3] text-[#975524]",
                OpportunityStatus.PUBLICADA
        );

        opportunityRepository.saveAll(List.of(edital, poc, evento, compra));

        if (notificationRepository.count() == 0) {
            Notification n1 = new Notification(null, "Catálogo de oportunidades ativo", "Novos editais, POCs e demandas corporativas estão disponíveis para análise da IA.", "Agora", "Oportunidades", false);
            notificationRepository.save(n1);
        }

        log.info("Catálogo inicial de oportunidades salvo no MySQL com sucesso!");
    }
}
