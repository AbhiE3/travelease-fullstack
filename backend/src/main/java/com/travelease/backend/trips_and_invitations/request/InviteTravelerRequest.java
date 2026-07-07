package com.travelease.backend.trips_and_invitations.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteTravelerRequest {

    @NotNull
    private UUID invitedBy;

    @NotEmpty
    private List<@Email String> inviteeEmails;

}