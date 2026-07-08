package com.travelease.backend.busbooking.controller;

import com.travelease.backend.busbooking.dto.request.AttachBusBookingRequest;
import com.travelease.backend.busbooking.dto.response.ApiResponse;
import com.travelease.backend.busbooking.dto.response.TripBusBookingResponse;
import com.travelease.backend.busbooking.dto.response.TripBusBookingSummaryResponse;
import com.travelease.backend.busbooking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Attach/detach/list only - Bus Booking's own lifecycle (create, confirm,
 * cancel, ticket, timeline) remains exclusively BookingController's
 * responsibility. Mirrors the existing accommodation.TripAccommodationController
 * precedent for HotelBooking-to-Trip association, adapted to this module's own
 * SecurityUtil-based current-user resolution convention instead of the
 * Authentication-parameter convention used there.
 */
@RestController
@RequestMapping("/api/trips/{tripId}")
@RequiredArgsConstructor
@Tag(name = "Trip Bus Booking", description = "Attach/detach/list Bus Bookings associated with a Traveler Trip")
public class TripBusBookingController {

    private final BookingService bookingService;

    @PostMapping("/bus-bookings")
    @PreAuthorize("hasAnyRole('TRAVELER','ADMIN')")
    @Operation(summary = "Attach an owned Bus Booking to a Traveler Trip", description = "Attach an owned Bus Booking to a Traveler Trip")
    public ResponseEntity<ApiResponse<TripBusBookingResponse>> attachBooking(
            @PathVariable UUID tripId,
            @Valid @RequestBody AttachBusBookingRequest request
    ) {
        TripBusBookingResponse response = bookingService.attachBookingToTrip(tripId, request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "Bus booking attached to trip", response, "/api/trips/" + tripId + "/bus-bookings"));
    }

    @DeleteMapping("/bus-bookings/{bookingId}")
    @PreAuthorize("hasAnyRole('TRAVELER','ADMIN')")
    @Operation(summary = "Detach an owned Bus Booking from a Traveler Trip", description = "Detach an owned Bus Booking from a Traveler Trip")
    public ResponseEntity<ApiResponse<Void>> removeBooking(
            @PathVariable UUID tripId,
            @PathVariable Long bookingId
    ) {
        bookingService.removeBookingFromTrip(tripId, bookingId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Bus booking removed from trip", null, "/api/trips/" + tripId + "/bus-bookings/" + bookingId));
    }

    @GetMapping("/bus-bookings")
    @PreAuthorize("hasAnyRole('TRAVELER','ADMIN')")
    @Operation(summary = "List Bus Bookings associated with a Traveler Trip", description = "List Bus Bookings associated with a Traveler Trip")
    public ResponseEntity<ApiResponse<TripBusBookingSummaryResponse>> tripBusBookings(@PathVariable UUID tripId) {
        TripBusBookingSummaryResponse response = bookingService.getTripBusBookings(tripId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Trip bus bookings retrieved", response, "/api/trips/" + tripId + "/bus-bookings"));
    }
}

