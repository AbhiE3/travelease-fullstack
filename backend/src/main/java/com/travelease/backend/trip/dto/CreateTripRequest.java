package com.travelease.backend.trip.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTripRequest(
        @NotBlank String tripName,
        @NotBlank String sourceLocation,
        @NotNull Integer destinationId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal budgetAmount,
        @NotNull Integer categoryId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
}
