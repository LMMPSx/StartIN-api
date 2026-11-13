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

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Endpoints para Login, Cadastro, Conta e Recuperação de Senha")
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

    @GetMapping("/user/{userId}")
    @Operation(summary = "Busca dados da conta do usuário logado")
    public ResponseEntity<AuthResponseDTO> getUser(@PathVariable Long userId) {
        return authService.getUserInfo(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/user/{userId}")
    @Operation(summary = "Atualiza nome e e-mail da conta do usuário")
    public ResponseEntity<AuthResponseDTO> updateUser(
            @PathVariable Long userId,
            @RequestBody Map<String, String> payload) {
        return authService.updateUserAccount(userId, payload)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/user/{userId}")
    @Operation(summary = "Exclui a conta do usuário e seu perfil")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        boolean deleted = authService.deleteUserAccount(userId);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Redefine a senha de acesso")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetRequestDTO request) {
        boolean success = authService.resetPassword(request);
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}
