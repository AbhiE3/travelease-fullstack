package com.travelease.backend.itinerary.controller;

import com.travelease.backend.itinerary.dto.ActivityBookingResponse;
import com.travelease.backend.itinerary.dto.ActivityBookingStatusTransitionRequest;
import com.travelease.backend.itinerary.dto.ActivityProviderRequest;
import com.travelease.backend.itinerary.dto.ActivityProviderResponse;
import com.travelease.backend.itinerary.dto.ActivitySlotRequest;
import com.travelease.backend.itinerary.dto.ActivitySlotResponse;
import com.travelease.backend.itinerary.service.ActivityBookingService;
import com.travelease.backend.itinerary.service.ActivityProviderService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Activity Provider management only - the existing traveler/reference lookup
 * (GET /api/activities?destinationId=) stays on ActivityController, untouched.
 * Deliberately namespaced /api/activity-provider (not the shared /api/provider
 * Hotel already uses) to avoid a third provider type sharing an ambiguous,
 * generic-sounding prefix.
 */
@RestController
@RequestMapping("/api/activity-provider")
@RequiredArgsConstructor
public class ActivityProviderController {

    private final ActivityProviderService activityProviderService;
    private final ActivityBookingService activityBookingService;

    @PostMapping("/activities")
    @PreAuthorize("hasAnyRole('ADMIN','ACTIVITY_PROVIDER')")
    public ResponseEntity<ApiResponse<ActivityProviderResponse>> createActivity(
            @Valid @RequestBody ActivityProviderRequest request
    ) {
        ActivityProviderResponse response = activityProviderService.createActivity(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Activity created"));
    }

    @GetMapping("/activities")
    @PreAuthorize("hasAnyRole('ADMIN','ACTIVITY_PROVIDER')")
    public ResponseEntity<ApiResponse<List<ActivityProviderResponse>>> activities(
            @RequestParam(required = false) Long providerId
    ) {
        List<ActivityProviderResponse> response = activityProviderService.getProviderActivities(providerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Provider activities retrieved"));
    }

    @GetMapping("/activities/{activityId}")
    @PreAuthorize("hasAnyRole('ADMIN','ACTIVITY_PROVIDER')")
    public ResponseEntity<ApiResponse<ActivityProviderResponse>> activityDetails(@PathVariable String activityId) {
        ActivityProviderResponse response = activityProviderService.getProviderActivity(activityId);
        return ResponseEntity.ok(ApiResponse.success(response, "Provider activity details retrieved"));
    }

    @PutMapping("/activities/{activityId}")
    @PreAuthorize("hasAnyRole('ADMIN','ACTIVITY_PROVIDER')")
    public ResponseEntity<ApiResponse<ActivityProviderResponse>> updateActivity(
            @PathVariable String activityId,
            @Valid @RequestBody ActivityProviderRequest request
    ) {
        ActivityProviderResponse response = activityProviderService.updateActivity(activityId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Activity updated"));
    }

    @PostMapping("/activities/{activityId}/slots")
    @PreAuthorize("hasAnyRole('ADMIN','ACTIVITY_PROVIDER')")
    public ResponseEntity<ApiResponse<ActivitySlotResponse>> createSlot(
            @PathVariable String activityId,
            @Valid @RequestBody ActivitySlotRequest request
    ) {
        ActivitySlotResponse response = activityProviderService.createSlot(activityId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Activity slot created"));
    }

    @GetMapping("/activities/{activityId}/slots")
    @PreAuthorize("hasAnyRole('ADMIN','ACTIVITY_PROVIDER')")
    public ResponseEntity<ApiResponse<List<ActivitySlotResponse>>> slots(@PathVariable String activityId) {
        List<ActivitySlotResponse> response = activityProviderService.getSlots(activityId);
        return ResponseEntity.ok(ApiResponse.success(response, "Activity slots retrieved"));
    }

    @PutMapping("/activities/{activityId}/slots/{slotId}")
    @PreAuthorize("hasAnyRole('ADMIN','ACTIVITY_PROVIDER')")
    public ResponseEntity<ApiResponse<ActivitySlotResponse>> updateSlot(
            @PathVariable String activityId,
            @PathVariable UUID slotId,
            @Valid @RequestBody ActivitySlotRequest request
    ) {
        ActivitySlotResponse response = activityProviderService.updateSlot(activityId, slotId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Activity slot updated"));
    }

    @GetMapping("/activities/{activityId}/bookings")
    @PreAuthorize("hasAnyRole('ADMIN','ACTIVITY_PROVIDER')")
    public ResponseEntity<ApiResponse<List<ActivityBookingResponse>>> bookingsForActivity(
            @PathVariable String activityId
    ) {
        List<ActivityBookingResponse> response = activityBookingService.getBookingsForActivity(activityId);
        return ResponseEntity.ok(ApiResponse.success(response, "Activity bookings retrieved"));
    }

    @GetMapping("/bookings/{bookingId}")
    @PreAuthorize("hasAnyRole('ADMIN','ACTIVITY_PROVIDER')")
    public ResponseEntity<ApiResponse<ActivityBookingResponse>> bookingDetails(@PathVariable UUID bookingId) {
        ActivityBookingResponse response = activityBookingService.getProviderBooking(bookingId);
        return ResponseEntity.ok(ApiResponse.success(response, "Activity booking retrieved"));
    }

    @PutMapping("/bookings/{bookingId}/attendance")
    @PreAuthorize("hasAnyRole('ADMIN','ACTIVITY_PROVIDER')")
    public ResponseEntity<ApiResponse<ActivityBookingResponse>> markAttendance(
            @PathVariable UUID bookingId,
            @Valid @RequestBody ActivityBookingStatusTransitionRequest request
    ) {
        ActivityBookingResponse response = activityBookingService.markAttendance(bookingId, request.status());
        return ResponseEntity.ok(ApiResponse.success(response, "Activity booking attendance updated"));
    }
}
