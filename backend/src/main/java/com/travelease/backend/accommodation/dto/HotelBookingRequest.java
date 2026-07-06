package com.travelease.backend.accommodation.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record HotelBookingRequest(
        @NotNull UUID hotelId,
        @NotBlank String roomType,
        @NotNull @Future LocalDate checkInDate,
        @NotNull @Future LocalDate checkOutDate,
        @NotNull @Min(1) Integer guests
) {
}
