package com.startin.api.controller;

import com.startin.api.dto.notification.NotificationDTO;
import com.startin.api.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notificações", description = "Endpoints integrados de alertas por perfil (Startup e Empresa) com redirecionamento e leitura")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Lista as notificações integradas de acordo com o perfil (STARTUP, COMPANY ou PROVIDER) e aba")
    public ResponseEntity<List<NotificationDTO>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long startupId,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long providerId) {
        return ResponseEntity.ok(notificationService.listForProfile(type, role, userId, startupId, companyId, providerId));
    }

    @PutMapping("/{id}/read")
    @PostMapping("/{id}/read")
    @Operation(summary = "Marca uma notificação específica como vista/lida ao ser acessada")
    public ResponseEntity<NotificationDTO> markAsRead(@PathVariable Long id) {
        NotificationDTO updated = notificationService.markAsRead(id);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/read-all")
    @PutMapping("/read-all")
    @Operation(summary = "Marca todas as notificações do perfil ativo como lidas")
    public ResponseEntity<Void> markAllAsRead(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long startupId,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long providerId) {
        notificationService.markAllAsRead(role, userId, startupId, companyId, providerId);
        return ResponseEntity.ok().build();
    }
}
