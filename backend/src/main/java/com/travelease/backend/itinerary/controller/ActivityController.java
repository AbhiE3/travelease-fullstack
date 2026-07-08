package com.travelease.backend.itinerary.controller;

import com.travelease.backend.itinerary.dto.ActivitySlotResponse;
import com.travelease.backend.itinerary.entity.Activity;
import com.travelease.backend.itinerary.repository.ActivityRepository;
import com.travelease.backend.itinerary.service.ActivityBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/activities")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityRepository activityRepository;
    private final ActivityBookingService activityBookingService;

    // US-ITI-01 — GET /api/activities?destinationId=
    // Get activities for dropdown when adding itinerary item
    @GetMapping
    public ResponseEntity<List<Activity>> getActivities(
            @RequestParam Integer destinationId) {
        List<Activity> activities = activityRepository
                .findByDestinationId(destinationId);
        return ResponseEntity.ok(activities);
    }

    // Traveler-facing slot discovery for booking - only slots that have not yet
    // started are returned, mirroring the eligibility rule enforced at booking
    // creation time (ActivityBookingServiceImpl.createBooking).
    @GetMapping("/{activityId}/slots")
    public ResponseEntity<List<ActivitySlotResponse>> getBookableSlots(@PathVariable String activityId) {
        return ResponseEntity.ok(activityBookingService.getBookableSlots(activityId));
    }
}