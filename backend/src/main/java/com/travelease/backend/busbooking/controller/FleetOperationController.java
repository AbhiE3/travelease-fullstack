package com.travelease.backend.busbooking.controller;

import com.travelease.backend.busbooking.dto.request.ConductorRequest;
import com.travelease.backend.busbooking.dto.request.DriverRequest;
import com.travelease.backend.busbooking.dto.request.MaintenanceRequest;
import com.travelease.backend.busbooking.dto.request.MaintenanceStatusTransitionRequest;
import com.travelease.backend.busbooking.dto.request.TripAssignmentRequest;
import com.travelease.backend.busbooking.dto.request.TripStatusTransitionRequest;
import com.travelease.backend.busbooking.dto.response.*;
import com.travelease.backend.busbooking.entity.enums.ConductorStatus;
import com.travelease.backend.busbooking.entity.enums.DriverStatus;
import com.travelease.backend.busbooking.entity.enums.MaintenanceStatus;
import com.travelease.backend.busbooking.entity.enums.TripStatus;
import com.travelease.backend.busbooking.service.ConductorService;
import com.travelease.backend.busbooking.service.DriverService;
import com.travelease.backend.busbooking.service.MaintenanceService;
import com.travelease.backend.busbooking.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/operations")
@RequiredArgsConstructor
@Tag(name = "Fleet Operations", description = "Transport provider operations: drivers, conductors, maintenance, trips")
public class FleetOperationController {

    private final DriverService driverService;
    private final ConductorService conductorService;
    private final MaintenanceService maintenanceService;
    private final TripService tripService;

    // ==================== DRIVER MANAGEMENT ====================

    @GetMapping("/drivers")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get drivers with optional filters")
    public ResponseEntity<ApiResponse<List<DriverResponse>>> getAllDrivers(
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) DriverStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        List<DriverResponse> response = driverService.getDrivers(providerId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Drivers fetched successfully", response, "/api/operations/drivers"));
    }

