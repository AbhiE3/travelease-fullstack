package com.travelease.backend.busbooking.dto.response;

import com.travelease.backend.busbooking.entity.enums.BookingStatus;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Lightweight Bus Booking summary for shared Traveler Trip views. Deliberately
 * excludes passenger names/ages/genders, contact details, ticket QR data, and
 * timeline history that the full BookingResponse carries - a fellow accepted
 * Trip member should be able to see "which bus, which date, what it cost,
 * confirmed or not" without seeing another traveler's private booking details.
 */
public record TripBusBookingResponse(
        Long bookingId,
        String bookingReference,
        BookingStatus status,
        Double totalFare,
        Long scheduleId,
        LocalDate travelDate,
        String source,
        String destination,
        UUID bookedByUserId,
        UUID travelerTripId
) {
}
