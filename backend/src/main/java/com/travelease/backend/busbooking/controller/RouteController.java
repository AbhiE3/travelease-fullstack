package com.travelease.backend.busbooking.controller;

import com.travelease.backend.busbooking.dto.request.RouteRequest;
import com.travelease.backend.busbooking.dto.response.ApiResponse;
import com.travelease.backend.busbooking.dto.response.MessageResponse;
import com.travelease.backend.busbooking.dto.response.RouteResponse;
import com.travelease.backend.busbooking.entity.enums.RouteStatus;
import com.travelease.backend.busbooking.service.RouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
@Tag(name = "Route Management", description = "Endpoints for managing bus routes")
public class RouteController {

    private final RouteService routeService;

    @GetMapping
    @Operation(summary = "Get routes with optional filters", description = "Get routes with optional filters")
    public ResponseEntity<ApiResponse<List<RouteResponse>>> getRoutes(
            @RequestParam(required = false) RouteStatus status) {
        List<RouteResponse> response = routeService.getRoutes(status);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Routes fetched successfully", response, "/api/routes"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get route by ID", description = "Get route by ID")
    public ResponseEntity<ApiResponse<RouteResponse>> getRouteById(@PathVariable Long id) {
        RouteResponse response = routeService.getRouteById(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Route fetched successfully", response, "/api/routes/" + id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new route", description = "Create a new route")
    public ResponseEntity<ApiResponse<RouteResponse>> createRoute(@Valid @RequestBody RouteRequest request) {
        RouteResponse response = routeService.createRoute(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Route created successfully", response, "/api/routes"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update route by ID", description = "Update route by ID")
    public ResponseEntity<ApiResponse<RouteResponse>> updateRoute(@PathVariable Long id,
                                                                   @Valid @RequestBody RouteRequest request) {
        RouteResponse response = routeService.updateRoute(id, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Route updated successfully", response, "/api/routes/" + id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete route by ID", description = "Delete route by ID")
    public ResponseEntity<ApiResponse<MessageResponse>> deleteRoute(@PathVariable Long id) {
        routeService.deleteRoute(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Route deleted successfully", new MessageResponse("Route deleted successfully"), "/api/routes/" + id));
    }
}

