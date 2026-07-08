package com.travelease.backend.admin.dto;

public record DestinationResponse(
        Integer destinationId,
        String destinationName,
        String state,
        String country,
        String description
) {
}
