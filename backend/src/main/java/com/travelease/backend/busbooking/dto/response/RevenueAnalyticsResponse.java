package com.travelease.backend.busbooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Revenue analytics with growth and breakdown.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueAnalyticsResponse {

    private Long providerId;

    // Effective date range that rangeRevenue / dailyRevenueTrend were calculated over
    private LocalDate rangeStart;
    private LocalDate rangeEnd;

    // Revenue metrics
    private Double dailyRevenue;
    private Double weeklyRevenue;
    private Double monthlyRevenue;
    private Double totalRevenue;
    private Double rangeRevenue;
    private Double revenueGrowthPercent;

    // Averages
    private Double averageBookingValue;
    private Double averageFare;

    // Coupon & discount
    private Long couponUsageCount;
    private Double totalCouponDiscount;
    private Double totalDiscountAmount;

    // Trends
    private List<ChartDataPoint> dailyRevenueTrend;
    private List<ChartDataPoint> weeklyRevenueTrend;
    private List<ChartDataPoint> monthlyRevenueTrend;
}
