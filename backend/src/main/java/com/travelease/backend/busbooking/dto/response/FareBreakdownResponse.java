package com.travelease.backend.busbooking.dto.response;

import com.travelease.backend.busbooking.entity.enums.BusType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Complete fare breakdown for a set of seats on a schedule.
 * Integration-ready for Booking, Trip Planning, and Analytics modules.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FareBreakdownResponse {

    // Schedule context
    private Long scheduleId;
    private Long routeId;
    private Long busId;
    private String busNumber;
    private BusType busType;
    private String source;
    private String destination;
    private LocalDate travelDate;
    private Integer numberOfSeats;
    private Double occupancyPercentage;

    // Fare components (total across all seats)
    private Double baseFare;
    private Double dynamicFareAdjustment;
    private Double weekendSurcharge;
    private Double festivalSurcharge;
    private Double seasonalSurcharge;
    private Double seatTypeSurcharge;
    private Double busTypeSurcharge;
    private Double subtotal;

    // Discounts
    private Double discountAmount;
    private String appliedDiscount;
    private Double couponDiscount;
    private String appliedCoupon;

    // Taxes
    private Double gstAmount;
    private Double gstPercent;
    private Double taxAmount;
    private Double taxPercent;

    // Final
    private Double finalAmount;

    // Cancellation / refund rules
    private Double cancellationChargePercent;
    private Double refundPercent;
    private Double cancellationCharge;
    private Double refundAmount;

    // Per-seat breakdown
    private List<SeatFareBreakdown> seatBreakdowns;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SeatFareBreakdown {
        private Long seatId;
        private String seatNumber;
        private String seatType;
        private Double baseFare;
        private Double seatTypeSurcharge;
        private Double dynamicAdjustment;
        private Double subtotal;
        private Double discount;
        private Double finalFare;
    }
}
