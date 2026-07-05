package com.travelease.backend.busbooking.mapper;

import com.travelease.backend.busbooking.dto.response.TripResponse;
import com.travelease.backend.busbooking.entity.Trip;
import org.springframework.stereotype.Component;

@Component
public class TripMapper {

    public TripResponse toResponse(Trip trip) {
        TripResponse.TripResponseBuilder builder = TripResponse.builder()
                .id(trip.getId())
                .scheduleId(trip.getSchedule().getId())
                .routeId(trip.getSchedule().getRoute().getId())
                .providerId(trip.getSchedule().getBus().getProviderId())
                .busId(trip.getSchedule().getBus().getId())
                .busNumber(trip.getSchedule().getBus().getBusNumber())
                .busName(trip.getSchedule().getBus().getBusName())
                .status(trip.getStatus())
                .actualDepartureTime(trip.getActualDepartureTime())
                .actualArrivalTime(trip.getActualArrivalTime())
                .delayMinutes(trip.getDelayMinutes())
                .distanceCoveredKm(trip.getDistanceCoveredKm())
                .notes(trip.getNotes())
                .createdAt(trip.getCreatedAt())
                .updatedAt(trip.getUpdatedAt());

        if (trip.getDriver() != null) {
            builder.driverId(trip.getDriver().getId())
                    .driverName(trip.getDriver().getName())
                    .driverLicense(trip.getDriver().getLicenseNumber());
        }

        if (trip.getConductor() != null) {
            builder.conductorId(trip.getConductor().getId())
                    .conductorName(trip.getConductor().getName());
        }

        return builder.build();
    }
}
