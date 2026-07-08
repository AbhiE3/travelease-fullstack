package com.travelease.backend.itinerary.service;

import com.travelease.backend.auth.entity.Role;
import com.travelease.backend.auth.entity.User;
import com.travelease.backend.auth.repository.UserRepository;
import com.travelease.backend.itinerary.dto.NotificationResponse;
import com.travelease.backend.itinerary.entity.Notification;
import com.travelease.backend.itinerary.mapper.ItineraryMapper;
import com.travelease.backend.itinerary.repository.NotificationRepository;
import com.travelease.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceOwnershipTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ItineraryMapper itineraryMapper;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        lenient().when(itineraryMapper.toNotificationResponse(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            NotificationResponse response = new NotificationResponse();
            response.setNotificationId(n.getNotificationId());
            response.setUserId(n.getUserId());
            response.setNotificationType(n.getNotificationType());
            response.setTitle(n.getTitle());
            response.setMessage(n.getMessage());
            response.setIsRead(n.getIsRead());
            response.setCreatedDate(n.getCreatedDate());
            return response;
        });
    }

    private User user(String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setName(email);
        user.setRole(Role.ROLE_TRAVELER);
        return user;
    }

    private Notification notificationFor(String userId) {
        Notification notification = new Notification();
        notification.setNotificationId(UUID.randomUUID().toString());
        notification.setUserId(userId);
        notification.setNotificationType("ACTIVITY_REMINDER");
        notification.setTitle("Reminder");
        notification.setMessage("Your activity is coming up");
        notification.setIsRead(false);
        notification.setCreatedDate(LocalDateTime.now());
        return notification;
    }

    // --- listing isolation (27, 28) ---

    @Test
    void userSeesOnlyOwnNotifications() {
        User alice = user("alice@travelease.test");
        Notification aliceNotification = notificationFor(alice.getId().toString());
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));
        when(notificationRepository.findByUserIdOrderByCreatedDateDesc(alice.getId().toString()))
                .thenReturn(List.of(aliceNotification));

        List<NotificationResponse> responses = notificationService.getNotifications(alice.getEmail(), null, null);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getUserId()).isEqualTo(alice.getId().toString());
    }

    @Test
    void listingIsAlwaysScopedToCallerRegardlessOfAnyClientSuppliedId() {
        // getNotifications no longer accepts a client-supplied userId at all - the
        // identity always comes from the authenticated email, so there is no
        // parameter through which another user's id could be substituted.
        User bob = user("bob@travelease.test");
        when(userRepository.findByEmail(bob.getEmail())).thenReturn(Optional.of(bob));
        when(notificationRepository.findByUserIdOrderByCreatedDateDesc(bob.getId().toString()))
                .thenReturn(List.of());

        notificationService.getNotifications(bob.getEmail(), null, null);

        verify(notificationRepository).findByUserIdOrderByCreatedDateDesc(bob.getId().toString());
    }

    // --- mark-as-read ownership (29, 30) ---

    @Test
    void userCanMarkOwnNotificationRead() {
        User alice = user("alice@travelease.test");
        Notification notification = notificationFor(alice.getId().toString());
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));
        when(notificationRepository.findById(notification.getNotificationId())).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse response = notificationService.markAsRead(notification.getNotificationId(), alice.getEmail());

        assertThat(response.getIsRead()).isTrue();
    }

    @Test
    void userCannotMarkAnotherUsersNotificationRead() {
        User alice = user("alice@travelease.test");
        User bob = user("bob@travelease.test");
        Notification aliceNotification = notificationFor(alice.getId().toString());
        when(userRepository.findByEmail(bob.getEmail())).thenReturn(Optional.of(bob));
        when(notificationRepository.findById(aliceNotification.getNotificationId()))
                .thenReturn(Optional.of(aliceNotification));

        assertThatThrownBy(() -> notificationService.markAsRead(aliceNotification.getNotificationId(), bob.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
        verify(notificationRepository, never()).save(any());
    }

    // --- creation security decision (31, 32) ---

    @Test
    void ownNotificationCreationDerivesUserIdFromAuthenticatedCaller() {
        User alice = user("alice@travelease.test");
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse response = notificationService.createOwnNotification(
                alice.getEmail(), "ACTIVITY_REMINDER", "Reminder", "Message");

        assertThat(response.getUserId()).isEqualTo(alice.getId().toString());
    }

    @Test
    void createOwnNotificationHasNoParameterThroughWhichAnotherUserIdCouldBeSupplied() {
        // createOwnNotification's signature only accepts the caller's own email,
        // notificationType, title, and message - there is no userId parameter at
        // all, so a client-supplied "userId" in the request body cannot redirect
        // the operation to another user; the controller never reads that field.
        User bob = user("bob@travelease.test");
        when(userRepository.findByEmail(bob.getEmail())).thenReturn(Optional.of(bob));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse response = notificationService.createOwnNotification(
                bob.getEmail(), "ACTIVITY_REMINDER", "Reminder", "Message");

        assertThat(response.getUserId()).isEqualTo(bob.getId().toString());
    }

    @Test
    void internalCreateNotificationStillAcceptsExplicitTargetUserIdForReminderScheduler() {
        // The internal system path (used by ReminderScheduler) must keep working
        // unchanged: it notifies a target user other than any current caller,
        // since a scheduled job has no authenticated principal at all.
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse response = notificationService.createNotification(
                "some-target-user-id", "ACTIVITY_REMINDER", "Reminder", "Message");

        assertThat(response.getUserId()).isEqualTo("some-target-user-id");
    }

    @Test
    void notificationNotFoundThrowsResourceNotFound() {
        User alice = user("alice@travelease.test");
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));
        when(notificationRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead("missing", alice.getEmail()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
