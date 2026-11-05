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
            Long companyId,
            Long providerId) {

        String normRole = role != null ? role.trim().toUpperCase() : "STARTUP";
        List<Notification> all = notificationRepository.findAllByOrderByCreatedAtDesc();

        List<Notification> profileFiltered = all.stream()
                .filter(n -> matchesProfile(n, normRole, userId, startupId, companyId, providerId))
                .toList();

        if (type != null && !type.isBlank() && !"Todas".equalsIgnoreCase(type)) {
            String tNorm = type.trim().toLowerCase();
            profileFiltered = profileFiltered.stream()
                    .filter(n -> matchesTypeTab(n.getType(), tNorm))
                    .toList();
        }

        return profileFiltered.stream().map(NotificationDTO::fromEntity).toList();
    }

    @Transactional
    public List<NotificationDTO> listForProfile(
            String type,
            String role,
            Long userId,
            Long startupId,
            Long companyId) {
        return listForProfile(type, role, userId, startupId, companyId, null);
    }

    public List<NotificationDTO> listAll(String type) {
        return listForProfile(type, null, null, null, null, null);
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
    public void markAllAsRead(String role, Long userId, Long startupId, Long companyId, Long providerId) {
        boolean hasProfileFilter = (role != null && !role.isBlank()) || userId != null || startupId != null || companyId != null || providerId != null;
        String normRole = role != null ? role.trim().toUpperCase() : "STARTUP";

        List<Notification> list = notificationRepository.findAll();
        for (Notification n : list) {
            if (!hasProfileFilter || matchesProfile(n, normRole, userId, startupId, companyId, providerId)) {
                n.setReadStatus(true);
            }
        }
        notificationRepository.saveAll(list);
    }

    @Transactional
    public void markAllAsRead(String role, Long userId, Long startupId, Long companyId) {
        markAllAsRead(role, userId, startupId, companyId, null);
    }

    @Transactional
    public void markAllAsRead() {
        markAllAsRead(null, null, null, null, null);
    }

    private boolean matchesProfile(
            Notification n,
            String normRole,
            Long userId,
            Long startupId,
            Long companyId,
            Long providerId) {
        String nRole = n.getRecipientRole() != null ? n.getRecipientRole().trim().toUpperCase() : "STARTUP";
        if ("COMPANY".equals(normRole)) {
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
        } else if ("PROVIDER".equals(normRole)) {
            if (!"PROVIDER".equals(nRole)) {
                return false;
            }
            if (providerId != null && n.getProviderId() != null) {
                return Objects.equals(n.getProviderId(), providerId);
            }
            if (userId != null && n.getRecipientUserId() != null) {
                return Objects.equals(n.getRecipientUserId(), userId);
            }
            return n.getProviderId() == null && n.getRecipientUserId() == null;
        } else {
            if (!"STARTUP".equals(nRole)) {
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
