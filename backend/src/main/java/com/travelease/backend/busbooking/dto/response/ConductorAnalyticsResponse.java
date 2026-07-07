package com.travelease.backend.busbooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Conductor analytics with performance metrics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConductorAnalyticsResponse {

    private Long conductorId;
    private String conductorName;
    private String employeeId;
    private Long providerId;

    // Trip metrics
    private Long totalTrips;
    private Long completedTrips;
    private Long passengerHandling;
    private Double rating;

    // Performance
    private Long rank;
    private String performanceCategory; // TOP, GOOD, AVERAGE, NEEDS_IMPROVEMENT
}
