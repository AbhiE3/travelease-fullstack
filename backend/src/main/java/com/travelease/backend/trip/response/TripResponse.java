package com.travelease.backend.trip.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.travelease.backend.trip.entity.TripStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripResponse {

    private UUID tripId;

    private String tripName;

    private String organizerName;

    private String sourceLocation;

    private Integer destinationId;

    private Integer categoryId;

    private BigDecimal budgetAmount;

    private LocalDate startDate;

    private LocalDate endDate;

    private TripStatus status;

}