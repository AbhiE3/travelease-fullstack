package com.travelease.backend.busbooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Booking analytics with trends and patterns.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingAnalyticsResponse {

    private Long providerId;

    // Effective date range this response was calculated over
    private LocalDate rangeStart;
    private LocalDate rangeEnd;

    // Trends
    private Long totalBookings;
    private Long confirmedBookings;
    private Long cancelledBookings;
    private Double cancellationRate;

    // Peak analysis
    private List<ChartDataPoint> peakBookingHours;
    private List<ChartDataPoint> peakTravelDays;
    private List<ChartDataPoint> bookingGrowth;

    // Status distribution
    private List<ChartDataPoint> bookingStatusDistribution;
}
