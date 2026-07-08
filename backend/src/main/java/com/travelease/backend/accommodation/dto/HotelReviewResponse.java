package com.travelease.backend.accommodation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record HotelReviewResponse(
        UUID reviewId,
        UUID hotelId,
        UUID userId,
        String userName,
        BigDecimal rating,
        String comment,
        LocalDateTime createdAt
) {
}
