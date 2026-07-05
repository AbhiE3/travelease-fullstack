package com.travelease.backend.accommodation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record RoomRequest(
        @NotBlank String roomType,
        @Positive Integer capacity,
        @NotBlank String bedType,
        @Positive BigDecimal pricePerNight,
        @NotBlank String availabilityStatus
) {
}
