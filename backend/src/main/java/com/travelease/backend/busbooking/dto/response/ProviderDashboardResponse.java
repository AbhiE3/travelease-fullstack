package com.travelease.backend.busbooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Provider dashboard summary with KPI cards and charts.
 *
 * The nested *Summary sections are intentionally lightweight (counts and a small
 * top-N list, not full analytics payloads) so this endpoint stays cheap for initial
 * page load. Full detail lives in the dedicated /api/analytics/{buses,routes,drivers,
 * conductors,maintenance} endpoints for drill-down screens.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderDashboardResponse {

    private Long providerId;

    // KPI Cards
    private KpiCard todayBookings;
    private KpiCard todayRevenue;
    private KpiCard weeklyRevenue;
    private KpiCard monthlyRevenue;
    private KpiCard totalRevenue;
    private KpiCard activeTrips;
    private KpiCard runningTrips;
    private KpiCard completedTrips;
    private KpiCard cancelledTrips;
    private KpiCard delayedTrips;
    private KpiCard totalPassengers;
    private KpiCard fleetAvailability;

    // Chart data
    private List<ChartDataPoint> revenueTrend;
    private List<ChartDataPoint> bookingTrend;
    private List<ChartDataPoint> tripStatusDistribution;

    // Lightweight widget sections (see class-level note)
    private FleetSummary fleetSummary;
    private StaffSummary staffSummary;
    private MaintenanceSummary maintenanceSummary;
    private List<TopRoute> topRoutes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FleetSummary {
        private Long totalBuses;
        private Long activeBuses;
        private Long maintenanceBuses;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StaffSummary {
        private Long activeDrivers;
        private Long activeConductors;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MaintenanceSummary {
        private Long upcomingCount;
        private List<MaintenanceAnalyticsResponse.UpcomingMaintenanceItem> nextItems;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopRoute {
        private Long routeId;
        private String source;
        private String destination;
        private Long bookingCount;
        private Double revenue;
    }
}
