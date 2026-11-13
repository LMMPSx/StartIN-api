package com.startin.api.service;

import com.startin.api.dto.opportunity.CreateOpportunityDTO;
import com.startin.api.dto.opportunity.OpportunityDTO;
import com.startin.api.model.Notification;
import com.startin.api.model.Opportunity;
import com.startin.api.model.StartupProfile;
import com.startin.api.model.enums.OpportunityStatus;
import com.startin.api.model.enums.OpportunityType;
import com.startin.api.repository.CompanyRepository;
import com.startin.api.repository.NotificationRepository;
import com.startin.api.repository.OpportunityRepository;
import com.startin.api.repository.StartupProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class OpportunityService {

    private final OpportunityRepository opportunityRepository;
    private final CompanyRepository companyRepository;
    private final StartupProfileRepository startupProfileRepository;
    private final NotificationRepository notificationRepository;
    private volatile boolean portalOpportunitiesSynced = false;

    public OpportunityService(
            OpportunityRepository opportunityRepository,
            CompanyRepository companyRepository,
            StartupProfileRepository startupProfileRepository,
            NotificationRepository notificationRepository) {
        this.opportunityRepository = opportunityRepository;
        this.companyRepository = companyRepository;
        this.startupProfileRepository = startupProfileRepository;
        this.notificationRepository = notificationRepository;
    }

    public List<OpportunityDTO> listOpportunities(String type, String query) {
        return listOpportunities(type, query, null, null);
    }

    public List<OpportunityDTO> listOpportunities(String type, String query, Long companyId) {
        return listOpportunities(type, query, companyId, null);
    }

    @Transactional
    public List<OpportunityDTO> listOpportunities(String type, String query, Long companyId, Long startupId) {
        if (startupId == null && companyId == null) {
            ensurePortalOpportunitiesSynced();
        }
        List<Opportunity> list;
        boolean hasQuery = query != null && !query.isBlank();
        boolean hasType = type != null && !type.isBlank() && !"Todos".equalsIgnoreCase(type);

        if (startupId != null) {
            list = opportunityRepository.findByStartupIdOrderByIdDesc(startupId);
            if (hasType) {
                OpportunityType oppType = OpportunityType.fromString(type);
                list = list.stream().filter(o -> o.getType() == oppType).toList();
            }
            if (hasQuery) {
                String q = query.trim().toLowerCase();
                list = list.stream().filter(o ->
                        (o.getTitle() != null && o.getTitle().toLowerCase().contains(q)) ||
                        (o.getDescription() != null && o.getDescription().toLowerCase().contains(q)) ||
                        (o.getTags() != null && o.getTags().toLowerCase().contains(q))
                ).toList();
            }
        } else if (companyId != null) {
            list = opportunityRepository.findByCompanyIdOrderByIdDesc(companyId);
            if (hasType) {
                OpportunityType oppType = OpportunityType.fromString(type);
                list = list.stream().filter(o -> o.getType() == oppType).toList();
            }
            if (hasQuery) {
                String q = query.trim().toLowerCase();
                list = list.stream().filter(o ->
                        (o.getTitle() != null && o.getTitle().toLowerCase().contains(q)) ||
                        (o.getDescription() != null && o.getDescription().toLowerCase().contains(q)) ||
                        (o.getTags() != null && o.getTags().toLowerCase().contains(q))
                ).toList();
            }
        } else if (hasType && hasQuery) {
            OpportunityType oppType = OpportunityType.fromString(type);
            list = opportunityRepository.searchByType(oppType, query);
        } else if (hasType) {
            OpportunityType oppType = OpportunityType.fromString(type);
            list = opportunityRepository.findByType(oppType);
        } else if (hasQuery) {
            list = opportunityRepository.search(query);
        } else {
            list = opportunityRepository.findAll();
        }

        // Listagem geral (sem dono informado): demandas de serviço de startups só aparecem
        // quando solicitadas explicitamente pelo tipo SERVICO (ex.: prestadores explorando demandas).
        if (startupId == null && companyId == null) {
            boolean explicitServico = hasType && OpportunityType.fromString(type) == OpportunityType.SERVICO;
            if (!explicitServico) {
                list = list.stream().filter(o -> o.getType() != OpportunityType.SERVICO).toList();
            }
        }

        return list.stream()
                .map(OpportunityDTO::fromEntity)
                .sorted((a, b) -> {
                    boolean aPro = Boolean.TRUE.equals(a.getProMember());
                    boolean bPro = Boolean.TRUE.equals(b.getProMember());
                    if (aPro != bPro) {
                        return aPro ? -1 : 1;
                    }
                    int aMatch = a.getMatch() != null ? a.getMatch() : 0;
                    int bMatch = b.getMatch() != null ? b.getMatch() : 0;
                    if (aMatch != bMatch) {
                        return Integer.compare(bMatch, aMatch);
                    }
                    long aId = a.getId() != null ? a.getId() : 0L;
                    long bId = b.getId() != null ? b.getId() : 0L;
                    return Long.compare(bId, aId);
                })
                .toList();
    }

    public Optional<OpportunityDTO> getById(Long id) {
        return opportunityRepository.findById(id).map(OpportunityDTO::fromEntity);
    }

    public Optional<OpportunityDTO> getByCode(String code) {
        return opportunityRepository.findByCode(code).map(OpportunityDTO::fromEntity);
    }

    @Transactional
    public OpportunityDTO createOpportunity(CreateOpportunityDTO dto) {
        Opportunity opp = new Opportunity();
        opp.setTitle(dto.getTitle());
        opp.setDescription(dto.getDescription());
        opp.setShortDescription(dto.getDescription().length() > 80 ? dto.getDescription().substring(0, 80) + "..." : dto.getDescription());
        opp.setType(OpportunityType.fromString(dto.getType()));
        opp.setTags(dto.getTags());
        opp.setMetaInfo(dto.getMeta() != null && !dto.getMeta().isBlank() ? dto.getMeta() : "Aberta · Brasil · Remoto");
        opp.setExternalUrl(dto.getExternalUrl());
        opp.setStatus(OpportunityStatus.PUBLICADA);
        opp.setMatchScore(90);

        if (dto.getCompanyId() != null) {
            com.startin.api.model.Company comp = companyRepository.findById(dto.getCompanyId()).orElse(null);
            boolean isPro = comp != null && comp.getUser() != null && "PRO".equalsIgnoreCase(comp.getUser().getPlan());
            int maxMonthly = isPro ? 15 : 5;
            java.time.YearMonth currentMonth = java.time.YearMonth.now();
            long monthlyCount = opportunityRepository.findByCompanyIdOrderByIdDesc(dto.getCompanyId()).stream()
                    .filter(o -> o.getCreatedAt() == null || java.time.YearMonth.from(o.getCreatedAt()).equals(currentMonth))
                    .count();
            if (monthlyCount >= maxMonthly) {
                throw new IllegalStateException("Limite mensal atingido: sua conta no Plano " + (isPro ? "Pro" : "Free") + " pode lançar até " + maxMonthly + " oportunidades por mês.");
            }
            if (comp != null) {
                opp.setCompany(comp);
                if (isPro) {
                    opp.setProMember(true);
                }
            }
        }
        if (dto.getStartupId() != null) {
            com.startin.api.model.StartupProfile st = startupProfileRepository.findById(dto.getStartupId())
                    .or(() -> startupProfileRepository.findByUserId(dto.getStartupId()))
                    .orElse(null);
            boolean isPro = st != null && st.getUser() != null && "PRO".equalsIgnoreCase(st.getUser().getPlan());
            int maxMonthly = isPro ? 15 : 5;
            Long effectiveStartupId = st != null ? st.getId() : dto.getStartupId();
            java.time.YearMonth currentMonth = java.time.YearMonth.now();
            long monthlyCount = opportunityRepository.findByStartupIdOrderByIdDesc(effectiveStartupId).stream()
                    .filter(o -> o.getCreatedAt() == null || java.time.YearMonth.from(o.getCreatedAt()).equals(currentMonth))
                    .count();
            if (monthlyCount >= maxMonthly) {
                throw new IllegalStateException("Limite mensal atingido: sua startup no Plano " + (isPro ? "Pro" : "Free") + " pode lançar até " + maxMonthly + " oportunidades por mês.");
            }
            if (st != null) {
                opp.setStartup(st);
                if (isPro) {
                    opp.setProMember(true);
                }
            }
        }
        if (dto.getTargetCategory() != null) {
            opp.setTargetCategory(dto.getTargetCategory());
        }

        opp.setColor(resolveColor(opp.getType()));

        Opportunity saved = opportunityRepository.save(opp);
        saved.setCode("opp-" + saved.getId());
        saved = opportunityRepository.save(saved);

        String companyName = saved.getCompany() != null && saved.getCompany().getName() != null
                ? saved.getCompany().getName()
                : "Empresa Parceira";
        String typeLabel = formatTypeLabel(saved.getType());

        // Notificação integrada para a Empresa que publicou a oportunidade
        if (saved.getCompany() != null) {
            Long companyUserId = saved.getCompany().getUser() != null ? saved.getCompany().getUser().getId() : null;
            notificationRepository.save(Notification.forCompany(
                    saved.getCompany().getId(),
                    companyUserId,
                    "Oportunidade lançada: " + saved.getTitle(),
                    "Sua demanda de " + typeLabel + " foi publicada e já está visível para startups compatíveis.",
                    "Oportunidades",
                    "edit-opportunity",
                    String.valueOf(saved.getId())
            ));
        }

        // Notificações integradas para todas as Startups compatíveis com o estágio desta oportunidade
        for (StartupProfile st : startupProfileRepository.findAll()) {
            if (MatchService.isOpportunityCompatibleWithStage(saved, st.getCurrentStage())) {
                Long stUserId = st.getUser() != null ? st.getUser().getId() : null;
                notificationRepository.save(Notification.forStartup(
                        st.getId(),
                        stUserId,
                        "Nova " + typeLabel + ": " + saved.getTitle(),
                        companyName + " lançou uma nova oportunidade compatível com o estágio da sua startup.",
                        "Oportunidades",
                        "opportunity",
                        String.valueOf(saved.getId())
                ));
            }
        }

        return OpportunityDTO.fromEntity(saved);
    }

    @Transactional
    public OpportunityDTO updateOpportunity(Long id, CreateOpportunityDTO dto) {
        Opportunity opp = opportunityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Oportunidade não encontrada: " + id));

        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            opp.setTitle(dto.getTitle().trim());
        }
        if (dto.getDescription() != null && !dto.getDescription().isBlank()) {
            opp.setDescription(dto.getDescription().trim());
            String d = dto.getDescription().trim();
            opp.setShortDescription(d.length() > 80 ? d.substring(0, 80) + "..." : d);
        }
        if (dto.getType() != null && !dto.getType().isBlank()) {
            opp.setType(OpportunityType.fromString(dto.getType()));
            opp.setColor(resolveColor(opp.getType()));
        }
        if (dto.getTags() != null) {
            opp.setTags(dto.getTags());
        }
        if (dto.getMeta() != null) {
            opp.setMetaInfo(dto.getMeta().isBlank() ? "Aberta · Brasil · Remoto" : dto.getMeta());
        }
        if (dto.getExternalUrl() != null) {
            opp.setExternalUrl(dto.getExternalUrl());
        }
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            opp.setStatus(OpportunityStatus.fromString(dto.getStatus()));
        }
        // Validação de segurança: garantir que a empresa só edite as próprias oportunidades
        if (opp.getCompany() != null && dto.getCompanyId() != null) {
            if (!opp.getCompany().getId().equals(dto.getCompanyId())) {
                throw new IllegalArgumentException("Acesso negado: você não tem permissão para alterar oportunidades de outra empresa.");
            }
        }
        if (opp.getCompany() == null && opp.getStartup() == null && dto.getCompanyId() != null) {
            companyRepository.findById(dto.getCompanyId()).ifPresent(opp::setCompany);
        }

        // Validação de segurança: garantir que a startup só edite as próprias oportunidades/demandas
        // (exige o startupId do dono; requisição sem startupId não pode alterar demanda de startup)
        if (opp.getStartup() != null) {
            if (dto.getStartupId() == null || !opp.getStartup().getId().equals(dto.getStartupId())) {
                throw new IllegalArgumentException("Acesso negado: você não tem permissão para alterar demandas de outra startup.");
            }
        }
        if (opp.getStartup() == null && opp.getCompany() == null && dto.getStartupId() != null) {
            startupProfileRepository.findById(dto.getStartupId()).ifPresent(opp::setStartup);
        }
        if (dto.getTargetCategory() != null) {
            opp.setTargetCategory(dto.getTargetCategory());
        }

        Opportunity saved = opportunityRepository.save(opp);

        if (saved.getCompany() != null) {
            Long companyUserId = saved.getCompany().getUser() != null ? saved.getCompany().getUser().getId() : null;
            notificationRepository.save(Notification.forCompany(
                    saved.getCompany().getId(),
                    companyUserId,
                    "Oportunidade atualizada: " + saved.getTitle(),
                    "As alterações de escopo, prazo ou status foram aplicadas com sucesso.",
                    "Sistema",
                    "edit-opportunity",
                    String.valueOf(saved.getId())
            ));
        }

        for (StartupProfile st : startupProfileRepository.findAll()) {
            if (MatchService.isOpportunityCompatibleWithStage(saved, st.getCurrentStage())) {
                Long stUserId = st.getUser() != null ? st.getUser().getId() : null;
                notificationRepository.save(Notification.forStartup(
                        st.getId(),
                        stUserId,
                        "Atualização em oportunidade: " + saved.getTitle(),
                        "Uma oportunidade compatível com seu perfil recebeu atualizações.",
                        "Oportunidades",
                        "opportunity",
                        String.valueOf(saved.getId())
                ));
            }
        }

        return OpportunityDTO.fromEntity(saved);
    }

    @Transactional
    public boolean deleteOpportunity(Long id) {
        if (!opportunityRepository.existsById(id)) {
            return false;
        }
        opportunityRepository.deleteById(id);
        return true;
    }

    @Transactional
    public void ensurePortalOpportunitiesSynced() {
        if (portalOpportunitiesSynced) {
            return;
        }
        List<Opportunity> existing = opportunityRepository.findAll();
        java.util.Map<String, Opportunity> existingByTitle = new java.util.HashMap<>();
        for (Opportunity o : existing) {
            if (o.getTitle() != null) {
                existingByTitle.put(o.getTitle().trim().toLowerCase(), o);
            }
        }

        record PortalSeed(String title, OpportunityType type, String description, String meta, String tags, String url) {}
        List<PortalSeed> seeds = List.of(
                new PortalSeed(
                        "Seleção Pública MCTI/Finep/FNDCT de Subvenção Econômica: Finep COOPERAMAIS Brasil Startups",
                        OpportunityType.EDITAL,
                        "Edital aberto mapeado no Capitaai com R$ 20,0 mi em subvenção econômica (Finep/MCTI) para startups e empresas de base tecnológica desenvolverem soluções inovadoras B2B e SaaS.",
                        "Fluxo Contínuo · Recursos: R$ 20,0 mi · Brasil",
                        "Capitaai • FINEP • Subvenção • Startups • Tecnologia",
                        "https://www.finep.gov.br/e/chamada-publica/222684/1060720"
                ),
                new PortalSeed(
                        "Edital FAPERJ Nº 26/2026 - Programa Bolsa de Inovação Tecnológica para Startups",
                        OpportunityType.EDITAL,
                        "Fomento mapeado pelo Capitaai para startups e empresas de tecnologia acelerarem projetos de pesquisa aplicada, inteligência artificial e transformação digital.",
                        "Prazo: 27/11/2026 · Rio de Janeiro, RJ · Brasil",
                        "Capitaai • FAPERJ • Inovação Tecnológica • Startups",
                        "https://www.faperj.br/rp/downloads/Edital_FAPPERJ_N%C2%BA_26_2026_%E2%80%93_Bolsa_de_Inova%C3%A7%C3%A3o_Tecnol%C3%B3gica_(INT).pdf"
                ),
                new PortalSeed(
                        "Chamada FAPEMIG SEDE 018/2026 - HUBMG GOV Inovação Aberta",
                        OpportunityType.EDITAL,
                        "Chamada pública com R$ 2,0 mi mapeada via Capitaai para contratação e fomento de soluções inovadoras de startups e empresas voltadas à eficiência operacional e GovTech.",
                        "Prazo: 09/12/2026 · Recursos: R$ 2,0 mi · Minas Gerais, MG · Brasil",
                        "Capitaai • FAPEMIG • GovTech • Inovação Aberta • B2B",
                        "https://api.site.fapemig.br/wp-content/uploads/2026-10-09__195__12819__sei_150716523_edital_fapemig_sede___018_2026___hubmg_gov__Am0Er84Q.pdf"
                ),
                new PortalSeed(
                        "Edital de Chamamento N° 7/2026 - Seleção de Projetos de Inovação em Medicamentos e Saúde",
                        OpportunityType.EDITAL,
                        "Edital aberto mapeado via Capitaai voltado a startups, empresas e ICTs com soluções de inovação em saúde, biotecnologia, fármacos e dispositivos médicos.",
                        "Prazo: 23/11/2026 · Brasil",
                        "Capitaai • Saúde & Healthtech • Biotech • Inovação",
                        "https://anvisalegis.datalegis.net/action/ActionDatalegis.php?acao=abrirResenhaAnoData&cod_modulo=630&cod_menu=9376"
                ),
                new PortalSeed(
                        "Edital Nº 06/2026 – Programa Nacional de Apoio à Geração de Empreendimentos Inovadores (Centelha 3)",
                        OpportunityType.EDITAL,
                        "Recursos de subvenção econômica mapeados no Capitaai para impulsionar startups em fase de validação e tração com produtos tecnológicos escaláveis.",
                        "Fluxo Contínuo · Brasil",
                        "Capitaai • Programa Centelha • Subvenção • Startups",
                        "https://www.fapeal.br/wp-content/uploads/2026/10/V.03.-Edital-FAPEAL-no-06.2026-Centelha-3-AL-2a-Retificacao_resulatdo-Fase-1-em-13.11.2026.pdf"
                ),
                new PortalSeed(
                        "Finep Mais Inovação Brasil – Rodada 2 – Tecnologias Digitais, IA e Semicondutores",
                        OpportunityType.EDITAL,
                        "Edital aberto na plataforma Capitaai (Finep) com R$ 100,0 mi para empresas e startups desenvolverem tecnologias de IA, sistemas embarcados, IoT e software avançado.",
                        "Prazo: 25/11/2026 · Recursos: R$ 100,0 mi · Brasil",
                        "Capitaai • FINEP • IA & Dados • Hardware & IoT",
                        "http://www.finep.gov.br/chamadas-publicas/chamadapublica/780"
                ),
                new PortalSeed(
                        "Edital Inova Startups 6ª Edição 2026 (Regulamento Oficial)",
                        OpportunityType.EDITAL,
                        "Programa de aceleração, capacitação e investimento financeiro para startups de base tecnológica com soluções B2B e IA.",
                        "Inscrições abertas até 30/11/2026 · Brasil · Remoto",
                        "InovaStartups • Aceleração • Investimento • B2B",
                        "https://inovastartups.com.br/edital-inova-6a-edicao.pdf"
                ),
                new PortalSeed(
                        "Chamada PIPE Jornada Tecnológica 2026 - Fase 1 (FAPESP)",
                        OpportunityType.EDITAL,
                        "Financiamento não reembolsável da FAPESP para validação de viabilidade técnico-científica de startups inovadoras.",
                        "Inscrições abertas em fluxo contínuo · São Paulo · Brasil",
                        "FAPESP • PIPE • Fase 1 • Subvenção",
                        "https://fapesp.br/pipe/normas/"
                ),
                new PortalSeed(
                        "REC'n'Play 2026 — Festival de Tecnologia, Inovação e Negócios (Porto Digital)",
                        OpportunityType.EVENTO,
                        "Maior festival de inovação e tecnologia do Nordeste, conectando startups a corporações, pitches e rodadas de negócios no ecossistema Porto Digital.",
                        "15 a 18 de Outubro de 2026 · Recife/PE · Presencial & Híbrido",
                        "Porto Digital • REC'n'Play • Startups • Conexão B2B • Inovação",
                        "https://recnplay.pe/recnplay-estreia-programacao-de-cinema-de-rua-com-mais-de-30-curtas-pernambucanos-e-o-agente-secreto/"
                ),
                new PortalSeed(
                        "Startup Summit 2026 — Sebrae Startups & ACATE",
                        OpportunityType.EVENTO,
                        "Encontro nacional de startups, fundos de Venture Capital e ecossistema corporativo com foco em escala, negócios e conexões de impacto.",
                        "26 a 28 de Agosto de 2026 · Florianópolis/SC · Híbrido",
                        "Sebrae • ACATE • Investimentos • Rodada de Negócios • B2B",
                        "https://www.startupsummit.com.br/ingressos"
                ),
                new PortalSeed(
                        "Gramado Summit 2026 — Feira de Startups, Inovação e Venture Capital",
                        OpportunityType.EVENTO,
                        "Um dos maiores eventos de brainstorming e conexão da América Latina, reunindo startups em validação e tração, investidores anjo, fundos de VC e executivos corporativos.",
                        "Inscrições abertas · Gramado/RS · Presencial",
                        "Gramado Summit • Venture Capital • Batalha de Startups • B2B",
                        "https://gramadosummit.com/lp-startup"
                ),
                new PortalSeed(
                        "Web Summit Rio 2026 — Programa Alpha & Beta para Startups",
                        OpportunityType.EVENTO,
                        "Conferência global de tecnologia e inovação no Rio de Janeiro conectando startups brasileiras e globais a fundos internacionais, grandes empresas e imprensa especializada.",
                        "Inscrições abertas · Rio de Janeiro/RJ · Presencial",
                        "Web Summit Rio • Global • Investidores • IA & SaaS • Startups",
                        "https://rio.websummit.com/pt-br/startups/pitch/"
                ),
                new PortalSeed(
                        "Ciclo de Inovação Aberta & Conexão Corporativa Porto Digital 2026",
                        OpportunityType.EVENTO,
                        "Encontros técnicos, desafios de inovação aberta, mentorias e rodadas de negócios para startups residentes e conectadas ao ecossistema do Porto Digital.",
                        "Agenda Contínua 2026 · Recife/PE · Presencial & Online",
                        "Porto Digital • Recife • Inovação Aberta • Startups • B2B",
                        "https://portodigital.org/noticia/recnplay-capitulo-saude-conecta-os-ecossistemas-de-saude-e-tecnologia-do-recife-confira-a-programacao/"
                )
        );

        for (PortalSeed seed : seeds) {
            String key = seed.title().trim().toLowerCase();
            Opportunity existingOpp = existingByTitle.get(key);
            if (existingOpp == null) {
                Opportunity opp = new Opportunity();
                opp.setTitle(seed.title());
                opp.setType(seed.type());
                opp.setDescription(seed.description());
                opp.setShortDescription(seed.description().length() > 110 ? seed.description().substring(0, 110) + "..." : seed.description());
                opp.setMetaInfo(seed.meta());
                opp.setTags(seed.tags());
                opp.setExternalUrl(seed.url());
                opp.setStatus(OpportunityStatus.PUBLICADA);
                opp.setMatchScore(90);
                opp.setColor(resolveColor(seed.type()));
                opp = opportunityRepository.save(opp);
                opp.setCode("web-" + opp.getId());
                opportunityRepository.save(opp);
                existingByTitle.put(key, opp);
            } else {
                String currentUrl = existingOpp.getExternalUrl() != null ? existingOpp.getExternalUrl().trim().toLowerCase() : "";
                boolean isBareHomeOrIntermediate = currentUrl.isEmpty()
                        || currentUrl.equals("https://recnplay.pe/") || currentUrl.equals("https://recnplay.pe")
                        || currentUrl.equals("https://www.startupsummit.com.br/") || currentUrl.equals("https://www.startupsummit.com.br")
                        || currentUrl.equals("https://gramadosummit.com/") || currentUrl.equals("https://gramadosummit.com")
                        || currentUrl.equals("https://portodigital.org/") || currentUrl.equals("https://portodigital.org")
                        || currentUrl.equals("https://rio.websummit.com/") || currentUrl.equals("https://websummit.com/rio/")
                        || (currentUrl.contains("capitaai.com.br/captacao/") && (seed.url().toLowerCase().contains(".pdf") || !seed.url().contains("capitaai.com.br/captacao/")));
                if (isBareHomeOrIntermediate) {
                    existingOpp.setExternalUrl(seed.url());
                    opportunityRepository.save(existingOpp);
                }
            }
        }
        portalOpportunitiesSynced = true;
    }

    private String formatTypeLabel(OpportunityType type) {
        if (type == null) return "Oportunidade";
        return switch (type) {
            case POC -> "POC";
            case PRIMEIRA_COMPRA -> "Primeira Compra";
            case COMPRA -> "Compra";
            case EVENTO -> "Evento";
            case EDITAL -> "Edital";
            case SERVICO -> "Demanda de Serviço";
            default -> "Oportunidade";
        };
    }

    private String resolveColor(OpportunityType type) {
        if (type == null) return "bg-[#fff2b8] text-[#795900]";
        return switch (type) {
            case EDITAL -> "bg-[#fff2b8] text-[#795900]";
            case POC -> "bg-[#d9f1e5] text-[#16734d]";
            case PRIMEIRA_COMPRA -> "bg-[#fef3c7] text-[#92400e]";
            case EVENTO -> "bg-[#dff1f5] text-[#1e7080]";
            case COMPRA -> "bg-[#ffe1c3] text-[#975524]";
            case SERVICO -> "bg-[#e0f2fe] text-[#0369a1]";
            default -> "bg-[#f3f6f8] text-[#111827]";
        };
    }
}
