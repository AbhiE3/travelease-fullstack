package com.travelease.backend.itinerary.dto;

import com.travelease.backend.itinerary.entity.ActivityBookingStatus;
import jakarta.validation.constraints.NotNull;

public record ActivityBookingStatusTransitionRequest(@NotNull ActivityBookingStatus status) {
}
