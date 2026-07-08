package com.travelease.backend.accommodation.dto;

import java.util.UUID;

public record HotelReviewResponse(
        UUID reviewId,
        UUID hotelId,
        UUID userId,
        String userName,
        Integer rating,
        String comment
) {
}
