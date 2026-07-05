package com.travelease.backend.busbooking.service;

import com.travelease.backend.busbooking.dto.request.TripAssignmentRequest;
import com.travelease.backend.busbooking.dto.request.TripStatusTransitionRequest;
import com.travelease.backend.busbooking.dto.response.FleetAvailabilityResponse;
import com.travelease.backend.busbooking.dto.response.TripResponse;
import com.travelease.backend.busbooking.entity.enums.TripStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TripService {

    TripResponse assignTrip(TripAssignmentRequest request);

    TripResponse getTripById(Long id);

    List<TripResponse> getTrips(Long scheduleId, Long driverId, Long conductorId, TripStatus status, Pageable pageable);

    TripResponse transitionTrip(Long id, TripStatusTransitionRequest request);

    FleetAvailabilityResponse getFleetAvailability(Long providerId);
}
