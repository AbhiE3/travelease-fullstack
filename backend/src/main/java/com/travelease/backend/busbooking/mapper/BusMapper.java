package com.travelease.backend.busbooking.mapper;

import com.travelease.backend.busbooking.dto.response.BusResponse;
import com.travelease.backend.busbooking.entity.Bus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class BusMapper {

    public BusResponse toResponse(Bus bus) {
        return BusResponse.builder()
                .id(bus.getId())
                .busNumber(bus.getBusNumber())
                .busName(bus.getBusName())
                .totalSeats(bus.getTotalSeats())
                .providerId(bus.getProviderId())
                .busType(bus.getBusType())
                .amenities(bus.getAmenities() == null ? new ArrayList<>() : new ArrayList<>(bus.getAmenities()))
                .status(bus.getStatus())
                .createdAt(bus.getCreatedAt())
                .build();
    }
}
