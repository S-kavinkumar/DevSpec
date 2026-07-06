package com.devspec.repository;

import com.devspec.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUsernameOrderByCreatedAtDesc(String username);
    List<Notification> findByUsernameAndIsReadOrderByCreatedAtDesc(String username, Boolean isRead);
}
