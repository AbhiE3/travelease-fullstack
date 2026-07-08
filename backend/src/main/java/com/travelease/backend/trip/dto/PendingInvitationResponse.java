package com.travelease.backend.trip.dto;

import com.travelease.backend.trip.entity.TripMemberStatus;

import java.time.LocalDate;
import java.util.UUID;

public record PendingInvitationResponse(
        UUID tripMemberId,
        UUID tripId,
        String tripName,
        TripOrganizerSummary organizer,
        String sourceLocation,
        LocalDate startDate,
        LocalDate endDate,
        TripMemberStatus memberStatus
) {
}
