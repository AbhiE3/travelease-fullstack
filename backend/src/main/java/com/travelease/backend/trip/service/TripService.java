package com.travelease.backend.trip.service;

import java.util.UUID;

import com.travelease.backend.trip.request.CreateTripRequest;
import com.travelease.backend.trip.request.UpdateTripRequest;
import com.travelease.backend.trip.response.TripDashboardResponse;
import com.travelease.backend.trip.response.TripResponse;

public interface TripService {

    // US-TRIP-01 Create Trip
    TripResponse createTrip(CreateTripRequest request);

    // US-TRIP-02 & US-TRIP-03 Modify Trip / Change Category
    TripResponse updateTrip(UUID tripId, UpdateTripRequest request);

    // US-TRIP-04 Cancel Trip
    TripResponse cancelTrip(UUID tripId);

    // US-TRIP-05 View Dashboard
    TripDashboardResponse getTripDashboard(UUID tripId);

}