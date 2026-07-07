package com.travelease.backend.busbooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsResponse {

    private Long totalBuses;
    private Long totalRoutes;
    private Long totalBookings;
    private Double totalRevenue;
    private Long todayBookings;
    private Long activeSchedules;
}
