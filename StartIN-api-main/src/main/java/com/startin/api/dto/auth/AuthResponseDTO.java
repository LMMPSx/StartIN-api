package com.startin.api.dto.auth;

public class AuthResponseDTO {

    private boolean success;
    private String token;
    private Long userId;
    private String email;
    private String name;
    private String cnpj;
    private Long startupProfileId;
    private String message;

    public AuthResponseDTO() {
    }

    public AuthResponseDTO(boolean success, String token, Long userId, String email, String name, String cnpj, Long startupProfileId, String message) {
        this.success = success;
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.cnpj = cnpj;
        this.startupProfileId = startupProfileId;
        this.message = message;
    }

    public static AuthResponseDTO ok(String token, Long userId, String email, String name, String cnpj, Long startupProfileId) {
        return new AuthResponseDTO(true, token, userId, email, name, cnpj, startupProfileId, "Autenticado com sucesso");
    }

    public static AuthResponseDTO error(String message) {
        return new AuthResponseDTO(false, null, null, null, null, null, null, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public Long getStartupProfileId() {
        return startupProfileId;
    }

    public void setStartupProfileId(Long startupProfileId) {
        this.startupProfileId = startupProfileId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
