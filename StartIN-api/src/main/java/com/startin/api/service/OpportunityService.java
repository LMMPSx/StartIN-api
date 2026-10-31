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
        return listOpportunities(type, query, null);
    }

    public List<OpportunityDTO> listOpportunities(String type, String query, Long companyId) {
        List<Opportunity> list;
        boolean hasQuery = query != null && !query.isBlank();
        boolean hasType = type != null && !type.isBlank() && !"Todos".equalsIgnoreCase(type);

        if (companyId != null) {
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

        return list.stream().map(OpportunityDTO::fromEntity).toList();
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
            companyRepository.findById(dto.getCompanyId()).ifPresent(opp::setCompany);
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
        if (dto.getCompanyId() != null) {
            companyRepository.findById(dto.getCompanyId()).ifPresent(opp::setCompany);
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

    private String formatTypeLabel(OpportunityType type) {
        if (type == null) return "Oportunidade";
        return switch (type) {
            case POC -> "POC";
            case PRIMEIRA_COMPRA -> "Primeira Compra";
            case COMPRA -> "Compra";
            case EVENTO -> "Evento";
            case EDITAL -> "Edital";
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
        };
    }
}
