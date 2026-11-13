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
    @Operation(summary = "Lista todas as oportunidades com filtros por tipo, busca, empresa ou startup")
    public ResponseEntity<List<OpportunityDTO>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long startupId) {
        return ResponseEntity.ok(opportunityService.listOpportunities(type, query, companyId, startupId));
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
    @Operation(summary = "Cadastra uma nova oportunidade (POC, Primeira Compra, Compra, Edital, Evento)")
    public ResponseEntity<OpportunityDTO> create(@Valid @RequestBody CreateOpportunityDTO dto) {
        return ResponseEntity.ok(opportunityService.createOpportunity(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita uma oportunidade existente (POC, Primeira Compra, Compra, Edital, Evento)")
    public ResponseEntity<OpportunityDTO> update(@PathVariable Long id, @RequestBody CreateOpportunityDTO dto) {
        return ResponseEntity.ok(opportunityService.updateOpportunity(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove uma oportunidade existente")
    public ResponseEntity<java.util.Map<String, Object>> delete(@PathVariable Long id) {
        boolean deleted = opportunityService.deleteOpportunity(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(java.util.Map.of("deleted", true, "id", id));
    }
}
