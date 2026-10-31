package com.startin.api.service;

import com.startin.api.dto.notification.NotificationDTO;
import com.startin.api.model.Notification;
import com.startin.api.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public List<NotificationDTO> listForProfile(
            String type,
            String role,
            Long userId,
            Long startupId,
            Long companyId) {

        boolean isCompany = "COMPANY".equalsIgnoreCase(role);
        List<Notification> all = notificationRepository.findAllByOrderByCreatedAtDesc();

        List<Notification> profileFiltered = all.stream()
                .filter(n -> matchesProfile(n, isCompany, userId, startupId, companyId))
                .toList();

        // Se for uma Empresa sem nenhuma notificação inicial ainda, semeia uma notificação corporativa integrada
        if (isCompany && profileFiltered.isEmpty() && companyId != null) {
            Notification seed = Notification.forCompany(
                    companyId,
                    userId,
                    "Startups compatíveis mapeadas pela IA",
                    "O motor de IA analisou as startups cadastradas para o perfil da sua empresa. Clique para visualizar os matches.",
                    "Matches",
                    "search-startups",
                    null
            );
            notificationRepository.save(seed);
            profileFiltered = List.of(seed);
        }

        if (type != null && !type.isBlank() && !"Todas".equalsIgnoreCase(type)) {
            String tNorm = type.trim().toLowerCase();
            profileFiltered = profileFiltered.stream()
                    .filter(n -> matchesTypeTab(n.getType(), tNorm))
                    .toList();
        }

        return profileFiltered.stream().map(NotificationDTO::fromEntity).toList();
    }

    public List<NotificationDTO> listAll(String type) {
        return listForProfile(type, null, null, null, null);
    }

    @Transactional
    public NotificationDTO markAsRead(Long id) {
        return notificationRepository.findById(id)
                .map(n -> {
                    n.setReadStatus(true);
                    return NotificationDTO.fromEntity(notificationRepository.save(n));
                })
                .orElse(null);
    }

    @Transactional
    public void markAllAsRead(String role, Long userId, Long startupId, Long companyId) {
        boolean hasProfileFilter = (role != null && !role.isBlank()) || userId != null || startupId != null || companyId != null;
        boolean isCompany = "COMPANY".equalsIgnoreCase(role);

        List<Notification> list = notificationRepository.findAll();
        for (Notification n : list) {
            if (!hasProfileFilter || matchesProfile(n, isCompany, userId, startupId, companyId)) {
                n.setReadStatus(true);
            }
        }
        notificationRepository.saveAll(list);
    }

    @Transactional
    public void markAllAsRead() {
        markAllAsRead(null, null, null, null);
    }

    private boolean matchesProfile(
            Notification n,
            boolean isCompany,
            Long userId,
            Long startupId,
            Long companyId) {
        String nRole = n.getRecipientRole() != null ? n.getRecipientRole().trim().toUpperCase() : "STARTUP";
        if (isCompany) {
            if (!"COMPANY".equals(nRole)) {
                return false;
            }
            if (companyId != null && n.getCompanyId() != null) {
                return Objects.equals(n.getCompanyId(), companyId);
            }
            if (userId != null && n.getRecipientUserId() != null) {
                return Objects.equals(n.getRecipientUserId(), userId);
            }
            return n.getCompanyId() == null && n.getRecipientUserId() == null;
        } else {
            if ("COMPANY".equals(nRole)) {
                return false;
            }
            if (startupId != null && n.getStartupId() != null) {
                return Objects.equals(n.getStartupId(), startupId);
            }
            if (userId != null && n.getRecipientUserId() != null) {
                return Objects.equals(n.getRecipientUserId(), userId);
            }
            return n.getStartupId() == null && n.getRecipientUserId() == null;
        }
    }

    private boolean matchesTypeTab(String notifType, String tabNorm) {
        String nt = (notifType != null ? notifType : "").trim().toLowerCase();
        if (tabNorm.startsWith("oportunidade")) {
            return nt.contains("oportunidade") || nt.contains("edital") || nt.contains("poc") || nt.contains("compra");
        }
        if (tabNorm.startsWith("match")) {
            return nt.contains("match") || nt.contains("empresa") || nt.contains("startup");
        }
        if (tabNorm.startsWith("sistema")) {
            return nt.contains("sistema") || nt.contains("conta") || nt.contains("perfil");
        }
        return nt.equalsIgnoreCase(tabNorm);
    }
}
