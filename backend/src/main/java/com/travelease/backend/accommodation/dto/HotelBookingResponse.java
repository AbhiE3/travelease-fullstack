package com.travelease.backend.accommodation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record HotelBookingResponse(
        UUID hotelBookingId,
        UUID tripId,
        UUID hotelId,
        String hotelName,
        UUID bookedByUserId,
        String bookedByUserName,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        String roomType,
        String roomNumber,
        BigDecimal totalAmount,
        String bookingStatus
) {
}
