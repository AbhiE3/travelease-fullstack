package com.travelease.backend.busbooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Route analytics with performance metrics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteAnalyticsResponse {

    private Long routeId;
    private String source;
    private String destination;

    // Metrics
    private Double revenue;
    private Long bookingCount;
    private Long passengerCount;
    private Double occupancyPercentage;
    private Long tripCount;
    private Double distanceKm;
    private Double revenuePerKm;

    // Performance ranking
    private String performanceCategory; // MOST_POPULAR, HIGH_REVENUE, AVERAGE, LOW
}
