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
@Tag(name = "Notificações", description = "Endpoints de alertas de matches, editais e sistema")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Lista as notificações com filtro opcional por aba (Todas, Oportunidades, Matches, Sistema)")
    public ResponseEntity<List<NotificationDTO>> list(@RequestParam(required = false) String type) {
        return ResponseEntity.ok(notificationService.listAll(type));
    }

    @PostMapping("/read-all")
    @Operation(summary = "Marca todas as notificações como lidas")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok().build();
    }
}
