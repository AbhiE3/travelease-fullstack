package com.travelease.backend.itinerary.dto;

public record ActivityProviderResponse(
        String activityId,
        Long providerId,
        Integer destinationId,
        String activityName,
        Double durationHours,
        String startTime,
        String endTime,
        String description
) {
}
