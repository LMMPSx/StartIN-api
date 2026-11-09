package com.startin.api.service;

import com.startin.api.dto.provider.ServiceProviderDTO;
import com.startin.api.model.*;
import com.startin.api.model.enums.OpportunityStatus;
import com.startin.api.model.enums.OpportunityType;
import com.startin.api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ServiceProviderService {

    private final ServiceProviderRepository providerRepository;
    private final ServiceConnectionRepository connectionRepository;
    private final StartupProfileRepository startupProfileRepository;
    private final UserRepository userRepository;
    private final OpportunityRepository opportunityRepository;
    private final ProviderSavedStartupRepository savedStartupRepository;
    private final NotificationRepository notificationRepository;
    private final AiIntegrationService aiIntegrationService;

    public ServiceProviderService(
            ServiceProviderRepository providerRepository,
            ServiceConnectionRepository connectionRepository,
            StartupProfileRepository startupProfileRepository,
            UserRepository userRepository,
            OpportunityRepository opportunityRepository,
            ProviderSavedStartupRepository savedStartupRepository,
            NotificationRepository notificationRepository,
            AiIntegrationService aiIntegrationService) {
        this.providerRepository = providerRepository;
        this.connectionRepository = connectionRepository;
        this.startupProfileRepository = startupProfileRepository;
        this.userRepository = userRepository;
        this.opportunityRepository = opportunityRepository;
        this.savedStartupRepository = savedStartupRepository;
        this.notificationRepository = notificationRepository;
        this.aiIntegrationService = aiIntegrationService;
    }

    public List<ServiceProviderDTO> listProviders(String category, String query, Long startupId, Long userId) {
        List<ServiceProvider> list;
        boolean hasCategory = category != null && !category.isBlank() && !"Todos".equalsIgnoreCase(category);
        boolean hasQuery = query != null && !query.isBlank();

        if (hasCategory && hasQuery) {
            String q = query.trim().toLowerCase();
            list = providerRepository.findByCategoryIgnoreCaseOrderByMatchScoreDesc(category.trim())
                    .stream()
                    .filter(p -> (p.getName() != null && p.getName().toLowerCase().contains(q))
                            || (p.getMainService() != null && p.getMainService().toLowerCase().contains(q))
                            || (p.getDescription() != null && p.getDescription().toLowerCase().contains(q))
                            || (p.getToolsAndSkills() != null && p.getToolsAndSkills().toLowerCase().contains(q))
                            || (p.getTags() != null && p.getTags().toLowerCase().contains(q)))
                    .collect(Collectors.toList());
        } else if (hasCategory) {
            list = providerRepository.findByCategoryIgnoreCaseOrderByMatchScoreDesc(category.trim());
        } else if (hasQuery) {
            list = providerRepository.searchProviders(query.trim());
        } else {
            list = providerRepository.findAllByOrderByMatchScoreDesc();
        }

        StartupProfile startup = resolveStartupProfile(startupId, userId);
        List<Opportunity> startupServiceDemands = startup != null && startup.getId() != null
                ? opportunityRepository.findByStartupIdOrderByIdDesc(startup.getId()).stream()
                        .filter(o -> o.getType() == OpportunityType.SERVICO && o.getStatus() == OpportunityStatus.PUBLICADA)
                        .toList()
                : List.of();

        List<ServiceProviderDTO> evaluated = aiIntegrationService.evaluateStartupToProvidersMatch(startup, startupServiceDemands, list);
        if (startup != null && startup.getId() != null) {
            List<ServiceConnection> connections = connectionRepository.findByStartupIdOrderByCreatedAtDesc(startup.getId());
            Map<Long, ServiceConnection> connByProviderId = new HashMap<>();
            for (ServiceConnection c : connections) {
                if (c.getProvider() != null && c.getProvider().getId() != null) {
                    connByProviderId.putIfAbsent(c.getProvider().getId(), c);
                }
            }
            for (ServiceProviderDTO p : evaluated) {
                if (p.getId() != null && connByProviderId.containsKey(p.getId())) {
                    ServiceConnection c = connByProviderId.get(p.getId());
                    p.setHasConnection(true);
                    p.setConnectionId(c.getId());
                    p.setConnectionStatus(c.getStatus());
                    p.setConnectionSenderType(c.getSenderType());
                } else {
                    p.setHasConnection(false);
                }
            }
        }
        return evaluated;
    }

    public List<ServiceProviderDTO> listProviders(String category, String query) {
        return listProviders(category, query, null, null);
    }

    public ServiceProviderDTO getProviderById(Long id, Long startupId, Long userId) {
        ServiceProvider entity = providerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Prestador não encontrado com id: " + id));
        StartupProfile startup = resolveStartupProfile(startupId, userId);
        List<Opportunity> demands = startup != null && startup.getId() != null
                ? opportunityRepository.findByStartupIdOrderByIdDesc(startup.getId()).stream()
                        .filter(o -> o.getType() == OpportunityType.SERVICO && o.getStatus() == OpportunityStatus.PUBLICADA)
                        .toList()
                : List.of();
        List<ServiceProviderDTO> evaluated = aiIntegrationService.evaluateStartupToProvidersMatch(startup, demands, List.of(entity));
        ServiceProviderDTO dto = !evaluated.isEmpty() ? evaluated.get(0) : ServiceProviderDTO.fromEntity(entity);
        if (dto.getMatchReasons() == null || dto.getMatchReasons().isEmpty()) {
            dto.setMatchReasons(buildDefaultProviderReasons(entity));
        }
        if (startup != null && startup.getId() != null && entity.getId() != null) {
            Optional<ServiceConnection> conn = connectionRepository.findByStartupIdAndProviderId(startup.getId(), entity.getId());
            if (conn.isPresent()) {
                ServiceConnection c = conn.get();
                dto.setHasConnection(true);
                dto.setConnectionId(c.getId());
                dto.setConnectionStatus(c.getStatus());
                dto.setConnectionSenderType(c.getSenderType());
            } else {
                dto.setHasConnection(false);
            }
        }
        return dto;
    }

    public ServiceProviderDTO getProviderById(Long id) {
        return getProviderById(id, null, null);
    }

    public ServiceProviderDTO getProviderByUserId(Long userId) {
        return providerRepository.findByUserId(userId)
                .map(entity -> {
                    ServiceProviderDTO dto = ServiceProviderDTO.fromEntity(entity);
                    dto.setMatchReasons(buildDefaultProviderReasons(entity));
                    return dto;
                })
                .orElse(null);
    }

    @Transactional
    public ServiceProviderDTO createProvider(ServiceProviderDTO dto) {
        ServiceProvider entity = null;
        if (dto.getId() != null) {
            entity = providerRepository.findById(dto.getId()).orElse(null);
        }
        if (entity == null && dto.getUserId() != null) {
            entity = providerRepository.findByUserId(dto.getUserId()).orElse(null);
        }
        if (entity == null) {
            entity = new ServiceProvider();
        }
        if (dto.getUserId() != null && entity.getUser() == null) {
            userRepository.findById(dto.getUserId()).ifPresent(entity::setUser);
        }
        applyDtoToEntity(dto, entity);
        return ServiceProviderDTO.fromEntity(providerRepository.save(entity));
    }

    @Transactional
    public ServiceProviderDTO updateProvider(Long id, ServiceProviderDTO dto) {
        ServiceProvider entity = providerRepository.findById(id)
                .or(() -> dto.getUserId() != null ? providerRepository.findByUserId(dto.getUserId()) : Optional.empty())
                .orElseGet(ServiceProvider::new);
        if (dto.getUserId() != null && entity.getUser() == null) {
            userRepository.findById(dto.getUserId()).ifPresent(entity::setUser);
        }
        applyDtoToEntity(dto, entity);
        return ServiceProviderDTO.fromEntity(providerRepository.save(entity));
    }

    /**
     * Retorna todas as OPORTUNIDADES lançadas pelas Startups em estágio de Tração avaliadas
     * com compatibilidade real pela IA contra o perfil do Prestador de Serviços logado,
     * incluindo status de favoritado (saved) e conexão ativa.
     */
    public List<Map<String, Object>> getMatchingStartupsForProvider(Long providerId, Long userId, boolean onlySaved) {
        ServiceProvider provider = resolveProvider(providerId, userId);
        Set<Long> savedIds = getSavedStartupIdSet(provider);

        List<StartupProfile> allStartups = startupProfileRepository.findAll();
        Map<Long, StartupProfile> tractionStartupsById = allStartups.stream()
                .filter(this::isTractionStartup)
                .collect(Collectors.toMap(StartupProfile::getId, s -> s, (s1, s2) -> s1));

        List<Opportunity> allServiceDemands = opportunityRepository.findByType(OpportunityType.SERVICO);
        List<Opportunity> candidateOpportunities = new ArrayList<>();
        Set<Long> startupsWithExplicitOpportunities = new HashSet<>();

        for (Opportunity opp : allServiceDemands) {
            if (opp.getStatus() == OpportunityStatus.PUBLICADA
                    && opp.getStartup() != null
                    && opp.getStartup().getId() != null
                    && tractionStartupsById.containsKey(opp.getStartup().getId())) {
                candidateOpportunities.add(opp);
                startupsWithExplicitOpportunities.add(opp.getStartup().getId());
            }
        }

        // Se houver startup em Tração sem oportunidade cadastrada formalmente, sintetiza uma demanda técnica
        for (StartupProfile st : tractionStartupsById.values()) {
            if (!startupsWithExplicitOpportunities.contains(st.getId())) {
                Opportunity synth = new Opportunity();
                synth.setId(100000L + st.getId());
                synth.setStartup(st);
                synth.setType(OpportunityType.SERVICO);
                synth.setStatus(OpportunityStatus.PUBLICADA);
                synth.setTitle("Demanda Técnica de " + (st.getMainArea() != null ? st.getMainArea() : "Inovação & Tecnologia"));
                synth.setDescription(st.getProblem() != null && !st.getProblem().isBlank()
                        ? st.getProblem()
                        : (st.getDescription() != null && !st.getDescription().isBlank()
                                ? st.getDescription()
                                : "Demanda de serviços especializados para startup em estágio de Tração."));
                synth.setTags(st.getTechnologies() != null && !st.getTechnologies().isBlank()
                        ? st.getTechnologies()
                        : (st.getAreasOfInterest() != null ? st.getAreasOfInterest() : "Inovação, Escalabilidade"));
                synth.setTargetCategory(st.getMainArea() != null ? st.getMainArea() : "Tecnologia");
                candidateOpportunities.add(synth);
            }
        }

        List<Map<String, Object>> evaluated = aiIntegrationService.evaluateProviderToOpportunitiesMatch(
                provider,
                candidateOpportunities,
                savedIds
        );

        List<ServiceConnection> connections = provider != null && provider.getId() != null
                ? connectionRepository.findByProviderIdOrderByCreatedAtDesc(provider.getId())
                : List.of();
        Map<Long, ServiceConnection> connectionByStartupId = new HashMap<>();
        for (ServiceConnection conn : connections) {
            if (conn.getStartup() != null && conn.getStartup().getId() != null) {
                connectionByStartupId.putIfAbsent(conn.getStartup().getId(), conn);
            }
        }

        for (Map<String, Object> item : evaluated) {
            Long sId = null;
            if (item.get("startupId") != null) {
                try {
                    sId = Long.valueOf(item.get("startupId").toString());
                } catch (Exception ignored) {}
            }
            if (sId == null && item.get("id") != null) {
                try {
                    sId = Long.valueOf(item.get("id").toString());
                } catch (Exception ignored) {}
            }
            if (sId != null && tractionStartupsById.containsKey(sId)) {
                StartupProfile stOwner = tractionStartupsById.get(sId);
                boolean isPro = stOwner.getUser() != null && "PRO".equalsIgnoreCase(stOwner.getUser().getPlan());
                item.put("proMember", isPro);
            } else {
                item.put("proMember", Boolean.TRUE.equals(item.get("proMember")));
            }
            if (sId != null && connectionByStartupId.containsKey(sId)) {
                ServiceConnection conn = connectionByStartupId.get(sId);
                item.put("hasConnection", true);
                item.put("connectionId", conn.getId());
                item.put("connectionStatus", conn.getStatus());
                item.put("connectionSenderType", conn.getSenderType());
                item.put("connectionCreatedAt", conn.getCreatedAt() != null ? conn.getCreatedAt().toString() : null);
            } else {
                item.put("hasConnection", false);
                item.put("connectionId", null);
                item.put("connectionStatus", null);
                item.put("connectionSenderType", null);
                item.put("connectionCreatedAt", null);
            }
        }

        // Impulsionamento Pro: oportunidades lançadas por startups assinantes do Plano Pro ficam na frente
        evaluated.sort((a, b) -> {
            boolean aPro = Boolean.TRUE.equals(a.get("proMember"));
            boolean bPro = Boolean.TRUE.equals(b.get("proMember"));
            if (aPro != bPro) {
                return aPro ? -1 : 1;
            }
            int aMatch = a.get("match") instanceof Number ? ((Number) a.get("match")).intValue() : 0;
            int bMatch = b.get("match") instanceof Number ? ((Number) b.get("match")).intValue() : 0;
            return Integer.compare(bMatch, aMatch);
        });

        if (onlySaved) {
            return evaluated.stream()
                    .filter(item -> Boolean.TRUE.equals(item.get("saved")))
                    .collect(Collectors.toList());
        }
        return evaluated;
    }

    /**
     * Alterna o salvamento/favorito de uma Startup de Tração para o Prestador no banco MySQL
     * (tabela provider_saved_startups + campo saved_startup_ids).
     */
    @Transactional
    public Map<String, Object> toggleSavedStartup(Long providerId, Long userId, Long startupId) {
        ServiceProvider provider = resolveProvider(providerId, userId);
        if (provider == null || provider.getId() == null) {
            throw new IllegalArgumentException("Perfil de prestador não encontrado para salvar startup.");
        }
        if (startupId == null) {
            throw new IllegalArgumentException("ID da startup é obrigatório.");
        }

        Long pid = provider.getId();
        Optional<ProviderSavedStartup> existing = savedStartupRepository.findByProviderIdAndStartupId(pid, startupId);
        boolean nowSaved;
        if (existing.isPresent()) {
            savedStartupRepository.delete(existing.get());
            nowSaved = false;
        } else {
            savedStartupRepository.save(new ProviderSavedStartup(pid, startupId));
            nowSaved = true;
        }

        List<Long> updatedIds = savedStartupRepository.findByProviderIdOrderByCreatedAtDesc(pid)
                .stream()
                .map(ProviderSavedStartup::getStartupId)
                .toList();

        provider.setSavedStartupIds(updatedIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        providerRepository.save(provider);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("saved", nowSaved);
        res.put("id", startupId);
        res.put("startupId", startupId);
        res.put("opportunityId", startupId);
        res.put("savedStartupIds", updatedIds);
        res.put("savedStartupsCount", updatedIds.size());
        return res;
    }

    /**
     * Resumo 100% real do Dashboard do Prestador integrado ao MySQL e à IA (zero dados mockados).
     */
    public Map<String, Object> getProviderDashboardSummary(Long providerId, Long userId) {
        ServiceProvider provider = resolveProvider(providerId, userId);
        Long resolvedId = provider != null ? provider.getId() : null;

        Set<Long> savedIds = getSavedStartupIdSet(provider);
        List<Map<String, Object>> matchedOpportunities = getMatchingStartupsForProvider(resolvedId, userId, false);

        List<ServiceConnection> connections = resolvedId != null
                ? connectionRepository.findByProviderIdOrderByCreatedAtDesc(resolvedId)
                : List.of();

        long receivedPendingCount = connections.stream()
                .filter(c -> "PENDING".equalsIgnoreCase(c.getStatus()) && "STARTUP".equalsIgnoreCase(c.getSenderType()))
                .count();

        long activeConnectionsCount = connections.stream()
                .filter(c -> "ACCEPTED".equalsIgnoreCase(c.getStatus()))
                .count();

        List<Map<String, Object>> recommendedOpportunities = matchedOpportunities.stream()
                .limit(4)
                .collect(Collectors.toList());

        List<Map<String, Object>> recentActivities = buildRealRecentActivities(provider, connections);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("providerId", resolvedId);
        summary.put("providerName", provider != null && provider.getName() != null ? provider.getName() : "Prestador");
        summary.put("providerCategory", provider != null && provider.getCategory() != null ? provider.getCategory() : "Serviços Especializados");
        summary.put("savedStartupsCount", savedIds.size());
        summary.put("suggestedStartupsCount", matchedOpportunities.size());
        summary.put("suggestedOpportunitiesCount", matchedOpportunities.size());
        summary.put("suggestedConnectionsCount", matchedOpportunities.size());
        summary.put("receivedRequestsCount", receivedPendingCount);
        summary.put("pendingRequestsCount", receivedPendingCount);
        summary.put("activeConnectionsCount", activeConnectionsCount);
        summary.put("recommendedStartups", recommendedOpportunities);
        summary.put("recommendedOpportunities", recommendedOpportunities);
        summary.put("recentActivities", recentActivities);
        return summary;
    }

    public Map<String, Object> getProviderDashboardSummary(Long providerId) {
        return getProviderDashboardSummary(providerId, null);
    }

    private List<Map<String, Object>> buildRealRecentActivities(ServiceProvider provider, List<ServiceConnection> connections) {
        List<Map<String, Object>> activities = new ArrayList<>();

        for (ServiceConnection conn : connections) {
            if (activities.size() >= 6) break;
            String stName = conn.getStartup() != null && conn.getStartup().getName() != null
                    ? conn.getStartup().getName() : "Startup";
            String status = conn.getStatus() != null ? conn.getStatus().toUpperCase() : "PENDING";
            String sender = conn.getSenderType() != null ? conn.getSenderType().toUpperCase() : "STARTUP";

            Map<String, Object> act = new LinkedHashMap<>();
            if ("ACCEPTED".equals(status)) {
                act.put("title", "Conexão ativa com " + stName);
                act.put("subtitle", "Parceria confirmada · Chat e contatos liberados");
                act.put("badge", "Conexão Ativa");
                act.put("type", "connection");
            } else if ("REJECTED".equals(status)) {
                act.put("title", "Solicitação encerrada com " + stName);
                act.put("subtitle", "Status atualizado no histórico de conexões");
                act.put("badge", "Recusada");
                act.put("type", "system");
            } else if ("STARTUP".equals(sender)) {
                act.put("title", "Nova solicitação recebida de " + stName);
                act.put("subtitle", conn.getMessage() != null && !conn.getMessage().isBlank()
                        ? (conn.getMessage().length() > 75 ? conn.getMessage().substring(0, 75) + "..." : conn.getMessage())
                        : "Aguardando sua resposta na aba Solicitações Recebidas");
                act.put("badge", (conn.getMatchScore() != null ? conn.getMatchScore() : 90) + "% Match");
                act.put("type", "request");
            } else {
                act.put("title", "Solicitação enviada para " + stName);
                act.put("subtitle", "Aguardando retorno da startup");
                act.put("badge", "Enviada");
                act.put("type", "sent");
            }
            act.put("time", formatTimeAgo(conn.getUpdatedAt() != null ? conn.getUpdatedAt() : conn.getCreatedAt()));
            activities.add(act);
        }

        if (provider != null && activities.size() < 6) {
            Long uid = provider.getUser() != null ? provider.getUser().getId() : null;
            Long pid = provider.getId();
            List<Notification> notifs = notificationRepository.findAllByOrderByCreatedAtDesc();
            for (Notification n : notifs) {
                if (activities.size() >= 6) break;
                if (!"PROVIDER".equalsIgnoreCase(n.getRecipientRole())) continue;
                boolean matchesProvider = (pid != null && Objects.equals(pid, n.getProviderId()))
                        || (uid != null && Objects.equals(uid, n.getRecipientUserId()));
                if (!matchesProvider) continue;

                Map<String, Object> act = new LinkedHashMap<>();
                act.put("title", n.getTitle());
                act.put("subtitle", n.getDescription() != null ? n.getDescription() : "");
                act.put("badge", n.getType() != null ? n.getType() : "Notificação");
                act.put("type", "notification");
                act.put("time", n.getTimeAgo() != null ? n.getTimeAgo() : formatTimeAgo(n.getCreatedAt()));
                activities.add(act);
            }
        }

        return activities;
    }

    private String formatTimeAgo(LocalDateTime dt) {
        if (dt == null) return "Recente";
        Duration d = Duration.between(dt, LocalDateTime.now());
        long mins = Math.max(0, d.toMinutes());
        if (mins < 2) return "Agora";
        if (mins < 60) return "Há " + mins + " min";
        long hours = d.toHours();
        if (hours < 24) return "Há " + hours + "h";
        long days = d.toDays();
        return "Há " + days + (days == 1 ? " dia" : " dias");
    }

    private boolean isTractionStartup(StartupProfile st) {
        if (st == null) return false;
        if (Boolean.TRUE.equals(st.getTractionAttested())) return true;
        String stage = st.getCurrentStage();
        if (stage == null || stage.isBlank()) return false;
        String norm = Normalizer.normalize(stage, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT);
        return norm.contains("tracao") || norm.contains("escala");
    }

    private Set<Long> getSavedStartupIdSet(ServiceProvider provider) {
        Set<Long> ids = new LinkedHashSet<>();
        if (provider == null || provider.getId() == null) return ids;
        savedStartupRepository.findByProviderIdOrderByCreatedAtDesc(provider.getId())
                .forEach(s -> {
                    if (s.getStartupId() != null) ids.add(s.getStartupId());
                });
        if (ids.isEmpty() && provider.getSavedStartupIds() != null && !provider.getSavedStartupIds().isBlank()) {
            for (String tok : provider.getSavedStartupIds().split(",")) {
                try {
                    ids.add(Long.parseLong(tok.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return ids;
    }

    private ServiceProvider resolveProvider(Long providerId, Long userId) {
        if (providerId != null) {
            Optional<ServiceProvider> byId = providerRepository.findById(providerId);
            if (byId.isPresent()) return byId.get();
            Optional<ServiceProvider> byUser = providerRepository.findByUserId(providerId);
            if (byUser.isPresent()) return byUser.get();
        }
        if (userId != null) {
            return providerRepository.findByUserId(userId).orElse(null);
        }
        return null;
    }

    private StartupProfile resolveStartupProfile(Long startupId, Long userId) {
        if (startupId != null) {
            Optional<StartupProfile> byId = startupProfileRepository.findById(startupId);
            if (byId.isPresent()) return byId.get();
            Optional<StartupProfile> byUser = startupProfileRepository.findByUserId(startupId);
            if (byUser.isPresent()) return byUser.get();
        }
        if (userId != null) {
            return startupProfileRepository.findByUserId(userId).orElse(null);
        }
        return null;
    }

    private Map<String, String> buildDefaultProviderReasons(ServiceProvider p) {
        String cat = p.getCategory() != null && !p.getCategory().isBlank() ? p.getCategory() : "Serviços Especializados";
        String main = p.getMainService() != null && !p.getMainService().isBlank() ? p.getMainService() : cat;
        String skills = p.getToolsAndSkills() != null && !p.getToolsAndSkills().isBlank()
                ? p.getToolsAndSkills()
                : (p.getTags() != null && !p.getTags().isBlank() ? p.getTags() : main);
        String conn = p.getConnectionPreference() != null && !p.getConnectionPreference().isBlank()
                ? p.getConnectionPreference() : "Projetos sob demanda e parceria flexível";
        String avail = p.getAvailability() != null && !p.getAvailability().isBlank()
                ? p.getAvailability() : "Disponível";
        String sectors = p.getInterestSectors() != null && !p.getInterestSectors().isBlank()
                ? p.getInterestSectors()
                : (p.getTargetSegment() != null && !p.getTargetSegment().isBlank() ? p.getTargetSegment() : "Startups em Tração");

        String sinergia = "Especialização em " + main + " (" + cat + ") com domínio prático em " + skills + ".";
        String modelo = "Atuação no formato '" + conn + "' com disponibilidade '" + avail + "'.";
        String impacto = "Foco em destravar resultados e escalabilidade para startups dos setores: " + sectors + ".";

        Map<String, String> reasons = new LinkedHashMap<>();
        reasons.put("sinergiaTecnica", sinergia);
        reasons.put("modeloAtuacao", modelo);
        reasons.put("impactoEsperado", impacto);
        reasons.put("perfil", sinergia);
        reasons.put("objetivo", modelo);
        reasons.put("contexto", impacto);
        return reasons;
    }

    private void applyDtoToEntity(ServiceProviderDTO dto, ServiceProvider entity) {
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getProviderType() != null) entity.setProviderType(dto.getProviderType());
        if (dto.getCategory() != null) entity.setCategory(dto.getCategory());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        if (dto.getLocation() != null) entity.setLocation(dto.getLocation());
        if (dto.getPortfolioUrl() != null) entity.setPortfolioUrl(dto.getPortfolioUrl());
        if (dto.getLinkedinUrl() != null) entity.setLinkedinUrl(dto.getLinkedinUrl());
        if (dto.getAvailability() != null) entity.setAvailability(dto.getAvailability());
        if (dto.getAvatarUrl() != null) entity.setAvatarUrl(dto.getAvatarUrl());

        if (dto.getMainService() != null) entity.setMainService(dto.getMainService());
        if (dto.getServiceExperience() != null) entity.setServiceExperience(dto.getServiceExperience());
        if (dto.getToolsAndSkills() != null) entity.setToolsAndSkills(dto.getToolsAndSkills());
        if (dto.getComplementaryCategory() != null) entity.setComplementaryCategory(dto.getComplementaryCategory());
        if (dto.getTargetSegment() != null) entity.setTargetSegment(dto.getTargetSegment());
        if (dto.getExperienceYears() != null) entity.setExperienceYears(dto.getExperienceYears());

        if (dto.getProjectName() != null) entity.setProjectName(dto.getProjectName());
        if (dto.getProjectLink() != null) entity.setProjectLink(dto.getProjectLink());
        if (dto.getProjectDescription() != null) entity.setProjectDescription(dto.getProjectDescription());
        if (dto.getProjectYear() != null) entity.setProjectYear(dto.getProjectYear());
        if (dto.getProjectRepoUrl() != null) entity.setProjectRepoUrl(dto.getProjectRepoUrl());
        if (dto.getProjectTools() != null) entity.setProjectTools(dto.getProjectTools());
        if (dto.getProjectType() != null) entity.setProjectType(dto.getProjectType());
        if (dto.getProjectCoverUrl() != null) entity.setProjectCoverUrl(dto.getProjectCoverUrl());

        if (dto.getInterestSectors() != null) entity.setInterestSectors(dto.getInterestSectors());
        if (dto.getConnectionPreference() != null) entity.setConnectionPreference(dto.getConnectionPreference());
        if (dto.getOpportunitiesSought() != null) entity.setOpportunitiesSought(dto.getOpportunitiesSought());
        if (dto.getInterestRegion() != null) entity.setInterestRegion(dto.getInterestRegion());
        if (dto.getTargetStartupSize() != null) entity.setTargetStartupSize(dto.getTargetStartupSize());
        if (dto.getCommunicationLanguage() != null) entity.setCommunicationLanguage(dto.getCommunicationLanguage());

        if (dto.getTags() != null) entity.setTags(dto.getTags());
        if (dto.getSavedStartupIds() != null) entity.setSavedStartupIds(dto.getSavedStartupIds());
        if (dto.getMatchScore() != null) entity.setMatchScore(dto.getMatchScore());
    }
}
