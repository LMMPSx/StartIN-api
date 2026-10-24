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

import java.util.Map;
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
        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            return AuthResponseDTO.error("Este e-mail já está cadastrado.");
        }

        User user = new User();
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPassword(request.getPassword());
        user.setCnpj(request.getCnpj());
        String defaultName = request.getName() != null && !request.getName().isBlank()
                ? request.getName()
                : request.getEmail().split("@")[0];
        user.setName(defaultName);
        user.setRole(UserRole.USER);
        user = userRepository.save(user);

        StartupProfile profile = new StartupProfile();
        profile.setUser(user);
        profile.setName(request.getName() != null && !request.getName().isBlank() ? request.getName() : "Minha Startup");
        profile.setCnpj(request.getCnpj());
        profile.setReceiveAiRecommendations(true);
        profile = startupProfileRepository.save(profile);

        String token = UUID.randomUUID().toString();
        return AuthResponseDTO.ok(token, user.getId(), user.getEmail(), user.getName(), user.getCnpj(), user.getAvatarUrl(), profile.getId());
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

        StartupProfile profile = startupProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    StartupProfile newProfile = new StartupProfile();
                    newProfile.setUser(user);
                    newProfile.setName(user.getName() != null ? user.getName() : "Minha Startup");
                    newProfile.setCnpj(user.getCnpj());
                    newProfile.setReceiveAiRecommendations(true);
                    return startupProfileRepository.save(newProfile);
                });

        String avatar = user.getAvatarUrl() != null ? user.getAvatarUrl() : profile.getAvatarUrl();
        String token = UUID.randomUUID().toString();
        return AuthResponseDTO.ok(token, user.getId(), user.getEmail(), user.getName(), user.getCnpj(), avatar, profile.getId());
    }

    public Optional<AuthResponseDTO> getUserInfo(Long userId) {
        return userRepository.findById(userId).map(user -> {
            Optional<StartupProfile> profileOpt = startupProfileRepository.findByUserId(user.getId());
            Long profileId = profileOpt.map(StartupProfile::getId).orElse(null);
            String avatar = user.getAvatarUrl() != null
                    ? user.getAvatarUrl()
                    : profileOpt.map(StartupProfile::getAvatarUrl).orElse(null);
            return AuthResponseDTO.ok(null, user.getId(), user.getEmail(), user.getName(), user.getCnpj(), avatar, profileId);
        });
    }

    @Transactional
    public Optional<AuthResponseDTO> updateUserAccount(Long userId, Map<String, String> payload) {
        return userRepository.findById(userId).map(user -> {
            if (payload.get("name") != null && !payload.get("name").isBlank()) {
                user.setName(payload.get("name").trim());
            }
            if (payload.get("email") != null && !payload.get("email").isBlank()) {
                user.setEmail(payload.get("email").toLowerCase().trim());
            }
            if (payload.get("cnpj") != null) {
                user.setCnpj(payload.get("cnpj").trim());
            }
            if (payload.containsKey("avatarUrl")) {
                user.setAvatarUrl(payload.get("avatarUrl"));
            }
            user = userRepository.save(user);

            Optional<StartupProfile> profileOpt = startupProfileRepository.findByUserId(user.getId());
            if (profileOpt.isPresent() && payload.containsKey("avatarUrl")) {
                StartupProfile p = profileOpt.get();
                p.setAvatarUrl(payload.get("avatarUrl"));
                startupProfileRepository.save(p);
            }

            Long profileId = profileOpt.map(StartupProfile::getId).orElse(null);
            return AuthResponseDTO.ok(null, user.getId(), user.getEmail(), user.getName(), user.getCnpj(), user.getAvatarUrl(), profileId);
        });
    }

    @Transactional
    public boolean deleteUserAccount(Long userId) {
        if (!userRepository.existsById(userId)) {
            return false;
        }
        startupProfileRepository.findByUserId(userId).ifPresent(startupProfileRepository::delete);
        userRepository.deleteById(userId);
        return true;
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
