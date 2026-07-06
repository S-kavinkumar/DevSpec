package com.devspec.controller;

import com.devspec.model.Notification;
import com.devspec.repository.NotificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    public ResponseEntity<?> getNotifications(Authentication authentication) {
        List<Notification> notifications = notificationRepository.findByUsernameOrderByCreatedAtDesc(authentication.getName());
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadNotifications(Authentication authentication) {
        List<Notification> notifications = notificationRepository.findByUsernameAndIsReadOrderByCreatedAtDesc(authentication.getName(), false);
        return ResponseEntity.ok(notifications);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id, Authentication authentication) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        if (!notification.getUsername().equalsIgnoreCase(authentication.getName())) {
            return ResponseEntity.status(403).body("Access Denied: You do not own this notification");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);

        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }

    @PostMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(Authentication authentication) {
        List<Notification> unread = notificationRepository.findByUsernameAndIsReadOrderByCreatedAtDesc(authentication.getName(), false);
        for (Notification n : unread) {
            n.setIsRead(true);
            notificationRepository.save(n);
        }
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }
}
