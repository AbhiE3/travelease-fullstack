package com.travelease.backend.trip.dto;

import com.travelease.backend.trip.entity.TravelerTripStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TripResponse(
        UUID tripId,
        String tripName,
        TripOrganizerSummary organizer,
        String sourceLocation,
        Integer destinationId,
        BigDecimal budgetAmount,
        Integer categoryId,
        LocalDate startDate,
        LocalDate endDate,
        TravelerTripStatus status,
        String viewerRole,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
