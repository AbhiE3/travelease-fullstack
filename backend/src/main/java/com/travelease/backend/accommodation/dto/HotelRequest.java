package com.travelease.backend.accommodation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record HotelRequest(
        @NotNull Integer destinationId,
        @NotBlank String hotelName,
        @NotBlank String address,
        @DecimalMin("0.0") BigDecimal rating,
        @Positive BigDecimal pricePerNight,
        String amenities,
        @NotBlank String status
) {
}
