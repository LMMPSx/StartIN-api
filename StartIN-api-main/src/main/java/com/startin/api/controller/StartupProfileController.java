package com.startin.api.controller;

import com.startin.api.dto.startup.MatchingPreferencesDTO;
import com.startin.api.dto.startup.StartupProfileDTO;
import com.startin.api.service.StartupProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/startups")
@Tag(name = "Startup", description = "Endpoints para gerenciamento do perfil e preferências da startup")
public class StartupProfileController {

    private final StartupProfileService startupProfileService;

    public StartupProfileController(StartupProfileService startupProfileService) {
        this.startupProfileService = startupProfileService;
    }

    @GetMapping
    @Operation(summary = "Lista todos os perfis de startup cadastrados no banco")
    public ResponseEntity<List<StartupProfileDTO>> getAllProfiles() {
        return ResponseEntity.ok(startupProfileService.getAllProfiles());
    }

    @GetMapping("/default")
    @Operation(summary = "Retorna o primeiro perfil de startup cadastrado no banco")
    public ResponseEntity<StartupProfileDTO> getDefaultProfile() {
        StartupProfileDTO dto = startupProfileService.getDefaultProfile();
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Busca perfil de startup pelo ID do usuário")
    public ResponseEntity<StartupProfileDTO> getProfileByUserId(@PathVariable Long userId) {
        return startupProfileService.getProfileByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca perfil de startup por ID")
    public ResponseEntity<StartupProfileDTO> getProfile(@PathVariable Long id) {
        return startupProfileService.getProfile(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza todos os dados de onboarding e perfil da startup")
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
