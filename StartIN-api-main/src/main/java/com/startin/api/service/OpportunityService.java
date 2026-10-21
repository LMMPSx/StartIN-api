package com.startin.api.service;

import com.startin.api.dto.opportunity.CreateOpportunityDTO;
import com.startin.api.dto.opportunity.OpportunityDTO;
import com.startin.api.model.Opportunity;
import com.startin.api.model.enums.OpportunityStatus;
import com.startin.api.model.enums.OpportunityType;
import com.startin.api.repository.OpportunityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class OpportunityService {

    private final OpportunityRepository opportunityRepository;

    public OpportunityService(OpportunityRepository opportunityRepository) {
        this.opportunityRepository = opportunityRepository;
    }

    public List<OpportunityDTO> listOpportunities(String type, String query) {
        List<Opportunity> list;
        boolean hasQuery = query != null && !query.isBlank();
        boolean hasType = type != null && !type.isBlank() && !"Todos".equalsIgnoreCase(type);

        if (hasType && hasQuery) {
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

        String color = switch (opp.getType()) {
            case EDITAL -> "bg-[#fff2b8] text-[#795900]";
            case POC -> "bg-[#d9f1e5] text-[#16734d]";
            case EVENTO -> "bg-[#dff1f5] text-[#1e7080]";
            case COMPRA -> "bg-[#ffe1c3] text-[#975524]";
        };
        opp.setColor(color);

        Opportunity saved = opportunityRepository.save(opp);
        saved.setCode("opp-" + saved.getId());
        saved = opportunityRepository.save(saved);

        return OpportunityDTO.fromEntity(saved);
    }
}
