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
    public ResponseEntity<ApiResponse<BookingValidationResponse>> validate(
            @Valid @RequestBody HotelBookingRequest request
    ) {
        BookingValidationResponse response = accommodationService.validateBooking(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Booking validation completed"));
    }

    @PostMapping("/quote")
    public ResponseEntity<ApiResponse<BookingQuoteResponse>> quote(@Valid @RequestBody HotelBookingRequest request) {
        BookingQuoteResponse response = accommodationService.quoteBooking(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Hotel booking quote generated"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<HotelBookingResponse>> create(
            @Valid @RequestBody HotelBookingRequest request,
            Authentication authentication
    ) {
        HotelBookingResponse response = accommodationService.createBooking(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Hotel booking confirmed"));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<HotelBookingResponse>> getBooking(
            @PathVariable UUID bookingId,
            Authentication authentication
    ) {
        HotelBookingResponse response = accommodationService.getBooking(bookingId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Hotel booking retrieved"));
    }

    @PutMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<HotelBookingResponse>> update(
            @PathVariable UUID bookingId,
            @Valid @RequestBody HotelBookingRequest request,
            Authentication authentication
    ) {
        HotelBookingResponse response = accommodationService.updateBooking(bookingId, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Hotel booking updated"));
    }

    @PutMapping("/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<HotelBookingResponse>> cancel(
            @PathVariable UUID bookingId,
            Authentication authentication
    ) {
        HotelBookingResponse response = accommodationService.cancelBooking(bookingId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Hotel booking cancelled"));
    }

    @GetMapping("/{bookingId}/bill")
    public ResponseEntity<ApiResponse<HotelBillResponse>> bill(
            @PathVariable UUID bookingId,
            Authentication authentication
    ) {
        HotelBillResponse response = accommodationService.getBill(bookingId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Hotel booking bill retrieved"));
    }
}
