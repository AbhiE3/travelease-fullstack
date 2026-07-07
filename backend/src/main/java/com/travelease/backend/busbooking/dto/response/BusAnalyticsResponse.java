package com.travelease.backend.busbooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bus analytics with utilization and performance metrics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusAnalyticsResponse {

    private Long busId;
    private String busNumber;
    private String busName;
    private Long providerId;

    // Utilization
    private Double utilizationPercentage;
    private Double occupancyPercentage;
    private Double revenue;
    private Long tripCount;
    private Long bookingCount;
    private Long totalSeats;
    private Long seatsSold;

    // Performance ranking
    private String performanceCategory; // BEST, GOOD, AVERAGE, LOW
}
