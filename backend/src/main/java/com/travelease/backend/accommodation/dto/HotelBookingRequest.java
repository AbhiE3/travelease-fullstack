package com.travelease.backend.accommodation.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record HotelBookingRequest(
        UUID tripId,
        @NotNull UUID hotelId,
        @NotNull @FutureOrPresent LocalDate checkInDate,
        @NotNull @FutureOrPresent LocalDate checkOutDate,
        @NotBlank String roomType,
        String roomNumber
) {
}
