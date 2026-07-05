package com.travelease.backend.busbooking.service.impl;

import com.travelease.backend.busbooking.dto.request.RouteRequest;
import com.travelease.backend.busbooking.dto.response.RouteResponse;
import com.travelease.backend.busbooking.entity.Route;
import com.travelease.backend.busbooking.entity.enums.RouteStatus;
import com.travelease.backend.busbooking.exception.ResourceNotFoundException;
import com.travelease.backend.busbooking.mapper.RouteMapper;
import com.travelease.backend.busbooking.repository.RouteRepository;
import com.travelease.backend.busbooking.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

    private final RouteRepository routeRepository;
    private final RouteMapper routeMapper;

    @Override
    @Transactional
    public RouteResponse createRoute(RouteRequest request) {
        Route route = Route.builder()
                .source(request.getSource())
                .destination(request.getDestination())
                .distanceKm(request.getDistanceKm())
                .durationHours(request.getDurationHours())
                .status(RouteStatus.ACTIVE)
                .build();
        return routeMapper.toResponse(routeRepository.save(route));
    }

    @Override
    @Transactional
    public RouteResponse updateRoute(Long id, RouteRequest request) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route", "id", id));
        route.setSource(request.getSource());
        route.setDestination(request.getDestination());
        route.setDistanceKm(request.getDistanceKm());
        route.setDurationHours(request.getDurationHours());
        return routeMapper.toResponse(routeRepository.save(route));
    }

    @Override
    @Transactional
    public void deleteRoute(Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route", "id", id));
        route.setStatus(RouteStatus.INACTIVE);
        routeRepository.save(route);
    }

    @Override
    @Transactional(readOnly = true)
    public RouteResponse getRouteById(Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route", "id", id));
        return routeMapper.toResponse(route);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RouteResponse> getRoutes(RouteStatus status) {
        return routeRepository.findAll().stream()
                .filter(route -> status == null || route.getStatus() == status)
                .map(routeMapper::toResponse)
                .toList();
    }
}
