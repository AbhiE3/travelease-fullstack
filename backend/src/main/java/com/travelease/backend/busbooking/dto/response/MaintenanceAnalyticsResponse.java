package com.travelease.backend.busbooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Maintenance analytics summary.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceAnalyticsResponse {

    private Long providerId;

    // Cost metrics
    private Double totalMaintenanceCost;
    private Double averageCostPerBus;
    private Long maintenanceCount;

    // Downtime
    private Long totalDowntimeDays;
    private Double averageDowntimePerBus;

    // Frequency
    private Double maintenanceFrequencyPerMonth;

    // Upcoming
    private List<UpcomingMaintenanceItem> upcomingMaintenance;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpcomingMaintenanceItem {
        private Long maintenanceId;
        private Long busId;
        private String busNumber;
        private String maintenanceType;
        private LocalDate scheduledDate;
    }
}
