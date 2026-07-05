package com.travelease.backend.busbooking.dto.response;

import com.travelease.backend.busbooking.entity.enums.BusType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Integration-ready smart search response for TravelEase platform.
 * Exposes sufficient information for future journey planning modules including
 * Trip Planning, Hotel Booking, Activity Booking, Itinerary Management,
 * Recommendation Engine, and Analytics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmartSearchResponse {

    // Schedule information
    private Long scheduleId;
    private LocalDate travelDate;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private Double duration;
    private Double fare;
    private Integer availableSeats;

    // Route information (for Trip Planning / Itinerary)
    private Long routeId;
    private String source;
    private String destination;

    // Provider information (for multi-provider integration)
    private Long providerId;

    // Bus information
    private Long busId;
    private String busName;
    private String busNumber;
    private BusType busType;
    private List<String> amenities;

    // Boarding/Drop points (for future Itinerary/Activity modules)
    private String boardingPoint;
    private String dropPoint;
}
