package com.startin.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Endpoint leve de verificacao de saude usado pelo healthcheck do Railway.
 * Nao depende do microservico de IA para responder.
 */
@RestController
public class HealthController {

    @GetMapping({"/api/health", "/health"})
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "startin-api",
                "timestamp", OffsetDateTime.now().toString()
        ));
    }
}
