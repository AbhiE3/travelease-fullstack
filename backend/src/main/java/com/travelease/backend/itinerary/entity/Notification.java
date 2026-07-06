package com.travelease.backend.itinerary.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @Column(name = "NotificationID")
    private String notificationId;

    @Column(name = "UserID", nullable = false)
    private String userId;

    @Column(name = "NotificationType")
    private String notificationType;

    @Column(name = "Title")
    private String title;

    @Column(name = "Message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "IsRead")
    private Boolean isRead = false;

    @Column(name = "CreatedDate")
    private LocalDateTime createdDate;
}