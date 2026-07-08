package com.travelease.backend.busbooking.dto.request;

import jakarta.validation.constraints.NotNull;

public record AttachBusBookingRequest(@NotNull Long bookingId) {
}
