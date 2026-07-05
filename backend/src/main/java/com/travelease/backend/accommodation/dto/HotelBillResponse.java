package com.travelease.backend.accommodation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record HotelBillResponse(
        UUID bookingId,
        String hotelName,
        String roomType,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        long nights,
        BigDecimal totalAmount,
        String bookingStatus
) {
}
