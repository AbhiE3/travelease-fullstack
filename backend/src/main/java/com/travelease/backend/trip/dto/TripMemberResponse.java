package com.travelease.backend.trip.dto;

import com.travelease.backend.trip.entity.TripMemberStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TripMemberResponse(
        UUID tripMemberId,
        UUID userId,
        String name,
        String email,
        TripMemberStatus memberStatus,
        LocalDateTime joinedDate,
        BigDecimal budgetAmount,
        BigDecimal spentAmount
) {
}
