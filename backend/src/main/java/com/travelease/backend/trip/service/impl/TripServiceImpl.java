package com.travelease.backend.trip.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import java.util.UUID;
import org.springframework.stereotype.Service;
import com.travelease.backend.trips_and_invitations.repository.InvitationRepository;
import com.travelease.backend.trip.repository.TripMemberRepository;
import com.travelease.backend.trip.repository.TripRepository;
import com.travelease.backend.auth.repository.UserRepository;
import com.travelease.backend.trip.request.CreateTripRequest;
import com.travelease.backend.trip.request.UpdateTripRequest;
import com.travelease.backend.trip.response.TripDashboardResponse;
import com.travelease.backend.trip.response.TripResponse;
import com.travelease.backend.trip.service.TripService;
import lombok.RequiredArgsConstructor;

import com.travelease.backend.trip.entity.Trip;
import com.travelease.backend.auth.entity.User;
import com.travelease.backend.trip.entity.TripStatus;
import com.travelease.backend.shared.exception.ResourceNotFoundException;
import com.travelease.backend.trips_and_invitations.response.TripMemberResponse;

@Service("coreTripService")
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    private final TripRepository tripRepository;

    private final UserRepository userRepository;

    private final TripMemberRepository tripMemberRepository;

    private final InvitationRepository invitationRepository;

    @Override
    public TripResponse createTrip(CreateTripRequest request) {
    	
        User organizer = userRepository.findById(request.getOrganizerId())
                .orElseThrow(() -> new ResourceNotFoundException("Organizer not found"));

        Trip trip = new Trip();
        trip.setTripName(request.getTripName());
        trip.setOrganizer(organizer);
        trip.setSourceLocation(request.getSourceLocation());
        trip.setDestinationId(request.getDestinationId());
        trip.setCategoryId(request.getCategoryId());
        trip.setBudgetAmount(request.getBudgetAmount());
        trip.setStartDate(request.getStartDate());
        trip.setEndDate(request.getEndDate());
        trip.setStatus(TripStatus.PLANNED);

        Trip savedTrip = tripRepository.save(trip);

        TripResponse response = new TripResponse();

        response.setTripId(savedTrip.getId());
        response.setTripName(savedTrip.getTripName());
        response.setOrganizerName(
                savedTrip.getOrganizer().getName());
        response.setSourceLocation(savedTrip.getSourceLocation());
        response.setDestinationId(savedTrip.getDestinationId());
        response.setCategoryId(savedTrip.getCategoryId());
        response.setBudgetAmount(savedTrip.getBudgetAmount());
        response.setStartDate(savedTrip.getStartDate());
        response.setEndDate(savedTrip.getEndDate());
        response.setStatus(savedTrip.getStatus());
        return response;
    }

    @Override
    public TripResponse updateTrip(UUID tripId, UpdateTripRequest request) {
    	Trip trip = tripRepository.findById(tripId)
    	        .orElseThrow(() ->
    	                new ResourceNotFoundException("Trip not found"));
    	
    	trip.setTripName(request.getTripName());
    	trip.setSourceLocation(request.getSourceLocation());
    	trip.setBudgetAmount(request.getBudgetAmount());
    	trip.setStartDate(request.getStartDate());
    	trip.setEndDate(request.getEndDate());
    	
    	trip.setDestinationId(request.getDestinationId());
    	trip.setCategoryId(request.getCategoryId());
    	
    	Trip updatedTrip=tripRepository.save(trip);
    	
    	TripResponse response = new TripResponse();
    	
    	response.setTripId(updatedTrip.getId());
    	response.setTripName(updatedTrip.getTripName());

    	response.setOrganizerName(
    	        updatedTrip.getOrganizer().getName());
    	response.setSourceLocation(updatedTrip.getSourceLocation());
    	response.setDestinationId(updatedTrip.getDestinationId());
    	response.setCategoryId(updatedTrip.getCategoryId());
    	response.setBudgetAmount(updatedTrip.getBudgetAmount());
    	response.setStartDate(updatedTrip.getStartDate());
    	response.setEndDate(updatedTrip.getEndDate());
    	response.setStatus(updatedTrip.getStatus());
    	return response;
    }

    @Override
    public TripResponse cancelTrip(UUID tripId) {
    	Trip trip = tripRepository.findById(tripId)
    	        .orElseThrow(() ->
    	                new ResourceNotFoundException("Trip not found"));

    	trip.setStatus(TripStatus.CANCELLED);

    	Trip cancelledTrip = tripRepository.save(trip);

    	TripResponse response = new TripResponse();
    	
    	response.setTripId(trip.getId());
    	response.setTripName(trip.getTripName());

    	response.setOrganizerName(
    	        trip.getOrganizer().getName());
    	
    	response.setSourceLocation(trip.getSourceLocation());

    	response.setDestinationId(trip.getDestinationId());

    	response.setCategoryId(trip.getCategoryId());

    	response.setBudgetAmount(trip.getBudgetAmount());

    	response.setStartDate(trip.getStartDate());

    	response.setEndDate(trip.getEndDate());

    	response.setStatus(trip.getStatus());
    	return response;
    }

    @Override
    public TripDashboardResponse getTripDashboard(UUID tripId) {
    	Trip trip = tripRepository.findById(tripId)
    	        .orElseThrow(() ->
    	                new ResourceNotFoundException("Trip not found"));
    	
    	TripDashboardResponse response = new TripDashboardResponse();
    	
    	response.setTripId(trip.getId());

    	response.setTripName(trip.getTripName());

    	response.setOrganizerName(
    	        trip.getOrganizer().getName());

    	response.setDestinationId(trip.getDestinationId());

    	response.setCategoryId(trip.getCategoryId());

    	response.setBudgetAmount(trip.getBudgetAmount());

    	response.setStartDate(trip.getStartDate());

    	response.setEndDate(trip.getEndDate());

    	response.setStatus(trip.getStatus());
    	
    	response.setTotalMembers(tripMemberRepository.findByTripId(tripId).size());
    	
    	List<TripMemberResponse> members =
    	        tripMemberRepository.findByTripId(tripId)
    	                .stream()
    	                .map(member -> {

    	                    TripMemberResponse memberResponse =
    	                            new TripMemberResponse();

    	                    memberResponse.setUserId(
    	                            member.getUser().getId());

    	                    memberResponse.setFullName(
    	                            member.getUser().getName());

    	                    memberResponse.setEmail(
    	                            member.getUser().getEmail());

    	                    memberResponse.setMemberStatus(
    	                            member.getMemberStatus());

    	                    memberResponse.setJoinedDate(
    	                            member.getJoinedDate());

    	                    return memberResponse;

    	                })
    	                .collect(Collectors.toList());
    	response.setMembers(members);

    	return response;
    }

}