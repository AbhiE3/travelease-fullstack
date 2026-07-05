package com.travelease.backend.busbooking.dto.request;

import com.travelease.backend.busbooking.entity.enums.BusType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FareRequest {

    @NotNull(message = "Route ID is required")
    private Long routeId;

    private BusType busType; // null = all bus types on this route

    @NotNull(message = "Base fare is required")
    @PositiveOrZero(message = "Base fare cannot be negative")
    private Double baseFare;

    // Dynamic pricing
    private Boolean dynamicFareEnabled;
    private Integer occupancyThreshold1;
    private Double fareMultiplier1;
    private Integer occupancyThreshold2;
    private Double fareMultiplier2;
    private Integer occupancyThreshold3;
    private Double fareMultiplier3;

    // Surcharges
    @PositiveOrZero(message = "Weekend surcharge cannot be negative")
    private Double weekendSurchargePercent;
    @PositiveOrZero(message = "Festival surcharge cannot be negative")
    private Double festivalSurchargePercent;
    private LocalDate festivalStartDate;
    private LocalDate festivalEndDate;
    @PositiveOrZero(message = "Seasonal surcharge cannot be negative")
    private Double seasonalSurchargePercent;
    private LocalDate seasonalStartDate;
    private LocalDate seasonalEndDate;

    // Seat-type surcharges
    private Double sleeperSurchargePercent;
    private Double semiSleeperSurchargePercent;
    private Double luxurySurchargePercent;

    // Tax / GST
    @PositiveOrZero(message = "GST percent cannot be negative")
    private Double gstPercent;
    @PositiveOrZero(message = "Tax percent cannot be negative")
    private Double taxPercent;

    // Cancellation / refund
    @PositiveOrZero(message = "Cancellation charge percent cannot be negative")
    private Double cancellationChargePercent;
    @PositiveOrZero(message = "Refund percent cannot be negative")
    private Double refundPercent;

    private Boolean active;
}
