package com.startin.api.controller;

import com.startin.api.dto.opportunity.CreateOpportunityDTO;
import com.startin.api.dto.opportunity.OpportunityDTO;
import com.startin.api.service.OpportunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/opportunities")
@Tag(name = "Oportunidades", description = "Endpoints para consulta e cadastro de Editais, POCs, Eventos e Compras")
public class OpportunityController {

    private final OpportunityService opportunityService;

    public OpportunityController(OpportunityService opportunityService) {
        this.opportunityService = opportunityService;
    }

    @GetMapping
    @Operation(summary = "Lista todas as oportunidades com filtros por tipo e busca")
    public ResponseEntity<List<OpportunityDTO>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(opportunityService.listOpportunities(type, query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca detalhes de uma oportunidade por ID ou Código")
    public ResponseEntity<OpportunityDTO> getById(@PathVariable String id) {
        try {
            Long numericId = Long.parseLong(id);
            return opportunityService.getById(numericId)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> opportunityService.getByCode(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build()));
        } catch (NumberFormatException e) {
            return opportunityService.getByCode(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
    }

    @PostMapping
    @Operation(summary = "Cadastra uma nova oportunidade (POC, Compra, Edital, Evento)")
    public ResponseEntity<OpportunityDTO> create(@Valid @RequestBody CreateOpportunityDTO dto) {
        return ResponseEntity.ok(opportunityService.createOpportunity(dto));
    }
}
