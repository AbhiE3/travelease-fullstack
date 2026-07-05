package com.travelease.backend.busbooking.controller;

import com.travelease.backend.busbooking.dto.request.BusRequest;
import com.travelease.backend.busbooking.dto.response.ApiResponse;
import com.travelease.backend.busbooking.dto.response.BusResponse;
import com.travelease.backend.busbooking.dto.response.MessageResponse;
import com.travelease.backend.busbooking.entity.enums.BusStatus;
import com.travelease.backend.busbooking.service.BusService;
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
@RequestMapping("/api/buses")
@RequiredArgsConstructor
@Tag(name = "Bus Management", description = "Endpoints for managing buses")
public class BusController {

    private final BusService busService;

    @GetMapping
    @Operation(summary = "Get buses with optional filters")
    public ResponseEntity<ApiResponse<List<BusResponse>>> getBuses(
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) BusStatus status) {
        List<BusResponse> response = busService.getBuses(providerId, status);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Buses fetched successfully", response, "/api/buses"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get bus by ID")
    public ResponseEntity<ApiResponse<BusResponse>> getBusById(@PathVariable Long id) {
        BusResponse response = busService.getBusById(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Bus fetched successfully", response, "/api/buses/" + id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new bus")
    public ResponseEntity<ApiResponse<BusResponse>> createBus(@Valid @RequestBody BusRequest request) {
        BusResponse response = busService.createBus(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Bus created successfully", response, "/api/buses"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update bus by ID")
    public ResponseEntity<ApiResponse<BusResponse>> updateBus(@PathVariable Long id,
                                                               @Valid @RequestBody BusRequest request) {
        BusResponse response = busService.updateBus(id, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Bus updated successfully", response, "/api/buses/" + id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft delete bus by ID")
    public ResponseEntity<ApiResponse<MessageResponse>> deleteBus(@PathVariable Long id) {
        busService.deleteBus(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Bus deleted successfully", new MessageResponse("Bus deleted successfully"), "/api/buses/" + id));
    }
}
