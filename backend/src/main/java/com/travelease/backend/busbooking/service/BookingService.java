package com.travelease.backend.busbooking.service;

import com.travelease.backend.busbooking.dto.request.BookingModificationRequest;
import com.travelease.backend.busbooking.dto.request.BookingRequest;
import com.travelease.backend.busbooking.dto.request.CancellationRequest;
import com.travelease.backend.busbooking.dto.request.PartialCancellationRequest;
import com.travelease.backend.busbooking.dto.response.*;
import com.travelease.backend.busbooking.entity.enums.BookingStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface BookingService {

    // Core CRUD
    BookingResponse createBooking(BookingRequest request);
    BookingResponse getBookingById(Long id);
    BookingResponse getBookingByReference(String ref);

    // Consolidated retrieval (replaces history/upcoming/past/search)
    PaginatedSearchResponse<BookingHistoryResponse> getBookings(String scope, BookingStatus status, String reference, LocalDate from, LocalDate to, Pageable pageable);

    // Phase 6 Ã¢â‚¬â€œ Booking Lifecycle
    BookingResponse confirmBooking(Long bookingId);
    BookingResponse modifyBooking(BookingModificationRequest request);
    BookingResponse completeBooking(Long bookingId);
    List<BookingTimelineResponse> getBookingTimeline(Long bookingId);

    // Phase 6 Ã¢â‚¬â€œ Ticket Management
    TicketResponse getTicket(Long bookingId);
    TicketResponse verifyTicket(String ticketNumber);

    // Phase 6 Ã¢â‚¬â€œ Admin lifecycle
    void expireBookings();

    // Phase 7 Ã¢â‚¬â€œ Enhanced Cancellation (unified)
    CancellationResponse cancelBookingUnified(Long bookingId, CancellationRequest request);
    CancellationResponse partialCancelBooking(PartialCancellationRequest request);
}
