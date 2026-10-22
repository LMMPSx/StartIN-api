package com.startin.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

public class SignupRequestDTO {

    private String cnpj;

    @NotBlank(message = "E-mail ou usuário é obrigatório")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    private String password;

    private String confirm;

    private String name;

    public SignupRequestDTO() {
    }

    public SignupRequestDTO(String cnpj, String email, String password, String confirm, String name) {
        this.cnpj = cnpj;
        this.email = email;
        this.password = password;
        this.confirm = confirm;
        this.name = name;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirm() {
        return confirm;
    }

    public void setConfirm(String confirm) {
        this.confirm = confirm;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
