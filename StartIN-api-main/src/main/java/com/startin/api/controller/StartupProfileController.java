package com.startin.api.controller;

import com.startin.api.dto.startup.MatchingPreferencesDTO;
import com.startin.api.dto.startup.StartupProfileDTO;
import com.startin.api.service.StartupProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/startups")
@Tag(name = "Startup", description = "Endpoints para gerenciamento do perfil e preferências da startup")
public class StartupProfileController {

    private final StartupProfileService startupProfileService;

    public StartupProfileController(StartupProfileService startupProfileService) {
        this.startupProfileService = startupProfileService;
    }

    @GetMapping("/default")
    @Operation(summary = "Retorna o perfil padrão da startup ativa (ex: Nexa Health)")
    public ResponseEntity<StartupProfileDTO> getDefaultProfile() {
        return ResponseEntity.ok(startupProfileService.getDefaultProfile());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca perfil de startup por ID")
    public ResponseEntity<StartupProfileDTO> getProfile(@PathVariable Long id) {
        return startupProfileService.getProfile(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza os dados de onboarding e perfil da startup")
    public ResponseEntity<StartupProfileDTO> updateProfile(
            @PathVariable Long id,
            @RequestBody StartupProfileDTO dto) {
        return ResponseEntity.ok(startupProfileService.updateProfile(id, dto));
    }

    @PutMapping("/{id}/preferences")
    @Operation(summary = "Atualiza as preferências de matching e recomendações da IA")
    public ResponseEntity<StartupProfileDTO> updatePreferences(
            @PathVariable Long id,
            @RequestBody MatchingPreferencesDTO prefs) {
        return ResponseEntity.ok(startupProfileService.updatePreferences(id, prefs));
    }
}
