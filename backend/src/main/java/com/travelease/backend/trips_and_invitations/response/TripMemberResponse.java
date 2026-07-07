package com.travelease.backend.trips_and_invitations.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.travelease.backend.trip.entity.TripMemberStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripMemberResponse {

    private UUID userId;

    private String fullName;

    private String email;

    private TripMemberStatus memberStatus;

    private LocalDateTime joinedDate;

}