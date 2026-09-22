package com.dentalclinic.service;

import com.dentalclinic.model.Notification;
import com.dentalclinic.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    public Notification sendNotification(String targetRole, String title, String message, String type) {
        Notification notification = new Notification(targetRole, title, message, type);
        Notification saved = notificationRepository.save(notification);

        if (messagingTemplate != null) {
            // Push via STOMP broker to subscribers
            messagingTemplate.convertAndSend("/topic/notifications", saved);
        }
        return saved;
    }

    public List<Notification> getAllRecent() {
        return notificationRepository.findTop20ByOrderByCreatedAtDesc();
    }

    public List<Notification> getForRole(String role) {
        return notificationRepository.findByTargetRoleOrTargetRoleOrderByCreatedAtDesc(role, "ALL");
    }
}
