package com.travelease.backend.accommodation.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AttachHotelBookingRequest(@NotNull UUID bookingId) {
}
