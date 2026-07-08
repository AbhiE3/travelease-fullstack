package com.travelease.backend.busbooking.dto.response;

import java.util.List;
import java.util.UUID;

public record TripBusBookingSummaryResponse(
        UUID tripId,
        int bookingCount,
        Double totalFare,
        List<TripBusBookingResponse> bookings
) {
}
