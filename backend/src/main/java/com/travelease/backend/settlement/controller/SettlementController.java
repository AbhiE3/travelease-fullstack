package com.travelease.backend.settlement.controller;

import com.travelease.backend.settlement.dto.SettlementResponse;
import com.travelease.backend.settlement.dto.SettlementSummaryResponse;
import com.travelease.backend.settlement.service.SettlementService;
import com.travelease.backend.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping("/api/trips/{tripId}/settlements/me")
    public ResponseEntity<ApiResponse<List<SettlementResponse>>> getMySettlements(
            @PathVariable UUID tripId,
            Authentication authentication
    ) {
        List<SettlementResponse> response = settlementService.getMySettlements(tripId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Settlement details retrieved"));
    }

    @GetMapping("/api/trips/{tripId}/settlements/summary")
    public ResponseEntity<ApiResponse<SettlementSummaryResponse>> getTripSettlementSummary(
            @PathVariable UUID tripId,
            Authentication authentication
    ) {
        SettlementSummaryResponse response = settlementService.getTripSummary(tripId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Settlement summary retrieved"));
    }

    @PatchMapping("/api/settlements/{settlementId}/paid")
    public ResponseEntity<ApiResponse<SettlementResponse>> markPaid(
            @PathVariable UUID settlementId,
            Authentication authentication
    ) {
        SettlementResponse response = settlementService.markPaid(settlementId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Settlement marked as paid"));
    }
}
