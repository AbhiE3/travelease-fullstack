package com.travelease.backend.trip.service;

import com.travelease.backend.trip.dto.AddTripMemberRequest;
import com.travelease.backend.trip.dto.CreateTripRequest;
import com.travelease.backend.trip.dto.PendingInvitationResponse;
import com.travelease.backend.trip.dto.TripMemberResponse;
import com.travelease.backend.trip.dto.TripResponse;
import com.travelease.backend.trip.dto.TripStatusTransitionRequest;
import com.travelease.backend.trip.dto.UpdateTripRequest;

import java.util.List;
import java.util.UUID;

public interface TripService {

    TripResponse createTrip(CreateTripRequest request, String currentUserEmail);

    List<TripResponse> getMyTrips(String currentUserEmail);

    TripResponse getTripById(UUID tripId, String currentUserEmail);

    TripResponse updateTrip(UUID tripId, UpdateTripRequest request, String currentUserEmail);

    TripResponse transitionStatus(UUID tripId, TripStatusTransitionRequest request, String currentUserEmail);

    List<TripMemberResponse> getTripMembers(UUID tripId, String currentUserEmail);

    TripMemberResponse addTripMember(UUID tripId, AddTripMemberRequest request, String currentUserEmail);

    void removeTripMember(UUID tripId, UUID tripMemberId, String currentUserEmail);

    TripMemberResponse acceptTripMemberInvitation(UUID tripId, UUID tripMemberId, String currentUserEmail);

    TripMemberResponse rejectTripMemberInvitation(UUID tripId, UUID tripMemberId, String currentUserEmail);

    List<PendingInvitationResponse> getMyPendingInvitations(String currentUserEmail);
}
