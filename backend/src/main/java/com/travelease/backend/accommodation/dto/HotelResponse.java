package com.travelease.backend.accommodation.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record HotelResponse(
        UUID hotelId,
        Integer destinationId,
        String hotelName,
        String address,
        BigDecimal rating,
        BigDecimal pricePerNight,
        String amenities,
        String status,
        String policies
) {
}
