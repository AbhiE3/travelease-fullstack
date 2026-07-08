package com.travelease.backend.itinerary.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record CreateActivityBookingRequest(
        @NotNull UUID activitySlotId,
        @NotNull @Positive Integer participantCount
) {
}
