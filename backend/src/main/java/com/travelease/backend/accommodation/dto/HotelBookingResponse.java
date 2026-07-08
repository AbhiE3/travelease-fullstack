package com.travelease.backend.accommodation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record HotelBookingResponse(
        UUID bookingId,
        UUID hotelId,
        String hotelName,
        UUID roomId,
        String roomType,
        UUID tripId,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Integer guests,
        String bookingStatus,
        BigDecimal totalAmount
) {
}
