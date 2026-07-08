package com.travelease.backend.accommodation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HotelRequest(
        @NotBlank String name,
        @NotNull Integer destinationId,
        @NotBlank String address,
        String description,
        String status
) {
}
