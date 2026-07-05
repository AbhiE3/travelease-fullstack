package com.travelease.backend.trip.entity;

import com.travelease.backend.auth.entity.User;
import com.travelease.backend.shared.entity.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@AttributeOverride(name = "id", column = @Column(name = "trip_id", nullable = false, updatable = false))
@Table(name = "trips")
public class Trip extends BaseEntity {

    @Column(name = "trip_name", nullable = false, length = 200)
    private String tripName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @Column(name = "source_location", nullable = false, length = 200)
    private String sourceLocation;

    @Column(name = "destination_id", nullable = false)
    private Integer destinationId;

    @Column(name = "budget_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal budgetAmount;

    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, length = 30)
    private String status;
}
