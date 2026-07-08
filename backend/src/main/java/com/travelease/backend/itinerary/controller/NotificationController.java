package com.travelease.backend.itinerary.controller;

import com.travelease.backend.itinerary.dto.NotificationResponse;
import com.travelease.backend.itinerary.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "http://localhost:4200")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    // ─────────────────────────────────────────────────────
    // US-NOTIF-01 — POST /api/notifications
    // Create a notification for the authenticated caller.
    // Body: { notificationType, title, message }
    // ─────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<NotificationResponse> createNotification(
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        NotificationResponse response =
                notificationService.createOwnNotification(
                        authentication.getName(),
                        body.get("notificationType"),
                        body.get("title"),
                        body.get("message")
                );
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────
    // US-NOTIF-01, US-NOTIF-02 — GET /api/notifications
    // Get the authenticated caller's own notifications with flexible filters.
    // Example: /api/notifications?isRead=false
    // Example: /api/notifications?type=ACTIVITY_REMINDER
    // Example: /api/notifications?isRead=false&type=DEPARTURE_SUGGESTION
    // ─────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) String type,
            Authentication authentication) {

        List<NotificationResponse> response =
                notificationService.getNotifications(
                        authentication.getName(), isRead, type);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────
    // US-NOTIF-01 — PUT /api/notifications/{notificationId}/read
    // Mark a single notification as read - only if it belongs to the caller.
    // ─────────────────────────────────────────────────────
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable String notificationId,
            Authentication authentication) {
        NotificationResponse response =
                notificationService.markAsRead(notificationId, authentication.getName());
        return ResponseEntity.ok(response);
    }
}