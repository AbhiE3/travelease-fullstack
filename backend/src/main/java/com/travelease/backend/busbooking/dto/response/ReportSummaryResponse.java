package com.travelease.backend.busbooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Report summary statistics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportSummaryResponse {

    private Long totalRecords;
    private Double totalRevenue;
    private Long totalBookings;
    private Long totalPassengers;
    private Long totalRefunds;
    private Long totalCancellations;
    private Double occupancyPercentage;
    private Double fleetUtilization;
}
