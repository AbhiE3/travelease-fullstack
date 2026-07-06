package com.travelease.backend.accommodation.controller;

import com.travelease.backend.accommodation.dto.AccommodationSummaryResponse;
import com.travelease.backend.accommodation.dto.AttachHotelBookingRequest;
import com.travelease.backend.accommodation.dto.HotelBookingResponse;
import com.travelease.backend.accommodation.service.AccommodationService;
import com.travelease.backend.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/trips/{tripId}")
@RequiredArgsConstructor
public class TripAccommodationController {

    private final AccommodationService accommodationService;

    @PostMapping("/hotel-bookings")
    public ResponseEntity<ApiResponse<HotelBookingResponse>> attachBookingToTrip(
            @PathVariable UUID tripId,
            @Valid @RequestBody AttachHotelBookingRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                accommodationService.attachBookingToTrip(tripId, request, authentication.getName()),
                "Hotel booking attached to trip"
        ));
    }

    @DeleteMapping("/hotel-bookings/{bookingId}")
    public ResponseEntity<ApiResponse<Void>> removeBookingFromTrip(
            @PathVariable UUID tripId,
            @PathVariable UUID bookingId,
            Authentication authentication
    ) {
        accommodationService.removeBookingFromTrip(tripId, bookingId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "Hotel booking removed from trip"));
    }

    @GetMapping("/accommodation-summary")
    public ResponseEntity<ApiResponse<AccommodationSummaryResponse>> getSummary(
            @PathVariable UUID tripId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                accommodationService.getTripAccommodationSummary(tripId, authentication.getName()),
                "Accommodation summary retrieved"
        ));
    }
}
