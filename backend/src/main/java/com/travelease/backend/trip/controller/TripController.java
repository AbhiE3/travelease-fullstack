package com.travelease.backend.trip.controller;

import com.travelease.backend.shared.dto.ApiResponse;
import com.travelease.backend.trip.dto.AddTripMemberRequest;
import com.travelease.backend.trip.dto.CreateTripRequest;
import com.travelease.backend.trip.dto.PendingInvitationResponse;
import com.travelease.backend.trip.dto.TripMemberResponse;
import com.travelease.backend.trip.dto.TripResponse;
import com.travelease.backend.trip.dto.TripStatusTransitionRequest;
import com.travelease.backend.trip.dto.UpdateTripRequest;
import com.travelease.backend.trip.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @PostMapping
    @PreAuthorize("hasRole('TRAVELER')")
    public ResponseEntity<ApiResponse<TripResponse>> createTrip(
            @Valid @RequestBody CreateTripRequest request,
            Authentication authentication
    ) {
        TripResponse response = tripService.createTrip(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Trip created successfully"));
    }

    @GetMapping
    @PreAuthorize("hasRole('TRAVELER')")
    public ResponseEntity<ApiResponse<List<TripResponse>>> getMyTrips(Authentication authentication) {
        List<TripResponse> response = tripService.getMyTrips(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Trips retrieved"));
    }

    @GetMapping("/invitations")
    @PreAuthorize("hasRole('TRAVELER')")
    public ResponseEntity<ApiResponse<List<PendingInvitationResponse>>> getMyPendingInvitations(Authentication authentication) {
        List<PendingInvitationResponse> response = tripService.getMyPendingInvitations(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Pending trip invitations retrieved"));
    }

    @GetMapping("/{tripId}")
    @PreAuthorize("hasAnyRole('TRAVELER','ADMIN')")
    public ResponseEntity<ApiResponse<TripResponse>> getTripById(
            @PathVariable UUID tripId,
            Authentication authentication
    ) {
        TripResponse response = tripService.getTripById(tripId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Trip retrieved"));
    }

    @PutMapping("/{tripId}")
    @PreAuthorize("hasAnyRole('TRAVELER','ADMIN')")
    public ResponseEntity<ApiResponse<TripResponse>> updateTrip(
            @PathVariable UUID tripId,
            @Valid @RequestBody UpdateTripRequest request,
            Authentication authentication
    ) {
        TripResponse response = tripService.updateTrip(tripId, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Trip updated successfully"));
    }

    @PatchMapping("/{tripId}/status")
    @PreAuthorize("hasAnyRole('TRAVELER','ADMIN')")
    public ResponseEntity<ApiResponse<TripResponse>> transitionStatus(
            @PathVariable UUID tripId,
            @Valid @RequestBody TripStatusTransitionRequest request,
            Authentication authentication
    ) {
        TripResponse response = tripService.transitionStatus(tripId, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Trip status updated"));
    }

    @GetMapping("/{tripId}/members")
    @PreAuthorize("hasAnyRole('TRAVELER','ADMIN')")
    public ResponseEntity<ApiResponse<List<TripMemberResponse>>> getTripMembers(
            @PathVariable UUID tripId,
            Authentication authentication
    ) {
        List<TripMemberResponse> response = tripService.getTripMembers(tripId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Trip members retrieved"));
    }

    @PostMapping("/{tripId}/members")
    @PreAuthorize("hasAnyRole('TRAVELER','ADMIN')")
    public ResponseEntity<ApiResponse<TripMemberResponse>> addTripMember(
            @PathVariable UUID tripId,
            @Valid @RequestBody AddTripMemberRequest request,
            Authentication authentication
    ) {
        TripMemberResponse response = tripService.addTripMember(tripId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Trip invitation sent"));
    }

    @PatchMapping("/{tripId}/members/{tripMemberId}/accept")
    @PreAuthorize("hasRole('TRAVELER')")
    public ResponseEntity<ApiResponse<TripMemberResponse>> acceptTripMemberInvitation(
            @PathVariable UUID tripId,
            @PathVariable UUID tripMemberId,
            Authentication authentication
    ) {
        TripMemberResponse response = tripService.acceptTripMemberInvitation(tripId, tripMemberId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Trip invitation accepted"));
    }

    @PatchMapping("/{tripId}/members/{tripMemberId}/reject")
    @PreAuthorize("hasRole('TRAVELER')")
    public ResponseEntity<ApiResponse<TripMemberResponse>> rejectTripMemberInvitation(
            @PathVariable UUID tripId,
            @PathVariable UUID tripMemberId,
            Authentication authentication
    ) {
        TripMemberResponse response = tripService.rejectTripMemberInvitation(tripId, tripMemberId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Trip invitation rejected"));
    }

    @DeleteMapping("/{tripId}/members/{tripMemberId}")
    @PreAuthorize("hasAnyRole('TRAVELER','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeTripMember(
            @PathVariable UUID tripId,
            @PathVariable UUID tripMemberId,
            Authentication authentication
    ) {
        tripService.removeTripMember(tripId, tripMemberId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "Trip member removed"));
    }
}
