package com.travelease.backend.busbooking.entity;

import com.travelease.backend.busbooking.entity.enums.BusType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "cancellation_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancellationPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_id")
    private Long routeId; // null = applies to all routes

    @Enumerated(EnumType.STRING)
    @Column(name = "bus_type")
    private BusType busType; // null = applies to all bus types

    @Column(name = "time_window_hours", nullable = false)
    @Builder.Default
    private Integer timeWindowHours = 24; // hours before departure

    @Column(name = "cancellation_charge_percent", nullable = false)
    @Builder.Default
    private Double cancellationChargePercent = 10.0;

    @Column(name = "refund_percent", nullable = false)
    @Builder.Default
    private Double refundPercent = 90.0;

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
