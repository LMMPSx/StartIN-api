package com.startin.api.service;

import com.startin.api.dto.notification.NotificationDTO;
import com.startin.api.model.Notification;
import com.startin.api.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<NotificationDTO> listAll(String type) {
        List<Notification> list;
        if (type != null && !type.isBlank() && !"Todas".equalsIgnoreCase(type)) {
            list = notificationRepository.findByTypeIgnoreCaseOrderByCreatedAtDesc(type);
        } else {
            list = notificationRepository.findAllByOrderByCreatedAtDesc();
        }
        return list.stream().map(NotificationDTO::fromEntity).toList();
    }

    @Transactional
    public void markAllAsRead() {
        List<Notification> list = notificationRepository.findAll();
        for (Notification n : list) {
            n.setReadStatus(true);
        }
        notificationRepository.saveAll(list);
    }
}
