package com.travelease.backend.accommodation.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RoomResponse(
        UUID roomId,
        UUID hotelId,
        String roomType,
        Integer capacity,
        String bedType,
        BigDecimal pricePerNight,
        String availabilityStatus
) {
}
