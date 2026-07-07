package com.travelease.backend.itinerary.controller;

import com.travelease.backend.itinerary.entity.Activity;
import com.travelease.backend.itinerary.repository.ActivityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/activities")
@CrossOrigin(origins = "http://localhost:4200")
public class ActivityController {

    @Autowired
    private ActivityRepository activityRepository;

    // US-ITI-01 — GET /api/activities?destinationId=
    // Get activities for dropdown when adding itinerary item
    @GetMapping
    public ResponseEntity<List<Activity>> getActivities(
            @RequestParam Integer destinationId) {
        List<Activity> activities = activityRepository
                .findByDestinationId(destinationId);
        return ResponseEntity.ok(activities);
    }
}