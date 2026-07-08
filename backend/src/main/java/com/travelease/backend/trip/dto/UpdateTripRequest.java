package com.travelease.backend.trip.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Trip.status is intentionally absent here - lifecycle transitions have their
 * own state-machine validation, authorization, and side effects distinct from
 * ordinary metadata edits, so they go through the dedicated
 * PATCH /api/trips/{tripId}/status endpoint instead of this generic update.
 * There is exactly one way to change a Trip's status.
 */
public record UpdateTripRequest(
        @NotBlank String tripName,
        @NotBlank String sourceLocation,
        @NotNull Integer destinationId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal budgetAmount,
        @NotNull Integer categoryId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
}
