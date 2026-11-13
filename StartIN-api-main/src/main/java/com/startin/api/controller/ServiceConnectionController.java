package com.startin.api.controller;

import com.startin.api.dto.provider.ServiceConnectionDTO;
import com.startin.api.service.ServiceConnectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/connections")
@Tag(name = "Conexões de Serviço", description = "Gerenciamento de solicitações e conexões ativas entre Startups e Prestadores")
public class ServiceConnectionController {

    private final ServiceConnectionService connectionService;

    public ServiceConnectionController(ServiceConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @GetMapping
    @Operation(summary = "Lista conexões por startupId, providerId ou userId/role")
    public ResponseEntity<List<ServiceConnectionDTO>> list(
            @RequestParam(required = false) Long startupId,
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String role) {
        return ResponseEntity.ok(connectionService.listForQuery(startupId, providerId, userId, role));
    }

    @PostMapping
    @Operation(summary = "Cria uma nova solicitação de conexão entre Startup e Prestador")
    public ResponseEntity<ServiceConnectionDTO> create(@RequestBody Map<String, Object> body) {
        Long startupId = body.get("startupId") != null ? Long.valueOf(body.get("startupId").toString()) : null;
        Long providerId = body.get("providerId") != null ? Long.valueOf(body.get("providerId").toString()) : null;
        String senderType = body.get("senderType") != null ? body.get("senderType").toString() : "STARTUP";
        String message = body.get("message") != null ? body.get("message").toString() : "";
        Integer matchScore = body.get("matchScore") != null ? Integer.valueOf(body.get("matchScore").toString()) : null;

        return ResponseEntity.ok(connectionService.createConnection(startupId, providerId, senderType, message, matchScore));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualiza o status da conexão (ACCEPTED, REJECTED, PENDING)")
    public ResponseEntity<ServiceConnectionDTO> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.getOrDefault("status", "ACCEPTED");
        return ResponseEntity.ok(connectionService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove/exclui uma conexão entre Startup em Tração e Prestador de Serviço")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        connectionService.deleteConnection(id);
        return ResponseEntity.ok(Map.of("deleted", true, "id", id));
    }
}
