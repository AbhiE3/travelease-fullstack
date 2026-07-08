package com.travelease.backend.trips_and_invitations.service.impl;

import com.travelease.backend.trip.repository.TripRepository;
import com.travelease.backend.trip.repository.TripMemberRepository;
import com.travelease.backend.auth.repository.UserRepository;
import com.travelease.backend.trip.entity.TripMemberStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.travelease.backend.trips_and_invitations.entity.Invitation;
import com.travelease.backend.trip.entity.Trip;
import com.travelease.backend.trip.entity.TripMember;
import com.travelease.backend.auth.entity.User;
import com.travelease.backend.trips_and_invitations.enums.InvitationStatus;
import com.travelease.backend.shared.exception.ResourceNotFoundException;
import com.travelease.backend.trips_and_invitations.repository.InvitationRepository;
import com.travelease.backend.trips_and_invitations.request.InviteTravelerRequest;
import com.travelease.backend.trips_and_invitations.response.InvitationResponse;
import com.travelease.backend.trips_and_invitations.response.TripMemberResponse;
import com.travelease.backend.trips_and_invitations.service.InvitationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvitationServiceImpl implements InvitationService{
	
	private final InvitationRepository invitationRepository;
	private final TripRepository tripRepository;
	private final UserRepository userRepository;//
	private final TripMemberRepository tripMemberRepository;

	@Override
	public List<InvitationResponse> inviteTraveler(UUID tripId, InviteTravelerRequest request) {
	    Trip trip = tripRepository.findById(tripId)
	            .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
	    User organizer = userRepository.findById(request.getInvitedBy())
	            .orElseThrow(() -> new ResourceNotFoundException("Organizer not found"));
	    List<InvitationResponse> responses =new ArrayList<>();
	    for(String email:request.getInviteeEmails()) {
			 if(invitationRepository.existsByTripIdAndInviteeEmail(tripId,email)){
				 throw new RuntimeException("Traveler is already invited to this trip");
			 }
	    	Invitation invitation = Invitation.builder()
	    	        .trip(trip)
	    	        .invitedBy(organizer)
	    	        .inviteeEmail(email)
	    	        .status(InvitationStatus.PENDING)
	    	        .sentDate(LocalDateTime.now())
	    	        .build();
	    	
	    	Invitation savedInvitation =invitationRepository.save(invitation);
	    	
	    	InvitationResponse response = InvitationResponse.builder()
	    	        .invitationId(savedInvitation.getInvitationId())
	    	        .tripId(savedInvitation.getTrip().getId())
	    	        .inviteeEmail(savedInvitation.getInviteeEmail())
	    	        .status(savedInvitation.getStatus())
	    	        .sentDate(savedInvitation.getSentDate())
	    	        .responseDate(savedInvitation.getResponseDate())
	    	        .build();

	    	responses.add(response);
	    }
	    return responses;
	}

	@Override
	public InvitationResponse acceptInvitation(UUID invitationId) {

	    Invitation invitation = invitationRepository.findById(invitationId)
	            .orElseThrow(() ->
	                    new ResourceNotFoundException("Invitation not found"));

	    invitation.setStatus(InvitationStatus.ACCEPTED);
	    invitation.setResponseDate(LocalDateTime.now());

	    Invitation savedInvitation = invitationRepository.save(invitation);

	    User user = userRepository.findByEmail(savedInvitation.getInviteeEmail())
	            .orElseThrow(() ->
	                    new ResourceNotFoundException("User not found"));

	    TripMember tripMember = new TripMember();
	    tripMember.setTrip(savedInvitation.getTrip());
	    tripMember.setUser(user);
	    tripMember.setMemberStatus(TripMemberStatus.ACCEPTED);
	    tripMember.setJoinedDate(LocalDateTime.now());

	    tripMemberRepository.save(tripMember);

	    InvitationResponse response = InvitationResponse.builder()
	            .invitationId(savedInvitation.getInvitationId())
	            .tripId(savedInvitation.getTrip().getId())
	            .inviteeEmail(savedInvitation.getInviteeEmail())
	            .status(savedInvitation.getStatus())
	            .sentDate(savedInvitation.getSentDate())
	            .responseDate(savedInvitation.getResponseDate())
	            .build();

	    return response;
	}

	@Override
	public InvitationResponse rejectInvitation(UUID invitationId) {

	    Invitation invitation = invitationRepository.findById(invitationId)
	            .orElseThrow(() ->
	                    new ResourceNotFoundException("Invitation not found"));

	    invitation.setStatus(InvitationStatus.REJECTED);
	    invitation.setResponseDate(LocalDateTime.now());

	    Invitation savedInvitation = invitationRepository.save(invitation);

	    InvitationResponse response = InvitationResponse.builder()
	            .invitationId(savedInvitation.getInvitationId())
	            .tripId(savedInvitation.getTrip().getId())
	            .inviteeEmail(savedInvitation.getInviteeEmail())
	            .status(savedInvitation.getStatus())
	            .sentDate(savedInvitation.getSentDate())
	            .responseDate(savedInvitation.getResponseDate())
	            .build();

	    return response;
	}

	@Override
	public List<TripMemberResponse> getTripMembers(UUID tripId) {

	    List<TripMember> members =
	            tripMemberRepository.findByTripId(tripId);

	    List<TripMemberResponse> response = members.stream()
	            .map(member -> {

	                TripMemberResponse memberResponse = new TripMemberResponse();

	                memberResponse.setUserId(member.getUser().getId());

	                memberResponse.setFullName(member.getUser().getName());

	                memberResponse.setEmail(member.getUser().getEmail());

	                memberResponse.setMemberStatus(member.getMemberStatus());

	                memberResponse.setJoinedDate(member.getJoinedDate());

	                return memberResponse;

	            })
	            .toList();

	    return response;
	}

}
