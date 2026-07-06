package com.travelease.backend.accommodation.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AccommodationSummaryResponse(
        UUID tripId,
        int bookingCount,
        BigDecimal totalAmount,
        List<HotelBookingResponse> bookings
) {
}