    @GetMapping("/drivers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get driver by ID")
    public ResponseEntity<ApiResponse<DriverResponse>> getDriverById(@PathVariable Long id) {
        DriverResponse response = driverService.getDriverById(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Driver fetched successfully", response, "/api/operations/drivers/" + id));
    }

    @PostMapping("/drivers")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create driver")
    public ResponseEntity<ApiResponse<DriverResponse>> createDriver(@Valid @RequestBody DriverRequest request) {
        DriverResponse response = driverService.createDriver(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Driver created successfully", response, "/api/operations/drivers"));
    }

    @PutMapping("/drivers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update driver")
    public ResponseEntity<ApiResponse<DriverResponse>> updateDriver(@PathVariable Long id, @Valid @RequestBody DriverRequest request) {
        DriverResponse response = driverService.updateDriver(id, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Driver updated successfully", response, "/api/operations/drivers/" + id));
    }

    @PatchMapping("/drivers/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update driver status")
    public ResponseEntity<ApiResponse<DriverResponse>> updateDriverStatus(
            @PathVariable Long id,
            @RequestParam DriverStatus status) {
        DriverResponse response = driverService.updateDriverStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Driver status updated successfully", response, "/api/operations/drivers/" + id + "/status"));
    }

    @DeleteMapping("/drivers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate driver")
    public ResponseEntity<ApiResponse<MessageResponse>> deactivateDriver(@PathVariable Long id) {
        driverService.deactivateDriver(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Driver deactivated successfully", new MessageResponse("Driver deactivated successfully"), "/api/operations/drivers/" + id));
    }

    // ==================== CONDUCTOR MANAGEMENT ====================

    @GetMapping("/conductors")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get conductors with optional filters")
    public ResponseEntity<ApiResponse<List<ConductorResponse>>> getAllConductors(
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) ConductorStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        List<ConductorResponse> response = conductorService.getConductors(providerId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Conductors fetched successfully", response, "/api/operations/conductors"));
    }

    @GetMapping("/conductors/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get conductor by ID")
    public ResponseEntity<ApiResponse<ConductorResponse>> getConductorById(@PathVariable Long id) {
        ConductorResponse response = conductorService.getConductorById(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Conductor fetched successfully", response, "/api/operations/conductors/" + id));
    }

    @PostMapping("/conductors")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create conductor")
    public ResponseEntity<ApiResponse<ConductorResponse>> createConductor(@Valid @RequestBody ConductorRequest request) {
        ConductorResponse response = conductorService.createConductor(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Conductor created successfully", response, "/api/operations/conductors"));
    }

    @PutMapping("/conductors/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update conductor")
    public ResponseEntity<ApiResponse<ConductorResponse>> updateConductor(@PathVariable Long id, @Valid @RequestBody ConductorRequest request) {
        ConductorResponse response = conductorService.updateConductor(id, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Conductor updated successfully", response, "/api/operations/conductors/" + id));
    }

    @PatchMapping("/conductors/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update conductor status")
    public ResponseEntity<ApiResponse<ConductorResponse>> updateConductorStatus(
            @PathVariable Long id,
            @RequestParam ConductorStatus status) {
        ConductorResponse response = conductorService.updateConductorStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Conductor status updated successfully", response, "/api/operations/conductors/" + id + "/status"));
    }

    @DeleteMapping("/conductors/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate conductor")
    public ResponseEntity<ApiResponse<MessageResponse>> deactivateConductor(@PathVariable Long id) {
        conductorService.deactivateConductor(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Conductor deactivated successfully", new MessageResponse("Conductor deactivated successfully"), "/api/operations/conductors/" + id));
    }

    // ==================== MAINTENANCE MANAGEMENT ====================

    @GetMapping("/maintenance")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get maintenance records with optional filters")
    public ResponseEntity<ApiResponse<List<MaintenanceResponse>>> getMaintenanceRecords(
            @RequestParam(required = false) Long busId,
            @RequestParam(required = false) MaintenanceStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        List<MaintenanceResponse> response = maintenanceService.getMaintenanceRecords(busId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Maintenance records fetched successfully", response, "/api/operations/maintenance"));
    }

    @GetMapping("/maintenance/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get maintenance record by ID")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> getMaintenanceById(@PathVariable Long id) {
        MaintenanceResponse response = maintenanceService.getMaintenanceById(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Maintenance record fetched successfully", response, "/api/operations/maintenance/" + id));
    }

    @PostMapping("/maintenance")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Schedule maintenance")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> scheduleMaintenance(@Valid @RequestBody MaintenanceRequest request) {
        MaintenanceResponse response = maintenanceService.scheduleMaintenance(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Maintenance scheduled successfully", response, "/api/operations/maintenance"));
    }

    @PutMapping("/maintenance/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update maintenance record")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> updateMaintenance(@PathVariable Long id, @Valid @RequestBody MaintenanceRequest request) {
        MaintenanceResponse response = maintenanceService.updateMaintenance(id, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Maintenance record updated successfully", response, "/api/operations/maintenance/" + id));
    }

    @PatchMapping("/maintenance/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Transition maintenance status (start/complete/cancel)")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> transitionMaintenanceStatus(
            @PathVariable Long id,
            @Valid @RequestBody MaintenanceStatusTransitionRequest request) {
        MaintenanceResponse response = maintenanceService.transitionMaintenance(id, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Maintenance status updated successfully", response, "/api/operations/maintenance/" + id + "/status"));
    }

    @GetMapping("/maintenance/bus/{busId}/cost")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get total maintenance cost by bus")
    public ResponseEntity<ApiResponse<Double>> getTotalMaintenanceCost(@PathVariable Long busId) {
        Double cost = maintenanceService.getTotalMaintenanceCost(busId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Total maintenance cost fetched successfully", cost, "/api/operations/maintenance/bus/" + busId + "/cost"));
    }

    // ==================== TRIP OPERATIONS ====================

    @GetMapping("/trips")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get trips with optional filters")
    public ResponseEntity<ApiResponse<List<TripResponse>>> getTrips(
            @RequestParam(required = false) Long scheduleId,
            @RequestParam(required = false) Long driverId,
            @RequestParam(required = false) Long conductorId,
            @RequestParam(required = false) TripStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        List<TripResponse> response = tripService.getTrips(scheduleId, driverId, conductorId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Trips fetched successfully", response, "/api/operations/trips"));
    }

    @GetMapping("/trips/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get trip by ID")
    public ResponseEntity<ApiResponse<TripResponse>> getTripById(@PathVariable Long id) {
        TripResponse response = tripService.getTripById(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Trip fetched successfully", response, "/api/operations/trips/" + id));
    }

    @PostMapping("/trips/assign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign trip")
    public ResponseEntity<ApiResponse<TripResponse>> assignTrip(@Valid @RequestBody TripAssignmentRequest request) {
        TripResponse response = tripService.assignTrip(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Trip assigned successfully", response, "/api/operations/trips/assign"));
    }

    @PatchMapping("/trips/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Transition trip status (boarding/depart/running/delayed/arrived/complete/cancel)")
    public ResponseEntity<ApiResponse<TripResponse>> transitionTripStatus(
            @PathVariable Long id,
            @Valid @RequestBody TripStatusTransitionRequest request) {
        TripResponse response = tripService.transitionTrip(id, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Trip status updated successfully", response, "/api/operations/trips/" + id + "/status"));
    }

    // ==================== FLEET AVAILABILITY ====================

    @GetMapping("/fleet/availability/{providerId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get fleet availability by provider")
    public ResponseEntity<ApiResponse<FleetAvailabilityResponse>> getFleetAvailability(@PathVariable Long providerId) {
        FleetAvailabilityResponse response = tripService.getFleetAvailability(providerId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Fleet availability fetched successfully", response, "/api/operations/fleet/availability/" + providerId));
    }
}
