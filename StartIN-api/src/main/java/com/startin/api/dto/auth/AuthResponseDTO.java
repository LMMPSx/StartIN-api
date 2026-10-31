package com.startin.api.dto.auth;

public class AuthResponseDTO {

    private boolean success;
    private String token;
    private Long userId;
    private String email;
    private String name;
    private String cnpj;
    private String phone;
    private String avatarUrl;
    private Long startupProfileId;
    private Long companyId;
    private String accountType;
    private String message;

    public AuthResponseDTO() {
    }

    public AuthResponseDTO(boolean success, String token, Long userId, String email, String name, String cnpj, String avatarUrl, Long startupProfileId, String message) {
        this(success, token, userId, email, name, cnpj, null, avatarUrl, startupProfileId, null, "STARTUP", message);
    }

    public AuthResponseDTO(boolean success, String token, Long userId, String email, String name, String cnpj, String avatarUrl, Long startupProfileId, Long companyId, String accountType, String message) {
        this(success, token, userId, email, name, cnpj, null, avatarUrl, startupProfileId, companyId, accountType, message);
    }

    public AuthResponseDTO(boolean success, String token, Long userId, String email, String name, String cnpj, String phone, String avatarUrl, Long startupProfileId, Long companyId, String accountType, String message) {
        this.success = success;
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.cnpj = cnpj;
        this.phone = phone;
        this.avatarUrl = avatarUrl;
        this.startupProfileId = startupProfileId;
        this.companyId = companyId;
        this.accountType = accountType != null ? accountType : "STARTUP";
        this.message = message;
    }

    public static AuthResponseDTO ok(String token, Long userId, String email, String name, String cnpj, Long startupProfileId) {
        return new AuthResponseDTO(true, token, userId, email, name, cnpj, null, null, startupProfileId, null, "STARTUP", "Autenticado com sucesso");
    }

    public static AuthResponseDTO ok(String token, Long userId, String email, String name, String cnpj, String avatarUrl, Long startupProfileId) {
        return new AuthResponseDTO(true, token, userId, email, name, cnpj, null, avatarUrl, startupProfileId, null, "STARTUP", "Autenticado com sucesso");
    }

    public static AuthResponseDTO ok(String token, Long userId, String email, String name, String cnpj, String avatarUrl, Long startupProfileId, Long companyId, String accountType) {
        return new AuthResponseDTO(true, token, userId, email, name, cnpj, null, avatarUrl, startupProfileId, companyId, accountType, "Autenticado com sucesso");
    }

    public static AuthResponseDTO ok(String token, Long userId, String email, String name, String cnpj, String phone, String avatarUrl, Long startupProfileId, Long companyId, String accountType) {
        return new AuthResponseDTO(true, token, userId, email, name, cnpj, phone, avatarUrl, startupProfileId, companyId, accountType, "Autenticado com sucesso");
    }

    public static AuthResponseDTO error(String message) {
        return new AuthResponseDTO(false, null, null, null, null, null, null, null, null, null, null, message);
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Long getStartupProfileId() {
        return startupProfileId;
    }

    public void setStartupProfileId(Long startupProfileId) {
        this.startupProfileId = startupProfileId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
