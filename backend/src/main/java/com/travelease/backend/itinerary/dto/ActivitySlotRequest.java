package com.travelease.backend.itinerary.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record ActivitySlotRequest(
        @NotNull LocalDate activityDate,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotNull @Positive BigDecimal price,
        @NotNull @Positive Integer capacity
) {
}
