package com.travelease.backend.itinerary.repository;

import com.travelease.backend.itinerary.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, String> {

    List<Notification> findByUserIdOrderByCreatedDateDesc(
            String userId);

    List<Notification> findByUserIdAndIsRead(
            String userId, Boolean isRead);

    List<Notification> findByUserIdAndNotificationType(
            String userId, String notificationType);
}