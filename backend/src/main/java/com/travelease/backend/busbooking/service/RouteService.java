package com.travelease.backend.busbooking.service;

import com.travelease.backend.busbooking.dto.request.RouteRequest;
import com.travelease.backend.busbooking.dto.response.RouteResponse;
import com.travelease.backend.busbooking.entity.enums.RouteStatus;

import java.util.List;

public interface RouteService {

    RouteResponse createRoute(RouteRequest request);

    RouteResponse updateRoute(Long id, RouteRequest request);

    void deleteRoute(Long id);

    RouteResponse getRouteById(Long id);

    List<RouteResponse> getRoutes(RouteStatus status);
}
