package com.startin.api.dto.auth;

import com.startin.api.model.User;

public class UserDTO {

    private Long id;
    private String email;
    private String name;
    private String cnpj;
    private String phone;
    private String avatarUrl;
    private String role;

    public UserDTO() {
    }

    public UserDTO(Long id, String email, String name, String cnpj, String phone, String avatarUrl, String role) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.cnpj = cnpj;
        this.phone = phone;
        this.avatarUrl = avatarUrl;
        this.role = role;
    }

    public static UserDTO fromEntity(User user) {
        if (user == null) return null;
        return new UserDTO(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getCnpj(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getRole() != null ? user.getRole().name() : "USER"
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
