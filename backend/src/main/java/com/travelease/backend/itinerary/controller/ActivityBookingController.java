package com.travelease.backend.itinerary.controller;

import com.travelease.backend.itinerary.dto.ActivityBookingResponse;
import com.travelease.backend.itinerary.dto.CreateActivityBookingRequest;
import com.travelease.backend.itinerary.service.ActivityBookingService;
import com.travelease.backend.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Traveler-facing Activity Booking endpoints - creating, viewing, and
 * cancelling one's own bookings. Provider-side visibility/attendance
 * endpoints live on ActivityProviderController; slot discovery (browsing
 * bookable slots for an Activity) lives on ActivityController, since that
 * class already owns the public/unauthenticated Activity catalog surface.
 */
@RestController
@RequestMapping("/api/activity-bookings")
@RequiredArgsConstructor
public class ActivityBookingController {

    private final ActivityBookingService activityBookingService;

    @PostMapping
    @PreAuthorize("hasAnyRole('TRAVELER','ADMIN')")
    public ResponseEntity<ApiResponse<ActivityBookingResponse>> createBooking(
            @Valid @RequestBody CreateActivityBookingRequest request
    ) {
        ActivityBookingResponse response = activityBookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Activity booking created"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TRAVELER','ADMIN')")
    public ResponseEntity<ApiResponse<List<ActivityBookingResponse>>> getMyBookings() {
        List<ActivityBookingResponse> response = activityBookingService.getMyBookings();
        return ResponseEntity.ok(ApiResponse.success(response, "Your activity bookings retrieved"));
    }

    @GetMapping("/{bookingId}")
    @PreAuthorize("hasAnyRole('TRAVELER','ADMIN')")
    public ResponseEntity<ApiResponse<ActivityBookingResponse>> getMyBooking(@PathVariable UUID bookingId) {
        ActivityBookingResponse response = activityBookingService.getMyBooking(bookingId);
        return ResponseEntity.ok(ApiResponse.success(response, "Activity booking retrieved"));
    }

    @PutMapping("/{bookingId}/cancel")
    @PreAuthorize("hasAnyRole('TRAVELER','ADMIN')")
    public ResponseEntity<ApiResponse<ActivityBookingResponse>> cancelBooking(@PathVariable UUID bookingId) {
        ActivityBookingResponse response = activityBookingService.cancelBooking(bookingId);
        return ResponseEntity.ok(ApiResponse.success(response, "Activity booking cancelled"));
    }
}
