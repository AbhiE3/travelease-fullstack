package com.travelease.backend.trip.dto;

import java.util.UUID;

public record TripOrganizerSummary(UUID userId, String name, String email) {
}
