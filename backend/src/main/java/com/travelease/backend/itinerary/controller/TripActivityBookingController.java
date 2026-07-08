package com.travelease.backend.itinerary.controller;

import com.travelease.backend.itinerary.dto.ActivityBookingResponse;
import com.travelease.backend.itinerary.dto.AttachActivityBookingRequest;
import com.travelease.backend.itinerary.service.ActivityBookingService;
import com.travelease.backend.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Attach/detach/list only - ActivityBooking's own lifecycle (create, cancel,
 * attendance) remains exclusively ActivityBookingController's/
 * ActivityProviderController's responsibility. Mirrors the existing
 * TripBusBookingController/TripAccommodationController precedent for
 * Bus/Hotel Booking-to-Trip association.
 */
@RestController
@RequestMapping("/api/trips/{tripId}/activity-bookings")
@RequiredArgsConstructor
public class TripActivityBookingController {

    private final ActivityBookingService activityBookingService;

    @PostMapping
    @PreAuthorize("hasAnyRole('TRAVELER','ADMIN')")
    public ResponseEntity<ApiResponse<ActivityBookingResponse>> attachBooking(
            @PathVariable UUID tripId,
            @Valid @RequestBody AttachActivityBookingRequest request
    ) {
        ActivityBookingResponse response = activityBookingService.attachBookingToTrip(tripId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Activity booking attached to trip"));
    }

    @DeleteMapping("/{bookingId}")
    @PreAuthorize("hasAnyRole('TRAVELER','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeBooking(
            @PathVariable UUID tripId,
            @PathVariable UUID bookingId
    ) {
        activityBookingService.removeBookingFromTrip(tripId, bookingId);
        return ResponseEntity.ok(ApiResponse.success(null, "Activity booking removed from trip"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TRAVELER','ADMIN')")
    public ResponseEntity<ApiResponse<List<ActivityBookingResponse>>> tripActivityBookings(@PathVariable UUID tripId) {
        List<ActivityBookingResponse> response = activityBookingService.getTripActivityBookings(tripId);
        return ResponseEntity.ok(ApiResponse.success(response, "Trip activity bookings retrieved"));
    }
}
