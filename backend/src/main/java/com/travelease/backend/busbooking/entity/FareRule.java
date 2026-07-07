package com.travelease.backend.busbooking.entity;

import com.travelease.backend.busbooking.entity.enums.BusType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fare_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FareRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_id", nullable = false)
    private Long routeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "bus_type")
    private BusType busType; // null = applies to all bus types on this route

    // â”€â”€ Base fare â”€â”€
    @Column(name = "base_fare", nullable = false)
    private Double baseFare;

    // â”€â”€ Dynamic pricing tiers â”€â”€
    @Column(name = "dynamic_fare_enabled")
    @Builder.Default
    private Boolean dynamicFareEnabled = false;

    @Column(name = "occupancy_threshold_1")
    @Builder.Default
    private Integer occupancyThreshold1 = 50;

    @Column(name = "fare_multiplier_1")
    @Builder.Default
    private Double fareMultiplier1 = 1.0;

    @Column(name = "occupancy_threshold_2")
    @Builder.Default
    private Integer occupancyThreshold2 = 75;

    @Column(name = "fare_multiplier_2")
    @Builder.Default
    private Double fareMultiplier2 = 1.2;

    @Column(name = "occupancy_threshold_3")
    @Builder.Default
    private Integer occupancyThreshold3 = 90;

    @Column(name = "fare_multiplier_3")
    @Builder.Default
    private Double fareMultiplier3 = 1.5;

    // â”€â”€ Surcharges â”€â”€
    @Column(name = "weekend_surcharge_percent")
    @Builder.Default
    private Double weekendSurchargePercent = 0.0;

    @Column(name = "festival_surcharge_percent")
    @Builder.Default
    private Double festivalSurchargePercent = 0.0;

    @Column(name = "festival_start_date")
    private LocalDate festivalStartDate;

    @Column(name = "festival_end_date")
    private LocalDate festivalEndDate;

    @Column(name = "seasonal_surcharge_percent")
    @Builder.Default
    private Double seasonalSurchargePercent = 0.0;

    @Column(name = "seasonal_start_date")
    private LocalDate seasonalStartDate;

    @Column(name = "seasonal_end_date")
    private LocalDate seasonalEndDate;

    // â”€â”€ Seat-type surcharges â”€â”€
    @Column(name = "sleeper_surcharge_percent")
    @Builder.Default
    private Double sleeperSurchargePercent = 0.0;

    @Column(name = "semi_sleeper_surcharge_percent")
    @Builder.Default
    private Double semiSleeperSurchargePercent = 0.0;

    @Column(name = "luxury_surcharge_percent")
    @Builder.Default
    private Double luxurySurchargePercent = 0.0;

    // â”€â”€ Tax / GST â”€â”€
    @Column(name = "gst_percent")
    @Builder.Default
    private Double gstPercent = 5.0;

    @Column(name = "tax_percent")
    @Builder.Default
    private Double taxPercent = 0.0;

    // â”€â”€ Cancellation / refund rules â”€â”€
    @Column(name = "cancellation_charge_percent")
    @Builder.Default
    private Double cancellationChargePercent = 10.0;

    @Column(name = "refund_percent")
    @Builder.Default
    private Double refundPercent = 90.0;

    // â”€â”€ Status â”€â”€
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
