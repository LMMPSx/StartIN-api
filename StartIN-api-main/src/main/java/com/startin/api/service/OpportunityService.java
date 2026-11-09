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

    public List<OpportunityDTO> listOpportunities(String type, String query, Long companyId, Long startupId) {
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
            java.time.YearMonth currentMonth = java.time.YearMonth.now();
            long monthlyCount = opportunityRepository.findByCompanyIdOrderByIdDesc(dto.getCompanyId()).stream()
                    .filter(o -> o.getCreatedAt() == null || java.time.YearMonth.from(o.getCreatedAt()).equals(currentMonth))
                    .count();
            if (monthlyCount >= 10) {
                throw new IllegalStateException("Limite mensal atingido: sua conta pode lançar até 10 oportunidades por mês.");
            }
            companyRepository.findById(dto.getCompanyId()).ifPresent(c -> {
                opp.setCompany(c);
                if (c.getUser() != null && "PRO".equalsIgnoreCase(c.getUser().getPlan())) {
                    opp.setProMember(true);
                }
            });
        }
        if (dto.getStartupId() != null) {
            java.time.YearMonth currentMonth = java.time.YearMonth.now();
            long monthlyCount = opportunityRepository.findByStartupIdOrderByIdDesc(dto.getStartupId()).stream()
                    .filter(o -> o.getCreatedAt() == null || java.time.YearMonth.from(o.getCreatedAt()).equals(currentMonth))
                    .count();
            if (monthlyCount >= 10) {
                throw new IllegalStateException("Limite mensal atingido: sua startup pode lançar até 10 oportunidades por mês.");
            }
            startupProfileRepository.findById(dto.getStartupId()).ifPresent(st -> {
                opp.setStartup(st);
                if (st.getUser() != null && "PRO".equalsIgnoreCase(st.getUser().getPlan())) {
                    opp.setProMember(true);
                }
            });
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
