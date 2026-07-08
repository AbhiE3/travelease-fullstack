package com.travelease.backend.trip.dto;

import com.travelease.backend.trip.entity.TravelerTripStatus;
import jakarta.validation.constraints.NotNull;

public record TripStatusTransitionRequest(@NotNull TravelerTripStatus status) {
}
