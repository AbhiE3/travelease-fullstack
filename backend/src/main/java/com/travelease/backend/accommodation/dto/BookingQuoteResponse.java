package com.travelease.backend.accommodation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BookingQuoteResponse(
        UUID hotelId,
        UUID roomId,
        String roomType,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        long nights,
        BigDecimal pricePerNight,
        BigDecimal totalAmount
) {
}
