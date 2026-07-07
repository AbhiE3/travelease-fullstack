package com.travelease.backend.itinerary.service;

import com.travelease.backend.itinerary.dto.NotificationResponse;
import com.travelease.backend.itinerary.entity.Notification;
import com.travelease.backend.itinerary.mapper.ItineraryMapper;
import com.travelease.backend.itinerary.repository.NotificationRepository;
import com.travelease.backend.shared.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ItineraryMapper itineraryMapper;

    // US-26 — Create / send a notification (called by system internally)
    public NotificationResponse createNotification(
            String userId,
            String notificationType,
            String title,
            String message) {

        Notification notification = new Notification();
        notification.setNotificationId(UUID.randomUUID().toString());
        notification.setUserId(userId);
        notification.setNotificationType(notificationType);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setIsRead(false);
        notification.setCreatedDate(LocalDateTime.now());

        Notification saved = notificationRepository.save(notification);
        return itineraryMapper.toNotificationResponse(saved);
    }

    // US-NOTIF-01 — Get all notifications for a user
    // Query params: isRead, type handled here
    public List<NotificationResponse> getNotifications(
            String userId,
            Boolean isRead,
            String type) {

        List<Notification> notifications;

        if (isRead != null && type != null) {
            // filter by both isRead and type
            notifications = notificationRepository
                    .findByUserIdAndIsRead(userId, isRead)
                    .stream()
                    .filter(n -> type.equals(n.getNotificationType()))
                    .collect(Collectors.toList());

        } else if (isRead != null) {
            // filter by isRead only — replaces old /unread endpoint
            notifications = notificationRepository
                    .findByUserIdAndIsRead(userId, isRead);

        } else if (type != null) {
            // filter by type only
            notifications = notificationRepository
                    .findByUserIdAndNotificationType(userId, type);

        } else {
            // get all notifications for user
            notifications = notificationRepository
                    .findByUserIdOrderByCreatedDateDesc(userId);
        }

        return notifications.stream()
                .map(itineraryMapper::toNotificationResponse)
                .collect(Collectors.toList());
    }

    // US-NOTIF-01 — Mark single notification as read
    public NotificationResponse markAsRead(String notificationId) {
        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found: " + notificationId));

        notification.setIsRead(true);
        Notification updated = notificationRepository.save(notification);
        return itineraryMapper.toNotificationResponse(updated);
    }

    // Used by ReminderScheduler — check if reminder already sent
    public boolean reminderAlreadySent(String userId,
                                       String type, String keyword) {
        return notificationRepository
                .findByUserIdAndNotificationType(userId, type)
                .stream()
                .anyMatch(n -> n.getMessage() != null
                        && n.getMessage().contains(keyword)
                        && n.getCreatedDate() != null
                        && n.getCreatedDate().toLocalDate()
                        .equals(java.time.LocalDate.now()));
    }
}