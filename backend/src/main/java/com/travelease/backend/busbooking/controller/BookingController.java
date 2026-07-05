package com.travelease.backend.busbooking.controller;

import com.travelease.backend.busbooking.dto.request.BookingModificationRequest;
import com.travelease.backend.busbooking.dto.request.BookingRequest;
import com.travelease.backend.busbooking.dto.request.CancellationRequest;
import com.travelease.backend.busbooking.dto.request.PartialCancellationRequest;
import com.travelease.backend.busbooking.dto.response.ApiResponse;
import com.travelease.backend.busbooking.dto.response.BookingHistoryResponse;
import com.travelease.backend.busbooking.dto.response.BookingResponse;
import com.travelease.backend.busbooking.dto.response.BookingTimelineResponse;
import com.travelease.backend.busbooking.dto.response.CancellationResponse;
import com.travelease.backend.busbooking.dto.response.PaginatedSearchResponse;
import com.travelease.backend.busbooking.dto.response.TicketResponse;
import com.travelease.backend.busbooking.entity.enums.BookingStatus;
import com.travelease.backend.busbooking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Booking Management", description = "Endpoints for managing bus bookings")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @Operation(summary = "Create a new booking")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(@Valid @RequestBody BookingRequest request) {
        BookingResponse response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Booking created successfully", response, "/api/bookings"));
    }

    @GetMapping
    @Operation(summary = "Get bookings with optional filters")
    public ResponseEntity<ApiResponse<PaginatedSearchResponse<BookingHistoryResponse>>> getBookings(
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20) Pageable pageable) {
        PaginatedSearchResponse<BookingHistoryResponse> response = bookingService.getBookings(scope, status, reference, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Bookings fetched successfully", response, "/api/bookings"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking by ID")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(@PathVariable Long id) {
        BookingResponse response = bookingService.getBookingById(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Booking fetched successfully", response, "/api/bookings/" + id));
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬ Phase 6 Ã¢â‚¬â€œ Booking Lifecycle Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm a booking")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(@PathVariable Long id) {
        BookingResponse response = bookingService.confirmBooking(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Booking confirmed successfully", response, "/api/bookings/" + id + "/confirm"));
    }

    @PutMapping("/modify")
    @Operation(summary = "Modify booking details")
    public ResponseEntity<ApiResponse<BookingResponse>> modifyBooking(@Valid @RequestBody BookingModificationRequest request) {
        BookingResponse response = bookingService.modifyBooking(request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Booking modified successfully", response, "/api/bookings/modify"));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete a booking")
    public ResponseEntity<ApiResponse<BookingResponse>> completeBooking(@PathVariable Long id) {
        BookingResponse response = bookingService.completeBooking(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Booking completed successfully", response, "/api/bookings/" + id + "/complete"));
    }

    @GetMapping("/{id}/timeline")
    @Operation(summary = "Get booking timeline")
    public ResponseEntity<ApiResponse<List<BookingTimelineResponse>>> getBookingTimeline(@PathVariable Long id) {
        List<BookingTimelineResponse> response = bookingService.getBookingTimeline(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Booking timeline fetched successfully", response, "/api/bookings/" + id + "/timeline"));
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬ Phase 6 Ã¢â‚¬â€œ Ticket Management Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    @GetMapping("/{id}/ticket")
    @Operation(summary = "Get ticket for a booking")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicket(@PathVariable Long id) {
        TicketResponse response = bookingService.getTicket(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Ticket fetched successfully", response, "/api/bookings/" + id + "/ticket"));
    }

    @GetMapping("/ticket/verify/{ticketNumber}")
    @Operation(summary = "Verify ticket by ticket number")
    public ResponseEntity<ApiResponse<TicketResponse>> verifyTicket(@PathVariable String ticketNumber) {
        TicketResponse response = bookingService.verifyTicket(ticketNumber);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Ticket verified successfully", response, "/api/bookings/ticket/verify/" + ticketNumber));
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬ Phase 7 Ã¢â‚¬â€œ Enhanced Cancellation Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel booking with optional reason")
    public ResponseEntity<ApiResponse<CancellationResponse>> cancelBooking(
            @PathVariable Long id,
            @RequestBody(required = false) CancellationRequest request) {
        CancellationResponse response = bookingService.cancelBookingUnified(id, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Booking cancelled successfully", response, "/api/bookings/" + id + "/cancel"));
    }

    @PostMapping("/cancel/partial")
    @Operation(summary = "Partially cancel booking (selected seats)")
    public ResponseEntity<ApiResponse<CancellationResponse>> partialCancelBooking(@Valid @RequestBody PartialCancellationRequest request) {
        CancellationResponse response = bookingService.partialCancelBooking(request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Partial cancellation successful", response, "/api/bookings/cancel/partial"));
    }
}
