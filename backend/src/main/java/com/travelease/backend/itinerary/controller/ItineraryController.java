package com.travelease.backend.itinerary.controller;

import com.travelease.backend.itinerary.dto.ItineraryRequest;
import com.travelease.backend.itinerary.dto.ItineraryResponse;
import com.travelease.backend.itinerary.service.ItineraryService;
import com.travelease.backend.shared.exception.InvalidRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ItineraryResponse> addItem(
            @RequestBody ItineraryRequest request) {
        ItineraryResponse response = itineraryService.addItem(request);
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
    public ResponseEntity<List<ItineraryResponse>> getItinerary(
            @RequestParam String tripId,
            @RequestParam(required = false) String activityDate,
            @RequestParam(required = false) String status) {

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
                    .getByTripIdAndDate(tripId, date);
        } else {
            response = itineraryService.getByTripId(tripId);
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
    public ResponseEntity<ItineraryResponse> updateItem(
            @PathVariable String itineraryId,
            @RequestBody ItineraryRequest request) {
        ItineraryResponse response =
                itineraryService.updateItem(itineraryId, request);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────
    // US-ITI-02 — DELETE /api/itinerary/{itineraryId}
    // Remove an activity from the itinerary
    // ─────────────────────────────────────────────────────
    @DeleteMapping("/{itineraryId}")
    public ResponseEntity<Map<String, String>> deleteItem(
            @PathVariable String itineraryId) {
        itineraryService.deleteItem(itineraryId);
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
    public ResponseEntity<Map<String, Object>> getProgress(
            @RequestParam String tripId) {
        Map<String, Object> progress =
                itineraryService.getProgress(tripId);
        return ResponseEntity.ok(progress);
    }
}