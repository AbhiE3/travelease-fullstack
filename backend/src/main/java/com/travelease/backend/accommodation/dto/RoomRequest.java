package com.travelease.backend.accommodation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RoomRequest(
        @NotBlank String roomType,
        @NotNull @Min(1) Integer capacity,
        @NotBlank String bedType,
        @NotNull @DecimalMin("0.0") BigDecimal pricePerNight,
        String availabilityStatus
) {
}
