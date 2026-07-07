package com.travelease.backend.busbooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Fleet availability summary for a provider.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FleetAvailabilityResponse {

    private Long providerId;
    private Long totalBuses;
    private Long activeBuses;
    private Long maintenanceBuses;
    private Long inactiveBuses;
    private Long availableDrivers;
    private Long availableConductors;
    private Long activeTrips;
    private Long scheduledTrips;
}
