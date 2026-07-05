package com.travelease.backend.busbooking.mapper;

import com.travelease.backend.busbooking.dto.response.RouteResponse;
import com.travelease.backend.busbooking.entity.Route;
import org.springframework.stereotype.Component;

@Component
public class RouteMapper {

    public RouteResponse toResponse(Route route) {
        return RouteResponse.builder()
                .id(route.getId())
                .source(route.getSource())
                .destination(route.getDestination())
                .distanceKm(route.getDistanceKm())
                .durationHours(route.getDurationHours())
                .status(route.getStatus())
                .createdAt(route.getCreatedAt())
                .build();
    }
}
