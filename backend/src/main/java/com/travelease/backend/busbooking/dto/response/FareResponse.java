package com.travelease.backend.busbooking.dto.response;

import com.travelease.backend.busbooking.entity.enums.BusType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FareResponse {

    private Long id;
    private Long routeId;
    private String source;
    private String destination;
    private BusType busType;
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
    private Double weekendSurchargePercent;
    private Double festivalSurchargePercent;
    private LocalDate festivalStartDate;
    private LocalDate festivalEndDate;
    private Double seasonalSurchargePercent;
    private LocalDate seasonalStartDate;
    private LocalDate seasonalEndDate;

    // Seat-type surcharges
    private Double sleeperSurchargePercent;
    private Double semiSleeperSurchargePercent;
    private Double luxurySurchargePercent;

    // Tax / GST
    private Double gstPercent;
    private Double taxPercent;

    // Cancellation / refund
    private Double cancellationChargePercent;
    private Double refundPercent;

    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
