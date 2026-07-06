package com.travelease.backend.itinerary.service;

import com.travelease.backend.itinerary.entity.Itinerary;
import com.travelease.backend.itinerary.repository.ItineraryRepository;
import com.travelease.backend.itinerary.repository.NotificationRepository;
import com.travelease.backend.itinerary.repository.DelayImpactAnalysisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ReminderScheduler {

    @Autowired
    private ItineraryRepository itineraryRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private DelayImpactAnalysisRepository delayImpactAnalysisRepository;

    // ─────────────────────────────────────────────────────────
    // US-NOTIF-01 — Activity Reminder
    // Runs every hour — checks for activities happening tomorrow
    // Creates ACTIVITY_REMINDER notification for each member
    // ─────────────────────────────────────────────────────────
    @Scheduled(cron = "0 0 * * * *")
    public void generateActivityReminders() {

        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // Get all PENDING itinerary items for tomorrow
        List<Itinerary> tomorrowItems = itineraryRepository
                .findAll()
                .stream()
                .filter(i -> "Pending".equals(i.getStatus())
                        && tomorrow.equals(i.getActivityDate()))
                .toList();

        for (Itinerary item : tomorrowItems) {

            String userId = item.getTripId();
            // Note: in full implementation, fetch all TripMembers
            // for item.getTripId() and notify each member
            // For now notifying using tripId as placeholder

            String activityTime = item.getStartTime() != null
                    ? item.getStartTime()
                    .format(DateTimeFormatter.ofPattern("hh:mm a"))
                    : "scheduled time";

            String keyword = item.getActivityId();

            // Avoid duplicate reminders
            boolean alreadySent = notificationService
                    .reminderAlreadySent(
                            userId,
                            "ACTIVITY_REMINDER",
                            keyword);

            if (!alreadySent) {
                notificationService.createNotification(
                        userId,
                        "ACTIVITY_REMINDER",
                        "Upcoming Activity Reminder",
                        "Reminder: Your activity is scheduled"
                                + " at " + activityTime + " tomorrow."
                );
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    // US-NOTIF-02 — Departure Suggestion
    // Runs every hour — calculates suggested departure time
    // Default: 30 min travel + 10 min buffer before activity
    // ─────────────────────────────────────────────────────────
    @Scheduled(cron = "0 0 * * * *")
    public void generateDepartureSuggestions() {

        LocalDate tomorrow = LocalDate.now().plusDays(1);

        int estimatedTravelMinutes = 30;
        int bufferMinutes = 10;

        List<Itinerary> tomorrowItems = itineraryRepository
                .findAll()
                .stream()
                .filter(i -> "Pending".equals(i.getStatus())
                        && tomorrow.equals(i.getActivityDate())
                        && i.getStartTime() != null)
                .toList();

        for (Itinerary item : tomorrowItems) {

            String userId = item.getTripId();

            LocalDateTime suggestedDeparture = item.getStartTime()
                    .minusMinutes(estimatedTravelMinutes)
                    .minusMinutes(bufferMinutes);

            String departureTime = suggestedDeparture
                    .format(DateTimeFormatter.ofPattern("hh:mm a"));

            String arrivalTime = item.getStartTime()
                    .format(DateTimeFormatter.ofPattern("hh:mm a"));

            String keyword = "departure-" + item.getActivityId();

            boolean alreadySent = notificationService
                    .reminderAlreadySent(
                            userId,
                            "DEPARTURE_SUGGESTION",
                            item.getActivityId());

            if (!alreadySent) {
                notificationService.createNotification(
                        userId,
                        "DEPARTURE_SUGGESTION",
                        "Departure Suggestion",
                        "Leave by " + departureTime
                                + " to reach your activity"
                                + " on time at " + arrivalTime
                );
            }
        }
    }
}