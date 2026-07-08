package com.travelease.backend.itinerary.service;

import com.travelease.backend.auth.entity.User;
import com.travelease.backend.auth.repository.UserRepository;
import com.travelease.backend.itinerary.dto.NotificationResponse;
import com.travelease.backend.itinerary.entity.Notification;
import com.travelease.backend.itinerary.mapper.ItineraryMapper;
import com.travelease.backend.itinerary.repository.NotificationRepository;
import com.travelease.backend.shared.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ItineraryMapper itineraryMapper;

    @Autowired
    private UserRepository userRepository;

    // Internal/system notification creation, addressed to an explicit target
    // userId. Used by ReminderScheduler (a direct in-process method call, not an
    // HTTP path) to notify a user other than any current caller - this method
    // must keep accepting an explicit target userId for that to keep working.
    // Never call this directly from an HTTP-facing controller with client input;
    // use createOwnNotification for that.
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

    // Traveler-facing self-notification creation - userId is always derived from
    // the authenticated caller, never trusted from client-supplied input.
    public NotificationResponse createOwnNotification(
            String currentUserEmail,
            String notificationType,
            String title,
            String message) {
        return createNotification(resolveCurrentUserId(currentUserEmail), notificationType, title, message);
    }

    // US-NOTIF-01 — Get all notifications for the authenticated caller
    // Query params: isRead, type handled here
    public List<NotificationResponse> getNotifications(
            String currentUserEmail,
            Boolean isRead,
            String type) {

        String userId = resolveCurrentUserId(currentUserEmail);
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

    // US-NOTIF-01 — Mark single notification as read, only if it belongs to the
    // authenticated caller.
    public NotificationResponse markAsRead(String notificationId, String currentUserEmail) {
        String userId = resolveCurrentUserId(currentUserEmail);
        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found: " + notificationId));

        if (!Objects.equals(notification.getUserId(), userId)) {
            throw new AccessDeniedException("Current user does not own this notification");
        }

        notification.setIsRead(true);
        Notification updated = notificationRepository.save(notification);
        return itineraryMapper.toNotificationResponse(updated);
    }

    private String resolveCurrentUserId(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        return user.getId().toString();
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