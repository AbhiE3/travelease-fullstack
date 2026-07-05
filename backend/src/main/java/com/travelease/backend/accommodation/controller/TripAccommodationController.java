package com.travelease.backend.accommodation.controller;

import com.travelease.backend.accommodation.dto.AccommodationSummaryResponse;
import com.travelease.backend.accommodation.dto.AttachHotelBookingRequest;
import com.travelease.backend.accommodation.dto.HotelBookingResponse;
import com.travelease.backend.accommodation.service.AccommodationService;
import com.travelease.backend.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse<HotelBookingResponse>> attachBooking(
            @PathVariable UUID tripId,
            @Valid @RequestBody AttachHotelBookingRequest request
    ) {
        HotelBookingResponse response = accommodationService.attachBookingToTrip(tripId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Hotel booking attached to trip"));
    }

    @DeleteMapping("/hotel-bookings/{bookingId}")
    public ResponseEntity<ApiResponse<Void>> removeBooking(
            @PathVariable UUID tripId,
            @PathVariable UUID bookingId
    ) {
        accommodationService.removeBookingFromTrip(tripId, bookingId);
        return ResponseEntity.ok(ApiResponse.success(null, "Hotel booking removed from trip"));
    }

    @GetMapping("/accommodation-summary")
    public ResponseEntity<ApiResponse<AccommodationSummaryResponse>> summary(@PathVariable UUID tripId) {
        AccommodationSummaryResponse response = accommodationService.getAccommodationSummary(tripId);
        return ResponseEntity.ok(ApiResponse.success(response, "Accommodation summary retrieved"));
    }
}
