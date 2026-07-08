package com.travelease.backend.itinerary.controller;

import com.travelease.backend.itinerary.dto.ItineraryRequest;
import com.travelease.backend.itinerary.dto.ItineraryResponse;
import com.travelease.backend.itinerary.service.ItineraryService;
import com.travelease.backend.shared.exception.InvalidRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/itinerary")
@CrossOrigin(origins = "http://localhost:4200")
public class ItineraryController {

    @Autowired
    private ItineraryService itineraryService;

    // ─────────────────────────────────────────────────────
    // US-ITI-01 — POST /api/itinerary
    // Create a new itinerary item
    // ─────────────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasAnyRole('TRAVELER','ADMIN')")
    public ResponseEntity<ItineraryResponse> addItem(
            @RequestBody ItineraryRequest request,
            Authentication authentication) {
        ItineraryResponse response = itineraryService.addItem(request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────
    // US-ITI-01 — GET /api/itinerary
    // Get all itinerary items for a trip
    // Optional filters: activityDate, status
    // Example: /api/itinerary?tripId=123
    // Example: /api/itinerary?tripId=123&status=Pending
    // Example: /api/itinerary?tripId=123&activityDate=2025-08-10
    // ─────────────────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasAnyRole('TRAVELER','ADMIN')")
    public ResponseEntity<List<ItineraryResponse>> getItinerary(
            @RequestParam String tripId,
            @RequestParam(required = false) String activityDate,
            @RequestParam(required = false) String status,
            Authentication authentication) {

        List<ItineraryResponse> response;

        if (activityDate != null) {
            LocalDate date;
            try {
                date = LocalDate.parse(activityDate);
            } catch (DateTimeParseException ex) {
                throw new InvalidRequestException(
                        "activityDate must be in yyyy-MM-dd format: " + activityDate);
            }
            response = itineraryService
                    .getByTripIdAndDate(tripId, date, authentication.getName());
        } else {
            response = itineraryService.getByTripId(tripId, authentication.getName());
        }

        // filter by status if provided
        if (status != null) {
            String s = status;
            response = response.stream()
                    .filter(i -> s.equals(i.getStatus()))
                    .toList();
        }

        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────
    // US-ITI-02, US-ITI-03 — PUT /api/itinerary/{itineraryId}
    // Update item details OR mark complete/incomplete
    // Send { "status": "Completed" } to mark complete
    // Send { "status": "Pending" }   to mark incomplete
    // ─────────────────────────────────────────────────────
    @PutMapping("/{itineraryId}")
    @PreAuthorize("hasAnyRole('TRAVELER','ADMIN')")
    public ResponseEntity<ItineraryResponse> updateItem(
            @PathVariable String itineraryId,
            @RequestBody ItineraryRequest request,
            Authentication authentication) {
        ItineraryResponse response =
                itineraryService.updateItem(itineraryId, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────
    // US-ITI-02 — DELETE /api/itinerary/{itineraryId}
    // Remove an activity from the itinerary (organizer-only)
    // ─────────────────────────────────────────────────────
    @DeleteMapping("/{itineraryId}")
    @PreAuthorize("hasAnyRole('TRAVELER','ADMIN')")
    public ResponseEntity<Map<String, String>> deleteItem(
            @PathVariable String itineraryId,
            Authentication authentication) {
        itineraryService.deleteItem(itineraryId, authentication.getName());
        return ResponseEntity.ok(
                Map.of("message",
                        "Itinerary item deleted successfully"));
    }

    // ─────────────────────────────────────────────────────
    // US-ITI-03 — GET /api/itinerary/progress?tripId=
    // Get completion progress for a trip
    // Returns total, completed, pending, percentage
    // ─────────────────────────────────────────────────────
    @GetMapping("/progress")
    @PreAuthorize("hasAnyRole('TRAVELER','ADMIN')")
    public ResponseEntity<Map<String, Object>> getProgress(
            @RequestParam String tripId,
            Authentication authentication) {
        Map<String, Object> progress =
                itineraryService.getProgress(tripId, authentication.getName());
        return ResponseEntity.ok(progress);
    }
}