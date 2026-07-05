package com.travelease.backend.itinerary.controller;

import com.travelease.backend.itinerary.dto.NotificationResponse;
import com.travelease.backend.itinerary.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    // Create a notification (called by system internally)
    // Body: { userId, notificationType, title, message }
    // ─────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<NotificationResponse> createNotification(
            @RequestBody Map<String, String> body) {
        NotificationResponse response =
                notificationService.createNotification(
                        body.get("userId"),
                        body.get("notificationType"),
                        body.get("title"),
                        body.get("message")
                );
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────
    // US-NOTIF-01, US-NOTIF-02 — GET /api/notifications
    // Get notifications with flexible filters
    // Example: /api/notifications?userId=123
    // Example: /api/notifications?userId=123&isRead=false
    // Example: /api/notifications?userId=123&type=ACTIVITY_REMINDER
    // Example: /api/notifications?userId=123&isRead=false&type=DEPARTURE_SUGGESTION
    // ─────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @RequestParam String userId,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) String type) {

        List<NotificationResponse> response =
                notificationService.getNotifications(
                        userId, isRead, type);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────
    // US-NOTIF-01 — PUT /api/notifications/{notificationId}/read
    // Mark a single notification as read
    // ─────────────────────────────────────────────────────
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable String notificationId) {
        NotificationResponse response =
                notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(response);
    }
}