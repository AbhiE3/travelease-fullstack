package com.travelease.backend.busbooking.mapper;

import com.travelease.backend.busbooking.dto.response.BusSearchResponse;
import com.travelease.backend.busbooking.dto.response.ScheduleResponse;
import com.travelease.backend.busbooking.dto.response.SmartSearchResponse;
import com.travelease.backend.busbooking.entity.BusSchedule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class ScheduleMapper {

    private final BusMapper busMapper;
    private final RouteMapper routeMapper;

    public ScheduleMapper(BusMapper busMapper, RouteMapper routeMapper) {
        this.busMapper = busMapper;
        this.routeMapper = routeMapper;
    }

    public ScheduleResponse toResponse(BusSchedule schedule) {
        return ScheduleResponse.builder()
                .id(schedule.getId())
                .bus(busMapper.toResponse(schedule.getBus()))
                .route(routeMapper.toResponse(schedule.getRoute()))
                .travelDate(schedule.getTravelDate())
                .departureTime(schedule.getDepartureTime())
                .arrivalTime(schedule.getArrivalTime())
                .fare(schedule.getFare())
                .availableSeats(schedule.getAvailableSeats())
                .status(schedule.getStatus())
                .build();
    }

    public BusSearchResponse toSearchResponse(BusSchedule schedule) {
        return BusSearchResponse.builder()
                .scheduleId(schedule.getId())
                .busName(schedule.getBus().getBusName())
                .busNumber(schedule.getBus().getBusNumber())
                .busType(schedule.getBus().getBusType())
                .source(schedule.getRoute().getSource())
                .destination(schedule.getRoute().getDestination())
                .departureTime(schedule.getDepartureTime())
                .arrivalTime(schedule.getArrivalTime())
                .fare(schedule.getFare())
                .availableSeats(schedule.getAvailableSeats())
                .duration(schedule.getRoute().getDurationHours())
                .travelDate(schedule.getTravelDate())
                .amenities(schedule.getBus().getAmenities() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(schedule.getBus().getAmenities()))
                .build();
    }

    public SmartSearchResponse toSmartSearchResponse(BusSchedule schedule) {
        return SmartSearchResponse.builder()
                .scheduleId(schedule.getId())
                .routeId(schedule.getRoute().getId())
                .providerId(schedule.getBus().getProviderId())
                .busId(schedule.getBus().getId())
                .busName(schedule.getBus().getBusName())
                .busNumber(schedule.getBus().getBusNumber())
                .busType(schedule.getBus().getBusType())
                .source(schedule.getRoute().getSource())
                .destination(schedule.getRoute().getDestination())
                .departureTime(schedule.getDepartureTime())
                .arrivalTime(schedule.getArrivalTime())
                .duration(schedule.getRoute().getDurationHours())
                .travelDate(schedule.getTravelDate())
                .fare(schedule.getFare())
                .availableSeats(schedule.getAvailableSeats())
                .amenities(schedule.getBus().getAmenities() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(schedule.getBus().getAmenities()))
                .boardingPoint(null)  // Reserved for future Itinerary/Activity modules
                .dropPoint(null)      // Reserved for future Itinerary/Activity modules
                .build();
    }
}
