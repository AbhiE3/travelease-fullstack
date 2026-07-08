package com.travelease.backend.accommodation.controller;

import com.travelease.backend.accommodation.dto.BookingQuoteResponse;
import com.travelease.backend.accommodation.dto.BookingValidationResponse;
import com.travelease.backend.accommodation.dto.HotelBillResponse;
import com.travelease.backend.accommodation.dto.HotelBookingRequest;
import com.travelease.backend.accommodation.dto.HotelBookingResponse;
import com.travelease.backend.accommodation.service.AccommodationService;
import com.travelease.backend.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/hotel-bookings")
@RequiredArgsConstructor
public class HotelBookingController {

    private final AccommodationService accommodationService;

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<BookingValidationResponse>> validateBooking(
            @Valid @RequestBody HotelBookingRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(accommodationService.validateBooking(request), "Booking validated"));
    }

    @PostMapping("/quote")
    public ResponseEntity<ApiResponse<BookingQuoteResponse>> quoteBooking(
            @Valid @RequestBody HotelBookingRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(accommodationService.quoteBooking(request), "Booking quote generated"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<HotelBookingResponse>> createBooking(
            @Valid @RequestBody HotelBookingRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                accommodationService.createBooking(request, authentication.getName()),
                "Hotel booking confirmed"
        ));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<HotelBookingResponse>> getBooking(
            @PathVariable UUID bookingId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                accommodationService.getBooking(bookingId, authentication.getName()),
                "Hotel booking retrieved"
        ));
    }

    @PutMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<HotelBookingResponse>> modifyBooking(
            @PathVariable UUID bookingId,
            @Valid @RequestBody HotelBookingRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                accommodationService.modifyBooking(bookingId, request, authentication.getName()),
                "Hotel booking updated"
        ));
    }

    @PutMapping("/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<HotelBookingResponse>> cancelBooking(
            @PathVariable UUID bookingId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                accommodationService.cancelBooking(bookingId, authentication.getName()),
                "Hotel booking cancelled"
        ));
    }

    @GetMapping("/{bookingId}/bill")
    public ResponseEntity<ApiResponse<HotelBillResponse>> getBill(
            @PathVariable UUID bookingId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                accommodationService.getBill(bookingId, authentication.getName()),
                "Hotel bill generated"
        ));
    }
}
