package com.travelease.backend.trip.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.travelease.backend.trip.entity.TripStatus;
import com.travelease.backend.trips_and_invitations.response.TripMemberResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripDashboardResponse {

    private UUID tripId;

    private String tripName;

    private String organizerName;

    private Integer destinationId;

    private Integer categoryId;

    private BigDecimal budgetAmount;

    private LocalDate startDate;

    private LocalDate endDate;

    private TripStatus status;

    private Integer totalMembers;

    private List<TripMemberResponse> members;

}