package com.startin.api.service;

import com.startin.api.dto.auth.AuthResponseDTO;
import com.startin.api.dto.auth.LoginRequestDTO;
import com.startin.api.dto.auth.PasswordResetRequestDTO;
import com.startin.api.dto.auth.SignupRequestDTO;
import com.startin.api.model.StartupProfile;
import com.startin.api.model.User;
import com.startin.api.model.enums.UserRole;
import com.startin.api.repository.StartupProfileRepository;
import com.startin.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final StartupProfileRepository startupProfileRepository;

    public AuthService(UserRepository userRepository, StartupProfileRepository startupProfileRepository) {
        this.userRepository = userRepository;
        this.startupProfileRepository = startupProfileRepository;
    }

    @Transactional
    public AuthResponseDTO signup(SignupRequestDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return AuthResponseDTO.error("Este e-mail já está cadastrado.");
        }

        User user = new User();
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPassword(request.getPassword()); // Em produção aplicar BCryptPasswordEncoder
        user.setCnpj(request.getCnpj());
        user.setName(request.getName() != null && !request.getName().isBlank() ? request.getName() : "Usuário StartIn");
        user.setRole(UserRole.USER);
        user = userRepository.save(user);

        // Cria automaticamente o perfil de startup vinculado
        StartupProfile profile = new StartupProfile();
        profile.setUser(user);
        profile.setName(request.getName() != null && !request.getName().isBlank() ? request.getName() : "Minha Startup");
        profile.setCnpj(request.getCnpj());
        profile = startupProfileRepository.save(profile);

        String token = UUID.randomUUID().toString();
        return AuthResponseDTO.ok(token, user.getId(), user.getEmail(), user.getName(), user.getCnpj(), profile.getId());
    }

    public AuthResponseDTO login(LoginRequestDTO request) {
        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail().toLowerCase().trim());
        if (optionalUser.isEmpty()) {
            return AuthResponseDTO.error("E-mail ou senha inválidos.");
        }

        User user = optionalUser.get();
        if (!user.getPassword().equals(request.getPassword())) {
            return AuthResponseDTO.error("E-mail ou senha inválidos.");
        }

        Optional<StartupProfile> profile = startupProfileRepository.findByUserId(user.getId());
        Long profileId = profile.map(StartupProfile::getId).orElse(null);

        String token = UUID.randomUUID().toString();
        return AuthResponseDTO.ok(token, user.getId(), user.getEmail(), user.getName(), user.getCnpj(), profileId);
    }

    @Transactional
    public boolean resetPassword(PasswordResetRequestDTO request) {
        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail().toLowerCase().trim());
        if (optionalUser.isEmpty()) {
            return false;
        }

        User user = optionalUser.get();
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            user.setPassword(request.getNewPassword());
            userRepository.save(user);
        }
        return true;
    }
}
