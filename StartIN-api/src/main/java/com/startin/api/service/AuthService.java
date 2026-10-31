package com.startin.api.service;

import com.startin.api.dto.auth.AuthResponseDTO;
import com.startin.api.dto.auth.LoginRequestDTO;
import com.startin.api.dto.auth.PasswordResetRequestDTO;
import com.startin.api.dto.auth.SignupRequestDTO;
import com.startin.api.model.Company;
import com.startin.api.model.StartupProfile;
import com.startin.api.model.User;
import com.startin.api.model.enums.CompanyType;
import com.startin.api.model.enums.UserRole;
import com.startin.api.repository.CompanyRepository;
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
    private final CompanyRepository companyRepository;

    public AuthService(
            UserRepository userRepository,
            StartupProfileRepository startupProfileRepository,
            CompanyRepository companyRepository) {
        this.userRepository = userRepository;
        this.startupProfileRepository = startupProfileRepository;
        this.companyRepository = companyRepository;
    }

    @Transactional
    public AuthResponseDTO signup(SignupRequestDTO request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            return AuthResponseDTO.error("Este e-mail já está cadastrado.");
        }

        UserRole resolvedRole = UserRole.fromString(request.getAccountType());
        boolean isCompany = resolvedRole == UserRole.COMPANY;

        User user = new User();
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPassword(request.getPassword());
        user.setCnpj(request.getCnpj());
        user.setPhone(request.getPhone());
        String defaultName = request.getName() != null && !request.getName().isBlank()
                ? request.getName().trim()
                : request.getEmail().split("@")[0];
        user.setName(defaultName);
        user.setRole(resolvedRole);
        user = userRepository.save(user);

        Long startupProfileId = null;
        Long companyId = null;
        String accountType = isCompany ? "COMPANY" : "STARTUP";

        if (isCompany) {
            Company company = new Company();
            company.setUser(user);
            company.setName(request.getName() != null && !request.getName().isBlank() ? request.getName().trim() : "Minha Empresa");
            company.setType(CompanyType.EMPRESA);
            company.setCnpj(request.getCnpj());
            company.setPhone(request.getPhone());
            company.setSector("Inovação Aberta & B2B");
            company.setLocation("Brasil · Nacional");
            company.setDescription("Organização em busca de startups para provas de conceito (POC) e contratação direta de soluções inovadoras.");
            company.setDemands("Automação de processos, IA aplicada, análise de dados e eficiência operacional.");
            company.setTags("IA, SaaS, B2B, Dados, Automação");
            company.setOperationMode("POC, Compra, POC + Compra");
            company = companyRepository.save(company);
            companyId = company.getId();
        } else {
            StartupProfile profile = new StartupProfile();
            profile.setUser(user);
            profile.setName(request.getName() != null && !request.getName().isBlank() ? request.getName().trim() : "Minha Startup");
            profile.setCnpj(request.getCnpj());
            profile.setPhone(request.getPhone());
            profile.setReceiveAiRecommendations(true);
            profile = startupProfileRepository.save(profile);
            startupProfileId = profile.getId();
        }

        String token = UUID.randomUUID().toString();
        return AuthResponseDTO.ok(token, user.getId(), user.getEmail(), user.getName(), user.getCnpj(), user.getPhone(), user.getAvatarUrl(), startupProfileId, companyId, accountType);
    }

    @Transactional
    public AuthResponseDTO login(LoginRequestDTO request) {
        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail().toLowerCase().trim());
        if (optionalUser.isEmpty()) {
            return AuthResponseDTO.error("E-mail ou senha inválidos.");
        }

        User user = optionalUser.get();
        if (!user.getPassword().equals(request.getPassword())) {
            return AuthResponseDTO.error("E-mail ou senha inválidos.");
        }

        boolean isCompany = user.getRole() == UserRole.COMPANY;
        Optional<Company> existingCompany = companyRepository.findByUserId(user.getId());
        if (existingCompany.isPresent() && user.getRole() == UserRole.COMPANY) {
            isCompany = true;
        }

        Long startupProfileId = null;
        Long companyId = null;
        String avatar = user.getAvatarUrl();

        if (isCompany) {
            Company company = existingCompany.orElseGet(() -> {
                Company c = new Company();
                c.setUser(user);
                c.setName(user.getName() != null ? user.getName() : "Minha Empresa");
                c.setType(CompanyType.EMPRESA);
                c.setCnpj(user.getCnpj());
                c.setSector("Inovação Aberta & B2B");
                c.setLocation("Brasil · Nacional");
                c.setDescription("Organização em busca de startups para provas de conceito (POC) e contratação de soluções.");
                c.setTags("IA, SaaS, B2B, Dados");
                c.setOperationMode("POC, Compra, POC + Compra");
                return companyRepository.save(c);
            });
            companyId = company.getId();
            if (avatar == null) avatar = company.getAvatarUrl();
        } else {
            StartupProfile profile = startupProfileRepository.findByUserId(user.getId())
                    .orElseGet(() -> {
                        StartupProfile newProfile = new StartupProfile();
                        newProfile.setUser(user);
                        newProfile.setName(user.getName() != null ? user.getName() : "Minha Startup");
                        newProfile.setCnpj(user.getCnpj());
                        newProfile.setReceiveAiRecommendations(true);
                        return startupProfileRepository.save(newProfile);
                    });
            startupProfileId = profile.getId();
            if (avatar == null) avatar = profile.getAvatarUrl();
            if (existingCompany.isPresent()) {
                companyId = existingCompany.get().getId();
            }
        }

        String accountType = isCompany ? "COMPANY" : "STARTUP";
        String token = UUID.randomUUID().toString();
        return AuthResponseDTO.ok(token, user.getId(), user.getEmail(), user.getName(), user.getCnpj(), user.getPhone(), avatar, startupProfileId, companyId, accountType);
    }

    public Optional<AuthResponseDTO> getUserInfo(Long userId) {
        return userRepository.findById(userId).map(user -> {
            Optional<StartupProfile> profileOpt = startupProfileRepository.findByUserId(user.getId());
            Optional<Company> companyOpt = companyRepository.findByUserId(user.getId());
            Long profileId = profileOpt.map(StartupProfile::getId).orElse(null);
            Long companyId = companyOpt.map(Company::getId).orElse(null);
            boolean isCompany = user.getRole() == UserRole.COMPANY;
            String accountType = isCompany ? "COMPANY" : "STARTUP";
            String avatar = user.getAvatarUrl() != null
                    ? user.getAvatarUrl()
                    : (isCompany
                        ? companyOpt.map(Company::getAvatarUrl).orElse(null)
                        : profileOpt.map(StartupProfile::getAvatarUrl).orElse(null));
            return AuthResponseDTO.ok(null, user.getId(), user.getEmail(), user.getName(), user.getCnpj(), user.getPhone(), avatar, profileId, companyId, accountType);
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
            if (payload.get("phone") != null) {
                user.setPhone(payload.get("phone").trim());
            }
            if (payload.containsKey("avatarUrl")) {
                user.setAvatarUrl(payload.get("avatarUrl"));
            }
            if (payload.get("accountType") != null && !payload.get("accountType").isBlank()) {
                user.setRole(UserRole.fromString(payload.get("accountType")));
            }
            user = userRepository.save(user);

            Optional<StartupProfile> profileOpt = startupProfileRepository.findByUserId(user.getId());
            if (profileOpt.isPresent()) {
                StartupProfile p = profileOpt.get();
                if (payload.containsKey("avatarUrl")) {
                    p.setAvatarUrl(payload.get("avatarUrl"));
                }
                if (payload.get("phone") != null) {
                    p.setPhone(payload.get("phone").trim());
                }
                startupProfileRepository.save(p);
            }

            Optional<Company> companyOpt = companyRepository.findByUserId(user.getId());
            if (user.getRole() == UserRole.COMPANY && companyOpt.isEmpty()) {
                Company c = new Company();
                c.setUser(user);
                c.setName(user.getName() != null ? user.getName() : "Minha Empresa");
                c.setType(CompanyType.EMPRESA);
                c.setCnpj(user.getCnpj());
                c.setPhone(user.getPhone());
                c.setAvatarUrl(user.getAvatarUrl());
                c.setSector("Inovação Aberta & B2B");
                c.setLocation("Brasil · Nacional");
                c.setDescription("Organização em busca de startups para provas de conceito (POC) e compras.");
                c.setTags("IA, SaaS, B2B, Dados");
                c.setOperationMode("POC, Compra, POC + Compra");
                companyOpt = Optional.of(companyRepository.save(c));
            } else if (companyOpt.isPresent()) {
                Company c = companyOpt.get();
                if (payload.containsKey("avatarUrl")) {
                    c.setAvatarUrl(payload.get("avatarUrl"));
                }
                if (payload.get("phone") != null) {
                    c.setPhone(payload.get("phone").trim());
                }
                companyRepository.save(c);
            }

            Long profileId = profileOpt.map(StartupProfile::getId).orElse(null);
            Long companyId = companyOpt.map(Company::getId).orElse(null);
            String accountType = user.getRole() == UserRole.COMPANY ? "COMPANY" : "STARTUP";
            return AuthResponseDTO.ok(null, user.getId(), user.getEmail(), user.getName(), user.getCnpj(), user.getPhone(), user.getAvatarUrl(), profileId, companyId, accountType);
        });
    }

    @Transactional
    public boolean deleteUserAccount(Long userId) {
        if (!userRepository.existsById(userId)) {
            return false;
        }
        startupProfileRepository.findByUserId(userId).ifPresent(startupProfileRepository::delete);
        companyRepository.findByUserId(userId).ifPresent(companyRepository::delete);
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
