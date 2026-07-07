package com.travelease.backend.trip.controller;

import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;

import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.travelease.backend.trip.request.CreateTripRequest;
import com.travelease.backend.trip.request.UpdateTripRequest;
import com.travelease.backend.trip.response.TripDashboardResponse;
import com.travelease.backend.trip.response.TripResponse;
import com.travelease.backend.trip.service.TripService;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;
    
    @PostMapping
    public TripResponse createTrip(@Valid @RequestBody CreateTripRequest request) {
        return tripService.createTrip(request);
    }

    @PutMapping("/{tripId}")
    public TripResponse updateTrip(@PathVariable UUID tripId,
    		@Valid @RequestBody UpdateTripRequest request) {
        return tripService.updateTrip(tripId, request);
    }

    @DeleteMapping("/{tripId}")
    public TripResponse cancelTrip(@PathVariable UUID tripId) {
        return tripService.cancelTrip(tripId);
    }

    @GetMapping("/{tripId}/dashboard")
    public TripDashboardResponse getTripDashboard(@PathVariable UUID tripId) {
        return tripService.getTripDashboard(tripId);
    }

}