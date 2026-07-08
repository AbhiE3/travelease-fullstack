package com.travelease.backend.admin.controller;

import com.travelease.backend.admin.dto.DestinationResponse;
import com.travelease.backend.admin.service.DestinationService;
import com.travelease.backend.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/destinations")
@RequiredArgsConstructor
public class DestinationController {

    private final DestinationService destinationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DestinationResponse>>> getAllDestinations() {
        List<DestinationResponse> response = destinationService.getAllDestinations();
        return ResponseEntity.ok(ApiResponse.success(response, "Destinations retrieved"));
    }

    @GetMapping("/{destinationId}")
    public ResponseEntity<ApiResponse<DestinationResponse>> getDestinationById(
            @PathVariable Integer destinationId
    ) {
        DestinationResponse response = destinationService.getDestinationById(destinationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Destination retrieved"));
    }
}
