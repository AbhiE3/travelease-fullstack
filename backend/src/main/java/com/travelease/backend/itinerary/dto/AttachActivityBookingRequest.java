package com.travelease.backend.itinerary.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AttachActivityBookingRequest(@NotNull UUID bookingId) {
}
