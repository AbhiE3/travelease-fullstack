package com.travelease.backend.busbooking.dto.response;

import com.travelease.backend.busbooking.entity.enums.TripStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Integration-ready trip response.
 * Exposes all information needed by future modules (Itinerary, Analytics, etc.).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripResponse {

    private Long id;
    private Long scheduleId;
    private Long routeId;
    private Long providerId;
    private Long busId;
    private String busNumber;
    private String busName;

    // Driver
    private Long driverId;
    private String driverName;
    private String driverLicense;

    // Conductor
    private Long conductorId;
    private String conductorName;

    // Trip details
    private TripStatus status;
    private LocalDateTime actualDepartureTime;
    private LocalDateTime actualArrivalTime;
    private Integer delayMinutes;
    private Double distanceCoveredKm;
    private String notes;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
