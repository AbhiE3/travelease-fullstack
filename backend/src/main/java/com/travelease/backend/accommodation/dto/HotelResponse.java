package com.travelease.backend.accommodation.dto;

import java.util.UUID;

public record HotelResponse(
        UUID hotelId,
        String name,
        Integer destinationId,
        String address,
        String description,
        String status,
        String policies
) {
}
