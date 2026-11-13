package com.startin.api.controller;

import com.startin.api.dto.provider.ServiceProviderDTO;
import com.startin.api.service.ServiceProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/providers")
@Tag(name = "Prestadores de Serviço", description = "Endpoints 100% integrados ao banco MySQL e à IA para prestadores de serviço e startups em Tração")
public class ServiceProviderController {

    private final ServiceProviderService providerService;

    public ServiceProviderController(ServiceProviderService providerService) {
        this.providerService = providerService;
    }

    @GetMapping
    @Operation(summary = "Lista prestadores de serviço com cálculo real de compatibilidade via IA contra a startup logada")
    public ResponseEntity<List<ServiceProviderDTO>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long startupId,
            @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(providerService.listProviders(category, q, startupId, userId));
    }

    @GetMapping("/dashboard-summary")
    @Operation(summary = "Retorna resumo 100% real do dashboard do prestador (contadores, startups recomendadas em Tração e atividades)")
    public ResponseEntity<Map<String, Object>> getDashboardSummaryQuery(
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(providerService.getProviderDashboardSummary(providerId, userId));
    }

    @GetMapping("/{id}/dashboard-summary")
    @Operation(summary = "Retorna resumo 100% real do dashboard de um prestador específico")
    public ResponseEntity<Map<String, Object>> getDashboardSummary(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(providerService.getProviderDashboardSummary(id, userId));
    }

    @GetMapping("/matching-startups")
    @Operation(summary = "Retorna as Startups em estágio de Tração avaliadas pela IA para o Prestador de Serviço")
    public ResponseEntity<List<Map<String, Object>>> getMatchingStartupsQuery(
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false, defaultValue = "false") boolean onlySaved) {
        return ResponseEntity.ok(providerService.getMatchingStartupsForProvider(providerId, userId, onlySaved));
    }

    @GetMapping("/{id}/matching-startups")
    @Operation(summary = "Retorna as Startups em estágio de Tração avaliadas pela IA para um Prestador específico")
    public ResponseEntity<List<Map<String, Object>>> getMatchingStartups(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false, defaultValue = "false") boolean onlySaved) {
        return ResponseEntity.ok(providerService.getMatchingStartupsForProvider(id, userId, onlySaved));
    }

    @PostMapping("/saved-startups/{startupId}")
    @Operation(summary = "Salva ou remove uma Startup de Tração dos favoritos do Prestador no MySQL")
    public ResponseEntity<Map<String, Object>> toggleSavedStartupQuery(
            @PathVariable Long startupId,
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(providerService.toggleSavedStartup(providerId, userId, startupId));
    }

    @PostMapping("/{id}/saved-startups/{startupId}")
    @Operation(summary = "Salva ou remove uma Startup de Tração dos favoritos de um Prestador específico no MySQL")
    public ResponseEntity<Map<String, Object>> toggleSavedStartup(
            @PathVariable Long id,
            @PathVariable Long startupId,
            @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(providerService.toggleSavedStartup(id, userId, startupId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca detalhes de um prestador por ID com análise de compatibilidade da IA")
    public ResponseEntity<ServiceProviderDTO> getById(
            @PathVariable Long id,
            @RequestParam(required = false) Long startupId,
            @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(providerService.getProviderById(id, startupId, userId));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Busca perfil do prestador pelo ID do usuário")
    public ResponseEntity<ServiceProviderDTO> getByUserId(@PathVariable Long userId) {
        ServiceProviderDTO dto = providerService.getProviderByUserId(userId);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    @Operation(summary = "Cadastra novo prestador de serviço")
    public ResponseEntity<ServiceProviderDTO> create(@RequestBody ServiceProviderDTO dto) {
        return ResponseEntity.ok(providerService.createProvider(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza dados do prestador de serviço")
    public ResponseEntity<ServiceProviderDTO> update(@PathVariable Long id, @RequestBody ServiceProviderDTO dto) {
        return ResponseEntity.ok(providerService.updateProvider(id, dto));
    }
}
