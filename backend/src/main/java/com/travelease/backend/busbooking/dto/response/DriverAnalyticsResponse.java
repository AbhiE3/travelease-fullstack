package com.travelease.backend.busbooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Driver analytics with performance metrics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverAnalyticsResponse {

    private Long driverId;
    private String driverName;
    private String licenseNumber;
    private Long providerId;

    // Trip metrics
    private Long totalTrips;
    private Long completedTrips;
    private Double distanceCovered;
    private Double rating;

    // Utilization
    private Double utilizationPercentage;
    private Long rank;

    // Performance
    private String performanceCategory; // TOP, GOOD, AVERAGE, NEEDS_IMPROVEMENT
}
