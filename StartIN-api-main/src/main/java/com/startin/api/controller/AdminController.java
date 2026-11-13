package com.startin.api.controller;

import com.startin.api.dto.opportunity.OpportunityDTO;
import com.startin.api.model.*;
import com.startin.api.model.enums.OpportunityStatus;
import com.startin.api.model.enums.OpportunityType;
import com.startin.api.model.enums.UserRole;
import com.startin.api.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Administração Geral", description = "Painel de moderação e controle total de usuários, oportunidades e processos")
public class AdminController {

    private final UserRepository userRepository;
    private final StartupProfileRepository startupProfileRepository;
    private final CompanyRepository companyRepository;
    private final ServiceProviderRepository serviceProviderRepository;
    private final OpportunityRepository opportunityRepository;
    private final MatchResultRepository matchResultRepository;
    private final ServiceConnectionRepository serviceConnectionRepository;

    public AdminController(
            UserRepository userRepository,
            StartupProfileRepository startupProfileRepository,
            CompanyRepository companyRepository,
            ServiceProviderRepository serviceProviderRepository,
            OpportunityRepository opportunityRepository,
            MatchResultRepository matchResultRepository,
            ServiceConnectionRepository serviceConnectionRepository) {
        this.userRepository = userRepository;
        this.startupProfileRepository = startupProfileRepository;
        this.companyRepository = companyRepository;
        this.serviceProviderRepository = serviceProviderRepository;
        this.opportunityRepository = opportunityRepository;
        this.matchResultRepository = matchResultRepository;
        this.serviceConnectionRepository = serviceConnectionRepository;
    }

    @GetMapping("/overview")
    @Operation(summary = "Resumo geral do sistema para o moderador/administrador")
    public ResponseEntity<Map<String, Object>> getOverview() {
        List<User> users = userRepository.findAll();
        List<Opportunity> opportunities = opportunityRepository.findAll();
        long startupsCount = users.stream().filter(u -> u.getRole() == UserRole.STARTUP || u.getRole() == UserRole.USER).count();
        long companiesCount = users.stream().filter(u -> u.getRole() == UserRole.COMPANY).count();
        long providersCount = users.stream().filter(u -> u.getRole() == UserRole.PROVIDER).count();
        long proUsersCount = users.stream().filter(u -> "PRO".equalsIgnoreCase(u.getPlan())).count();
        long freeUsersCount = users.size() - proUsersCount;

        long companyOpps = opportunities.stream().filter(o -> o.getCompany() != null || o.getType() != OpportunityType.SERVICO).count();
        long startupOpps = opportunities.stream().filter(o -> o.getStartup() != null || o.getType() == OpportunityType.SERVICO).count();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("totalUsers", users.size());
        res.put("startupsCount", startupsCount);
        res.put("companiesCount", companiesCount);
        res.put("providersCount", providersCount);
        res.put("proUsersCount", proUsersCount);
        res.put("freeUsersCount", freeUsersCount);
        res.put("totalOpportunities", opportunities.size());
        res.put("companyOpportunitiesCount", companyOpps);
        res.put("startupOpportunitiesCount", startupOpps);
        res.put("totalMatches", matchResultRepository.count());
        res.put("totalConnections", serviceConnectionRepository.count());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/users")
    @Operation(summary = "Lista todos os usuários do sistema com dados de perfil e assinatura")
    public ResponseEntity<List<Map<String, Object>>> listUsers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> out = new ArrayList<>();

        for (User u : users) {
            Optional<StartupProfile> stOpt = startupProfileRepository.findByUserId(u.getId());
            Optional<Company> coOpt = companyRepository.findByUserId(u.getId());
            Optional<ServiceProvider> prOpt = serviceProviderRepository.findByUserId(u.getId());

            String accountType = "STARTUP";
            if (u.getRole() == UserRole.ADMIN) {
                accountType = "ADMIN";
            } else if (u.getRole() == UserRole.PROVIDER || prOpt.isPresent()) {
                accountType = "PROVIDER";
            } else if (u.getRole() == UserRole.COMPANY || coOpt.isPresent()) {
                accountType = "COMPANY";
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", u.getId());
            item.put("name", u.getName());
            item.put("email", u.getEmail());
            item.put("cnpj", u.getCnpj());
            item.put("phone", u.getPhone());
            item.put("role", u.getRole() != null ? u.getRole().name() : "STARTUP");
            item.put("accountType", accountType);
            item.put("plan", u.getPlan());
            item.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : null);

            if (stOpt.isPresent()) {
                StartupProfile st = stOpt.get();
                item.put("startupProfileId", st.getId());
                item.put("profileName", st.getName());
                item.put("currentStage", st.getCurrentStage() != null ? st.getCurrentStage() : "Validação");
                item.put("sectorOrArea", st.getMainArea());
            } else if (coOpt.isPresent()) {
                Company co = coOpt.get();
                item.put("companyId", co.getId());
                item.put("profileName", co.getName());
                item.put("currentStage", "Corporativo");
                item.put("sectorOrArea", co.getSector());
            } else if (prOpt.isPresent()) {
                ServiceProvider pr = prOpt.get();
                item.put("providerId", pr.getId());
                item.put("profileName", pr.getName());
                item.put("currentStage", pr.getAvailability());
                item.put("sectorOrArea", pr.getCategory());
            } else {
                item.put("profileName", u.getName());
                item.put("currentStage", accountType);
                item.put("sectorOrArea", "Sistema");
            }
            out.add(item);
        }

        out.sort((a, b) -> Long.compare(
                b.get("id") instanceof Number ? ((Number) b.get("id")).longValue() : 0L,
                a.get("id") instanceof Number ? ((Number) a.get("id")).longValue() : 0L
        ));
        return ResponseEntity.ok(out);
    }

