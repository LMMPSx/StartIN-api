package com.startin.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

public class PasswordResetRequestDTO {

    @NotBlank(message = "E-mail ou usuário é obrigatório")
    private String email;

    private String newPassword;

    public PasswordResetRequestDTO() {
    }

    public PasswordResetRequestDTO(String email, String newPassword) {
        this.email = email;
        this.newPassword = newPassword;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
