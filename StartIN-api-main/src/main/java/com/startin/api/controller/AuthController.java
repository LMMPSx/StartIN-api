package com.startin.api.controller;

import com.startin.api.dto.auth.AuthResponseDTO;
import com.startin.api.dto.auth.LoginRequestDTO;
import com.startin.api.dto.auth.PasswordResetRequestDTO;
import com.startin.api.dto.auth.SignupRequestDTO;
import com.startin.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Endpoints para Login, Cadastro e Recuperação de Senha")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Realiza login na plataforma")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        AuthResponseDTO response = authService.login(request);
        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/signup")
    @Operation(summary = "Cadastra uma nova conta de startup")
    public ResponseEntity<AuthResponseDTO> signup(@Valid @RequestBody SignupRequestDTO request) {
        AuthResponseDTO response = authService.signup(request);
        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Redefine a senha de acesso")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetRequestDTO request) {
        boolean success = authService.resetPassword(request);
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}
