package com.travelease.backend.trips_and_invitations.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.travelease.backend.trips_and_invitations.enums.InvitationStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvitationResponse {

    private UUID invitationId;

    private UUID tripId;

    private String inviteeEmail;

    private InvitationStatus status;

    private LocalDateTime sentDate;

    private LocalDateTime responseDate;

}