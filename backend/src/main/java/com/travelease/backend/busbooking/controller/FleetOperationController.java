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
import com.travelease.backend.busbooking.service.BusService;
import com.travelease.backend.busbooking.service.ConductorService;
import com.travelease.backend.busbooking.service.DriverService;
import com.travelease.backend.busbooking.service.MaintenanceService;
import com.travelease.backend.busbooking.service.ScheduleService;
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
    private final BusService busService;
    private final ScheduleService scheduleService;
    private final com.travelease.backend.busbooking.security.SecurityUtil securityUtil;

    // ==================== DRIVER MANAGEMENT ====================

    @GetMapping("/drivers")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Get drivers with optional filters", description = "Get drivers with optional filters")
    public ResponseEntity<ApiResponse<List<DriverResponse>>> getAllDrivers(
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) DriverStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Long effectiveProviderId = securityUtil.resolveEffectiveProviderId(providerId);
        List<DriverResponse> response = driverService.getDrivers(effectiveProviderId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Drivers fetched successfully", response, "/api/operations/drivers"));
    }

    @GetMapping("/drivers/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Get driver by ID", description = "Get driver by ID")
    public ResponseEntity<ApiResponse<DriverResponse>> getDriverById(@PathVariable Long id) {
        assertOwnsDriver(id);
        DriverResponse response = driverService.getDriverById(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Driver fetched successfully", response, "/api/operations/drivers/" + id));
    }

    @PostMapping("/drivers")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Create driver", description = "Create driver")
    public ResponseEntity<ApiResponse<DriverResponse>> createDriver(@Valid @RequestBody DriverRequest request) {
        request.setProviderId(securityUtil.resolveEffectiveProviderId(request.getProviderId()));
        DriverResponse response = driverService.createDriver(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Driver created successfully", response, "/api/operations/drivers"));
    }

    @PutMapping("/drivers/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Update driver", description = "Update driver")
    public ResponseEntity<ApiResponse<DriverResponse>> updateDriver(@PathVariable Long id, @Valid @RequestBody DriverRequest request) {
        assertOwnsDriver(id);
        request.setProviderId(securityUtil.resolveEffectiveProviderId(request.getProviderId()));
        DriverResponse response = driverService.updateDriver(id, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Driver updated successfully", response, "/api/operations/drivers/" + id));
    }

    @DeleteMapping("/drivers/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Deactivate driver", description = "Deactivate driver")
    public ResponseEntity<ApiResponse<MessageResponse>> deactivateDriver(@PathVariable Long id) {
        assertOwnsDriver(id);
        driverService.deactivateDriver(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Driver deactivated successfully", new MessageResponse("Driver deactivated successfully"), "/api/operations/drivers/" + id));
    }

    // ==================== CONDUCTOR MANAGEMENT ====================

    @GetMapping("/conductors")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Get conductors with optional filters", description = "Get conductors with optional filters")
    public ResponseEntity<ApiResponse<List<ConductorResponse>>> getAllConductors(
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) ConductorStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Long effectiveProviderId = securityUtil.resolveEffectiveProviderId(providerId);
        List<ConductorResponse> response = conductorService.getConductors(effectiveProviderId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Conductors fetched successfully", response, "/api/operations/conductors"));
    }

    @GetMapping("/conductors/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Get conductor by ID", description = "Get conductor by ID")
    public ResponseEntity<ApiResponse<ConductorResponse>> getConductorById(@PathVariable Long id) {
        assertOwnsConductor(id);
        ConductorResponse response = conductorService.getConductorById(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Conductor fetched successfully", response, "/api/operations/conductors/" + id));
    }

    @PostMapping("/conductors")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Create conductor", description = "Create conductor")
    public ResponseEntity<ApiResponse<ConductorResponse>> createConductor(@Valid @RequestBody ConductorRequest request) {
        request.setProviderId(securityUtil.resolveEffectiveProviderId(request.getProviderId()));
        ConductorResponse response = conductorService.createConductor(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Conductor created successfully", response, "/api/operations/conductors"));
    }

    @PutMapping("/conductors/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Update conductor", description = "Update conductor")
    public ResponseEntity<ApiResponse<ConductorResponse>> updateConductor(@PathVariable Long id, @Valid @RequestBody ConductorRequest request) {
        assertOwnsConductor(id);
        request.setProviderId(securityUtil.resolveEffectiveProviderId(request.getProviderId()));
        ConductorResponse response = conductorService.updateConductor(id, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Conductor updated successfully", response, "/api/operations/conductors/" + id));
    }

    @DeleteMapping("/conductors/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Deactivate conductor", description = "Deactivate conductor")
    public ResponseEntity<ApiResponse<MessageResponse>> deactivateConductor(@PathVariable Long id) {
        assertOwnsConductor(id);
        conductorService.deactivateConductor(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Conductor deactivated successfully", new MessageResponse("Conductor deactivated successfully"), "/api/operations/conductors/" + id));
    }

    // ==================== MAINTENANCE MANAGEMENT ====================

    @GetMapping("/maintenance")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Get maintenance records with optional filters", description = "Get maintenance records with optional filters")
    public ResponseEntity<ApiResponse<List<MaintenanceResponse>>> getMaintenanceRecords(
            @RequestParam(required = false) Long busId,
            @RequestParam(required = false) MaintenanceStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        if (busId != null) {
            assertOwnsBus(busId);
        }
        Long effectiveProviderId = securityUtil.resolveEffectiveProviderId(null);
        List<MaintenanceResponse> response = maintenanceService.getMaintenanceRecords(effectiveProviderId, busId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Maintenance records fetched successfully", response, "/api/operations/maintenance"));
    }

    @GetMapping("/maintenance/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Get maintenance record by ID", description = "Get maintenance record by ID")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> getMaintenanceById(@PathVariable Long id) {
        assertOwnsMaintenance(id);
        MaintenanceResponse response = maintenanceService.getMaintenanceById(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Maintenance record fetched successfully", response, "/api/operations/maintenance/" + id));
    }

    @PostMapping("/maintenance")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Schedule maintenance", description = "Schedule maintenance")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> scheduleMaintenance(@Valid @RequestBody MaintenanceRequest request) {
        assertOwnsBus(request.getBusId());
        MaintenanceResponse response = maintenanceService.scheduleMaintenance(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Maintenance scheduled successfully", response, "/api/operations/maintenance"));
    }

    @PutMapping("/maintenance/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Update maintenance record", description = "Update maintenance record")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> updateMaintenance(@PathVariable Long id, @Valid @RequestBody MaintenanceRequest request) {
        // Ownership is asserted against the maintenance record's actual bus (not
        // request.getBusId()): MaintenanceServiceImpl.updateMaintenance never
        // reassigns the bus, so trusting the request body's busId here would let a
        // provider pass a busId they own while editing a record that actually
        // belongs to someone else's bus.
        assertOwnsMaintenance(id);
        MaintenanceResponse response = maintenanceService.updateMaintenance(id, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Maintenance record updated successfully", response, "/api/operations/maintenance/" + id));
    }

    @PatchMapping("/maintenance/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Transition maintenance status (start/complete/cancel)", description = "Transition maintenance status (start/complete/cancel)")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> transitionMaintenanceStatus(
            @PathVariable Long id,
            @Valid @RequestBody MaintenanceStatusTransitionRequest request) {
        assertOwnsMaintenance(id);
        MaintenanceResponse response = maintenanceService.transitionMaintenance(id, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Maintenance status updated successfully", response, "/api/operations/maintenance/" + id + "/status"));
    }

    @GetMapping("/maintenance/bus/{busId}/cost")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Get total maintenance cost by bus", description = "Get total maintenance cost by bus")
    public ResponseEntity<ApiResponse<Double>> getTotalMaintenanceCost(@PathVariable Long busId) {
        assertOwnsBus(busId);
        Double cost = maintenanceService.getTotalMaintenanceCost(busId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Total maintenance cost fetched successfully", cost, "/api/operations/maintenance/bus/" + busId + "/cost"));
    }

    // ==================== TRIP OPERATIONS ====================

    @GetMapping("/trips")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Get trips with optional filters", description = "Get trips with optional filters")
    public ResponseEntity<ApiResponse<List<TripResponse>>> getTrips(
            @RequestParam(required = false) Long scheduleId,
            @RequestParam(required = false) Long driverId,
            @RequestParam(required = false) Long conductorId,
            @RequestParam(required = false) TripStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        if (scheduleId != null) {
            assertOwnsSchedule(scheduleId);
        }
        List<TripResponse> response = tripService.getTrips(scheduleId, driverId, conductorId, status, pageable);
        // TripService has no providerId-based filter to scope the query by, so
        // regardless of which optional filters were supplied, post-filter the
        // result to the caller's own provider when they are a PROVIDER (not ADMIN)
        // to prevent cross-tenant data exposure.
        if (securityUtil.getCurrentUserRoles().contains("ROLE_PROVIDER")) {
            Long currentProviderId = securityUtil.getCurrentProviderId();
            response = response.stream()
                    .filter(t -> currentProviderId.equals(t.getProviderId()))
                    .toList();
        }
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Trips fetched successfully", response, "/api/operations/trips"));
    }

    @GetMapping("/trips/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Get trip by ID", description = "Get trip by ID")
    public ResponseEntity<ApiResponse<TripResponse>> getTripById(@PathVariable Long id) {
        assertOwnsTrip(id);
        TripResponse response = tripService.getTripById(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Trip fetched successfully", response, "/api/operations/trips/" + id));
    }

    @PostMapping("/trips/assign")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Assign trip", description = "Assign trip")
    public ResponseEntity<ApiResponse<TripResponse>> assignTrip(@Valid @RequestBody TripAssignmentRequest request) {
        assertOwnsSchedule(request.getScheduleId());
        TripResponse response = tripService.assignTrip(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Trip assigned successfully", response, "/api/operations/trips/assign"));
    }

    @PatchMapping("/trips/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Transition trip status (boarding/depart/running/delayed/arrived/complete/cancel)", description = "Transition trip status (boarding/depart/running/delayed/arrived/complete/cancel)")
    public ResponseEntity<ApiResponse<TripResponse>> transitionTripStatus(
            @PathVariable Long id,
            @Valid @RequestBody TripStatusTransitionRequest request) {
        assertOwnsTrip(id);
        TripResponse response = tripService.transitionTrip(id, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Trip status updated successfully", response, "/api/operations/trips/" + id + "/status"));
    }

    // ==================== FLEET AVAILABILITY ====================

    @GetMapping("/fleet/availability/{providerId}")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Get fleet availability by provider", description = "Get fleet availability by provider")
    public ResponseEntity<ApiResponse<FleetAvailabilityResponse>> getFleetAvailability(@PathVariable Long providerId) {
        Long effectiveProviderId = securityUtil.resolveEffectiveProviderId(providerId);
        FleetAvailabilityResponse response = tripService.getFleetAvailability(effectiveProviderId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Fleet availability fetched successfully", response, "/api/operations/fleet/availability/" + providerId));
    }

    // ==================== OWNERSHIP HELPERS ====================

    private void assertOwnsBus(Long busId) {
        securityUtil.resolveEffectiveProviderId(busService.getBusById(busId).getProviderId());
    }

    private void assertOwnsDriver(Long driverId) {
        securityUtil.resolveEffectiveProviderId(driverService.getDriverById(driverId).getProviderId());
    }

    private void assertOwnsConductor(Long conductorId) {
        securityUtil.resolveEffectiveProviderId(conductorService.getConductorById(conductorId).getProviderId());
    }

    private void assertOwnsMaintenance(Long maintenanceId) {
        assertOwnsBus(maintenanceService.getMaintenanceById(maintenanceId).getBusId());
    }

    private void assertOwnsSchedule(Long scheduleId) {
        ScheduleResponse existing = scheduleService.getScheduleById(scheduleId);
        securityUtil.resolveEffectiveProviderId(existing.getBus().getProviderId());
    }

    private void assertOwnsTrip(Long tripId) {
        securityUtil.resolveEffectiveProviderId(tripService.getTripById(tripId).getProviderId());
    }
}

