package com.travelease.backend.accommodation.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record HotelBillResponse(
        UUID bookingId,
        String hotelName,
        String guestName,
        String roomType,
        long nights,
        BigDecimal totalAmount,
        String bookingStatus
) {
}
