package com.travelease.backend.trips_and_invitations.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.travelease.backend.trips_and_invitations.request.InviteTravelerRequest;
import com.travelease.backend.trips_and_invitations.response.InvitationResponse;
import com.travelease.backend.trips_and_invitations.response.TripMemberResponse;
import com.travelease.backend.trips_and_invitations.service.InvitationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping("/{tripId}")
    public List<InvitationResponse> inviteTraveler(
            @PathVariable UUID tripId,
            @Valid @RequestBody InviteTravelerRequest request) {

        return invitationService.inviteTraveler(tripId, request);
    }

    @PutMapping("/{invitationId}/accept")
    public InvitationResponse acceptInvitation(
            @PathVariable UUID invitationId) {

        return invitationService.acceptInvitation(invitationId);
    }

    @PutMapping("/{invitationId}/reject")
    public InvitationResponse rejectInvitation(
            @PathVariable UUID invitationId) {

        return invitationService.rejectInvitation(invitationId);
    }

    @GetMapping("/trip/{tripId}/members")
    public List<TripMemberResponse> getTripMembers(
            @PathVariable UUID tripId) {

        return invitationService.getTripMembers(tripId);
    }
}