    @PutMapping("/users/{id}")
    @Transactional
    @Operation(summary = "Altera dados, perfil, estágio ou plano (FREE/PRO) de qualquer usuário")
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + id));

        if (payload.get("name") != null && !String.valueOf(payload.get("name")).isBlank()) {
            user.setName(String.valueOf(payload.get("name")).trim());
        }
        if (payload.get("email") != null && !String.valueOf(payload.get("email")).isBlank()) {
            user.setEmail(String.valueOf(payload.get("email")).trim().toLowerCase());
        }
        if (payload.get("cnpj") != null) {
            user.setCnpj(String.valueOf(payload.get("cnpj")).trim());
        }
        if (payload.get("phone") != null) {
            user.setPhone(String.valueOf(payload.get("phone")).trim());
        }
        if (payload.get("password") != null && !String.valueOf(payload.get("password")).isBlank()) {
            user.setPassword(String.valueOf(payload.get("password")).trim());
        }
        if (payload.get("accountType") != null && !String.valueOf(payload.get("accountType")).isBlank()) {
            user.setRole(UserRole.fromString(String.valueOf(payload.get("accountType")).trim()));
        } else if (payload.get("role") != null && !String.valueOf(payload.get("role")).isBlank()) {
            user.setRole(UserRole.fromString(String.valueOf(payload.get("role")).trim()));
        }
        if (payload.get("plan") != null && !String.valueOf(payload.get("plan")).isBlank()) {
            user.setPlan(String.valueOf(payload.get("plan")).trim().toUpperCase());
        }

        user = userRepository.save(user);
        boolean isPro = "PRO".equalsIgnoreCase(user.getPlan());

        Optional<StartupProfile> stOpt = startupProfileRepository.findByUserId(user.getId());
        if (stOpt.isPresent()) {
            StartupProfile st = stOpt.get();
            if (payload.get("profileName") != null && !String.valueOf(payload.get("profileName")).isBlank()) {
                st.setName(String.valueOf(payload.get("profileName")).trim());
            } else if (payload.get("name") != null && !String.valueOf(payload.get("name")).isBlank()) {
                st.setName(String.valueOf(payload.get("name")).trim());
            }
            if (payload.get("currentStage") != null && !String.valueOf(payload.get("currentStage")).isBlank()) {
                String stage = String.valueOf(payload.get("currentStage")).trim();
                st.setCurrentStage(stage);
                if (stage.toLowerCase().contains("tra")) {
                    st.setTractionAttested(true);
                }
            }
            if (payload.get("sectorOrArea") != null && !String.valueOf(payload.get("sectorOrArea")).isBlank()) {
                st.setMainArea(String.valueOf(payload.get("sectorOrArea")).trim());
            }
            startupProfileRepository.save(st);

            for (Opportunity o : opportunityRepository.findByStartupIdOrderByIdDesc(st.getId())) {
                o.setProMember(isPro);
                opportunityRepository.save(o);
            }
        }

        Optional<Company> coOpt = companyRepository.findByUserId(user.getId());
        if (coOpt.isPresent()) {
            Company co = coOpt.get();
            if (payload.get("profileName") != null && !String.valueOf(payload.get("profileName")).isBlank()) {
                co.setName(String.valueOf(payload.get("profileName")).trim());
            } else if (payload.get("name") != null && !String.valueOf(payload.get("name")).isBlank()) {
                co.setName(String.valueOf(payload.get("name")).trim());
            }
            if (payload.get("sectorOrArea") != null && !String.valueOf(payload.get("sectorOrArea")).isBlank()) {
                co.setSector(String.valueOf(payload.get("sectorOrArea")).trim());
            }
            companyRepository.save(co);

            for (Opportunity o : opportunityRepository.findByCompanyIdOrderByIdDesc(co.getId())) {
                o.setProMember(isPro);
                opportunityRepository.save(o);
            }
        }

        Optional<ServiceProvider> prOpt = serviceProviderRepository.findByUserId(user.getId());
        if (prOpt.isPresent()) {
            ServiceProvider pr = prOpt.get();
            if (payload.get("profileName") != null && !String.valueOf(payload.get("profileName")).isBlank()) {
                pr.setName(String.valueOf(payload.get("profileName")).trim());
            } else if (payload.get("name") != null && !String.valueOf(payload.get("name")).isBlank()) {
                pr.setName(String.valueOf(payload.get("name")).trim());
            }
            if (payload.get("sectorOrArea") != null && !String.valueOf(payload.get("sectorOrArea")).isBlank()) {
                pr.setCategory(String.valueOf(payload.get("sectorOrArea")).trim());
            }
            serviceProviderRepository.save(pr);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("id", user.getId());
        out.put("name", user.getName());
        out.put("email", user.getEmail());
        out.put("plan", user.getPlan());
        out.put("role", user.getRole().name());
        return ResponseEntity.ok(out);
    }

    @DeleteMapping("/users/{id}")
    @Transactional
    @Operation(summary = "Exclui um usuário e limpa seus perfis, oportunidades e processos vinculados")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        startupProfileRepository.findByUserId(id).ifPresent(st -> {
            List<MatchResult> matches = matchResultRepository.findByStartupProfileIdOrderByMatchScoreDesc(st.getId());
            if (!matches.isEmpty()) matchResultRepository.deleteAll(matches);

            List<ServiceConnection> conns = serviceConnectionRepository.findByStartupIdOrderByIdDesc(st.getId());
            if (!conns.isEmpty()) serviceConnectionRepository.deleteAll(conns);

            List<Opportunity> stOpps = opportunityRepository.findByStartupIdOrderByIdDesc(st.getId());
            for (Opportunity opp : stOpps) {
                deleteOpportunityCleanly(opp.getId());
            }
            startupProfileRepository.delete(st);
        });

        companyRepository.findByUserId(id).ifPresent(co -> {
            List<Opportunity> coOpps = opportunityRepository.findByCompanyIdOrderByIdDesc(co.getId());
            for (Opportunity opp : coOpps) {
                deleteOpportunityCleanly(opp.getId());
            }
            companyRepository.delete(co);
        });

        serviceProviderRepository.findByUserId(id).ifPresent(pr -> {
            List<ServiceConnection> conns = serviceConnectionRepository.findByProviderIdOrderByIdDesc(pr.getId());
            if (!conns.isEmpty()) serviceConnectionRepository.deleteAll(conns);
            serviceProviderRepository.delete(pr);
        });

        userRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("deleted", true, "id", id));
    }

    @GetMapping("/opportunities")
    @Operation(summary = "Lista todas as oportunidades (de empresas, editais, eventos e de startups)")
    public ResponseEntity<List<OpportunityDTO>> listAllOpportunities() {
        List<OpportunityDTO> list = opportunityRepository.findAll().stream()
                .map(OpportunityDTO::fromEntity)
                .sorted((a, b) -> Long.compare(
                        b.getId() != null ? b.getId() : 0L,
                        a.getId() != null ? a.getId() : 0L
                ))
                .toList();
        return ResponseEntity.ok(list);
    }

    @PutMapping("/opportunities/{id}")
    @Transactional
    @Operation(summary = "Altera qualquer oportunidade do sistema sem restrição de proprietário")
    public ResponseEntity<OpportunityDTO> updateOpportunity(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Opportunity opp = opportunityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Oportunidade não encontrada: " + id));

        if (payload.get("title") != null && !String.valueOf(payload.get("title")).isBlank()) {
            opp.setTitle(String.valueOf(payload.get("title")).trim());
        }
        if (payload.get("description") != null && !String.valueOf(payload.get("description")).isBlank()) {
            String desc = String.valueOf(payload.get("description")).trim();
            opp.setDescription(desc);
            opp.setShortDescription(desc.length() > 80 ? desc.substring(0, 80) + "..." : desc);
        }
        if (payload.get("type") != null && !String.valueOf(payload.get("type")).isBlank()) {
            opp.setType(OpportunityType.fromString(String.valueOf(payload.get("type")).trim()));
        }
        if (payload.get("status") != null && !String.valueOf(payload.get("status")).isBlank()) {
            opp.setStatus(OpportunityStatus.fromString(String.valueOf(payload.get("status")).trim()));
        }
        if (payload.get("tags") != null) {
            opp.setTags(String.valueOf(payload.get("tags")));
        }
        if (payload.get("meta") != null) {
            opp.setMetaInfo(String.valueOf(payload.get("meta")));
        }
        if (payload.get("externalUrl") != null) {
            opp.setExternalUrl(String.valueOf(payload.get("externalUrl")));
        }
        if (payload.get("targetCategory") != null) {
            opp.setTargetCategory(String.valueOf(payload.get("targetCategory")));
        }
        if (payload.get("proMember") != null) {
            opp.setProMember(Boolean.parseBoolean(String.valueOf(payload.get("proMember"))));
        }
        if (payload.get("match") instanceof Number) {
            opp.setMatchScore(((Number) payload.get("match")).intValue());
        }

        Opportunity saved = opportunityRepository.save(opp);
        return ResponseEntity.ok(OpportunityDTO.fromEntity(saved));
    }

    @DeleteMapping("/opportunities/{id}")
    @Transactional
    @Operation(summary = "Remove qualquer oportunidade do sistema")
    public ResponseEntity<Map<String, Object>> deleteOpportunity(@PathVariable Long id) {
        if (!opportunityRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        deleteOpportunityCleanly(id);
        return ResponseEntity.ok(Map.of("deleted", true, "id", id));
    }

    @GetMapping("/processes")
    @Operation(summary = "Lista todos os processos ativos (Matches Startup-Empresa e Conexões Startup-Prestador)")
    public ResponseEntity<List<Map<String, Object>>> listProcesses() {
        List<Map<String, Object>> processes = new ArrayList<>();

        for (ServiceConnection conn : serviceConnectionRepository.findAll()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", conn.getId());
            item.put("processType", "CONNECTION");
            item.put("title", (conn.getStartup() != null ? conn.getStartup().getName() : "Startup")
                    + " <-> "
                    + (conn.getProvider() != null ? conn.getProvider().getName() : "Prestador"));
            item.put("sourceName", conn.getStartup() != null ? conn.getStartup().getName() : "Startup");
            item.put("targetName", conn.getProvider() != null ? conn.getProvider().getName() : "Prestador");
            item.put("category", conn.getProvider() != null ? conn.getProvider().getCategory() : "Serviço");
            item.put("status", conn.getStatus() != null ? conn.getStatus() : "PENDING");
            item.put("matchScore", conn.getMatchScore() != null ? conn.getMatchScore() : 88);
            item.put("message", conn.getMessage());
            item.put("createdAt", conn.getCreatedAt() != null ? conn.getCreatedAt().toString() : null);
            processes.add(item);
        }

        for (MatchResult m : matchResultRepository.findAll()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", m.getId());
            item.put("processType", "MATCH");
            String stName = m.getStartupProfile() != null ? m.getStartupProfile().getName() : "Startup";
            String oppTitle = m.getOpportunity() != null ? m.getOpportunity().getTitle() : "Oportunidade";
            item.put("title", stName + " -> " + oppTitle);
            item.put("sourceName", stName);
            item.put("targetName", oppTitle);
            item.put("category", m.getOpportunity() != null && m.getOpportunity().getType() != null ? m.getOpportunity().getType().name() : "MATCH");
            item.put("status", m.getStatus() != null ? m.getStatus().name() : "RECOMENDADA");
            item.put("matchScore", m.getMatchScore() != null ? m.getMatchScore() : 85);
            item.put("message", m.getJustification());
            item.put("createdAt", m.getCreatedAt() != null ? m.getCreatedAt().toString() : null);
            processes.add(item);
        }

        return ResponseEntity.ok(processes);
    }

    @DeleteMapping("/processes/{type}/{id}")
    @Transactional
    @Operation(summary = "Remove um processo (MATCH ou CONNECTION) pelo ID")
    public ResponseEntity<Map<String, Object>> deleteProcess(@PathVariable String type, @PathVariable Long id) {
        if ("CONNECTION".equalsIgnoreCase(type)) {
            if (!serviceConnectionRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            serviceConnectionRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("deleted", true, "type", "CONNECTION", "id", id));
        } else {
            if (!matchResultRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            matchResultRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("deleted", true, "type", "MATCH", "id", id));
        }
    }

    private void deleteOpportunityCleanly(Long oppId) {
        List<MatchResult> matches = matchResultRepository.findAll().stream()
                .filter(m -> m.getOpportunity() != null && Objects.equals(m.getOpportunity().getId(), oppId))
                .toList();
        if (!matches.isEmpty()) {
            matchResultRepository.deleteAll(matches);
        }
        opportunityRepository.deleteById(oppId);
    }
}
