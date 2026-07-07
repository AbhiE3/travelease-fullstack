package com.travelease.backend.trips_and_invitations.service;

import java.util.List;
import java.util.UUID;

import com.travelease.backend.trips_and_invitations.request.InviteTravelerRequest;
import com.travelease.backend.trips_and_invitations.response.InvitationResponse;
import com.travelease.backend.trips_and_invitations.response.TripMemberResponse;

public interface InvitationService {

    // US-INV-01 Invite Travelers
    List<InvitationResponse> inviteTraveler(UUID tripId,InviteTravelerRequest request);

    // US-INV-02 Accept Invitation
    InvitationResponse acceptInvitation(UUID invitationId);

    // US-INV-03 Reject Invitation
    InvitationResponse rejectInvitation(UUID invitationId);

    // US-INV-04 View Trip Members
    List<TripMemberResponse> getTripMembers(UUID tripId);

}