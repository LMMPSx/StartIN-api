package com.startin.api.service;

import com.startin.api.dto.provider.ServiceConnectionDTO;
import com.startin.api.model.Notification;
import com.startin.api.model.Opportunity;
import com.startin.api.model.ServiceConnection;
import com.startin.api.model.ServiceProvider;
import com.startin.api.model.StartupProfile;
import com.startin.api.model.enums.OpportunityStatus;
import com.startin.api.model.enums.OpportunityType;
import com.startin.api.repository.NotificationRepository;
import com.startin.api.repository.OpportunityRepository;
import com.startin.api.repository.ServiceConnectionRepository;
import com.startin.api.repository.ServiceProviderRepository;
import com.startin.api.repository.StartupProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ServiceConnectionService {

    private final ServiceConnectionRepository connectionRepository;
    private final StartupProfileRepository startupRepository;
    private final ServiceProviderRepository providerRepository;
    private final OpportunityRepository opportunityRepository;
    private final NotificationRepository notificationRepository;
    private final AiIntegrationService aiIntegrationService;

    public ServiceConnectionService(
            ServiceConnectionRepository connectionRepository,
            StartupProfileRepository startupRepository,
            ServiceProviderRepository providerRepository,
            OpportunityRepository opportunityRepository,
            NotificationRepository notificationRepository,
            AiIntegrationService aiIntegrationService) {
        this.connectionRepository = connectionRepository;
        this.startupRepository = startupRepository;
        this.providerRepository = providerRepository;
        this.opportunityRepository = opportunityRepository;
        this.notificationRepository = notificationRepository;
        this.aiIntegrationService = aiIntegrationService;
    }

    public List<ServiceConnectionDTO> listByStartup(Long startupId) {
        return connectionRepository.findByStartupIdOrderByCreatedAtDesc(startupId)
                .stream()
                .map(ServiceConnectionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ServiceConnectionDTO> listByProvider(Long providerId) {
        return connectionRepository.findByProviderIdOrderByCreatedAtDesc(providerId)
                .stream()
                .map(ServiceConnectionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ServiceConnectionDTO> listForQuery(Long startupId, Long providerId, Long userId, String role) {
        if (startupId != null) {
            Optional<StartupProfile> st = startupRepository.findById(startupId)
                    .or(() -> startupRepository.findByUserId(startupId));
            return st.map(s -> listByStartup(s.getId())).orElse(List.of());
        }
        if (providerId != null) {
            Optional<ServiceProvider> pr = providerRepository.findById(providerId)
                    .or(() -> providerRepository.findByUserId(providerId));
            return pr.map(p -> listByProvider(p.getId())).orElse(List.of());
        }
        if (userId != null) {
            if ("PROVIDER".equalsIgnoreCase(role)) {
                return providerRepository.findByUserId(userId)
                        .map(p -> listByProvider(p.getId()))
                        .orElse(List.of());
            }
            if ("STARTUP".equalsIgnoreCase(role)) {
                return startupRepository.findByUserId(userId)
                        .map(s -> listByStartup(s.getId()))
                        .orElse(List.of());
            }
            Optional<ServiceProvider> pr = providerRepository.findByUserId(userId);
            if (pr.isPresent()) {
                return listByProvider(pr.get().getId());
            }
            Optional<StartupProfile> st = startupRepository.findByUserId(userId);
            if (st.isPresent()) {
                return listByStartup(st.get().getId());
            }
        }
        return connectionRepository.findAll().stream()
                .map(ServiceConnectionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ServiceConnectionDTO createConnection(Long startupId, Long providerId, String senderType, String message, Integer matchScore) {
        StartupProfile startup = resolveStartup(startupId)
                .orElseThrow(() -> new IllegalArgumentException("Perfil de Startup não encontrado no banco de dados."));
        ServiceProvider provider = resolveProvider(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Perfil de Prestador não encontrado no banco de dados."));

        boolean isStartupTracao = "Tração".equalsIgnoreCase(startup.getCurrentStage())
                || "Tracao".equalsIgnoreCase(startup.getCurrentStage())
                || Boolean.TRUE.equals(startup.getTractionAttested())
                || (startup.getTrlScore() != null && startup.getTrlScore() >= 7);
        if (!isStartupTracao) {
            throw new IllegalArgumentException("Conexões entre startups e prestadores de serviços são permitidas exclusivamente para startups no estágio de Tração.");
        }

        String normalizedSender = senderType != null ? senderType.toUpperCase() : "STARTUP";

        int computedScore = matchScore != null && matchScore > 0 ? matchScore : 0;
        if (computedScore == 0) {
            List<Opportunity> demands = startup.getId() != null
                    ? opportunityRepository.findByStartupIdOrderByIdDesc(startup.getId()).stream()
                            .filter(o -> o.getType() == OpportunityType.SERVICO && o.getStatus() == OpportunityStatus.PUBLICADA)
                            .toList()
                    : List.of();
            Map<String, Object> comp = aiIntegrationService.computeProviderStartupCompatibility(provider, startup, demands);
            Object sc = comp.get("matchScore");
            computedScore = sc instanceof Number n ? n.intValue() : 88;
        }

        ServiceConnection conn = new ServiceConnection(
                startup,
                provider,
                normalizedSender,
                message,
                "PENDING",
                computedScore
        );
        ServiceConnection saved = connectionRepository.save(conn);

        // Cria notificação real no MySQL para o destinatário
        try {
            if ("STARTUP".equals(normalizedSender)) {
                Long providerUserId = provider.getUser() != null ? provider.getUser().getId() : null;
                Notification notif = Notification.forProvider(
                        provider.getId(),
                        providerUserId,
                        "Nova solicitação de conexão: " + startup.getName(),
                        "A startup " + startup.getName() + " (" + computedScore + "% Match) enviou uma proposta de conexão para seus serviços de " + provider.getCategory() + ".",
                        "Matches",
                        "connections",
                        String.valueOf(saved.getId())
                );
                notificationRepository.save(notif);
            } else {
                Long startupUserId = startup.getUser() != null ? startup.getUser().getId() : null;
                Notification notif = Notification.forStartup(
                        startup.getId(),
                        startupUserId,
                        "Prestador interessado: " + provider.getName(),
                        provider.getName() + " (" + provider.getCategory() + " · " + computedScore + "% Match) enviou uma solicitação de conexão para apoiar sua startup.",
                        "Matches",
                        "connections",
                        String.valueOf(saved.getId())
                );
                notificationRepository.save(notif);
            }
        } catch (Exception ignored) {
        }

        return ServiceConnectionDTO.fromEntity(saved);
    }

    @Transactional
    public ServiceConnectionDTO updateStatus(Long connectionId, String status) {
        ServiceConnection conn = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("Conexão não encontrada: " + connectionId));
        String normalizedStatus = status != null ? status.toUpperCase() : "PENDING";
        conn.setStatus(normalizedStatus);
        ServiceConnection saved = connectionRepository.save(conn);

        // Cria notificação real no MySQL sobre o aceite ou recusa da conexão
        try {
            StartupProfile st = saved.getStartup();
            ServiceProvider pr = saved.getProvider();
            if (st != null && pr != null) {
                if ("ACCEPTED".equals(normalizedStatus)) {
                    if ("STARTUP".equalsIgnoreCase(saved.getSenderType())) {
                        Long stUserId = st.getUser() != null ? st.getUser().getId() : null;
                        notificationRepository.save(Notification.forStartup(
                                st.getId(),
                                stUserId,
                                "Conexão aceita por " + pr.getName() + "!",
                                pr.getName() + " (" + pr.getCategory() + ") aceitou sua solicitação. O chat direto e os contatos foram liberados.",
                                "Matches",
                                "connections",
                                String.valueOf(saved.getId())
                        ));
                    } else {
                        Long prUserId = pr.getUser() != null ? pr.getUser().getId() : null;
                        notificationRepository.save(Notification.forProvider(
                                pr.getId(),
                                prUserId,
                                "Conexão aceita pela startup " + st.getName() + "!",
                                "A startup " + st.getName() + " aceitou sua solicitação de parceria. O chat direto e os contatos já estão liberados.",
                                "Matches",
                                "connections",
                                String.valueOf(saved.getId())
                        ));
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return ServiceConnectionDTO.fromEntity(saved);
    }

    @Transactional
    public void deleteConnection(Long connectionId) {
        if (connectionId == null) {
            throw new IllegalArgumentException("ID da conexão é obrigatório.");
        }
        if (connectionRepository.existsById(connectionId)) {
            connectionRepository.deleteById(connectionId);
        }
    }

    private Optional<StartupProfile> resolveStartup(Long idOrUserId) {
        if (idOrUserId == null) return Optional.empty();
        Optional<StartupProfile> byId = startupRepository.findById(idOrUserId);
        if (byId.isPresent()) return byId;
        return startupRepository.findByUserId(idOrUserId);
    }

    private Optional<ServiceProvider> resolveProvider(Long idOrUserId) {
        if (idOrUserId == null) return Optional.empty();
        Optional<ServiceProvider> byId = providerRepository.findById(idOrUserId);
        if (byId.isPresent()) return byId;
        return providerRepository.findByUserId(idOrUserId);
    }
}
