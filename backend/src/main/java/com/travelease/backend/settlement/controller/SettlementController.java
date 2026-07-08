package com.travelease.backend.settlement.controller;

import com.travelease.backend.settlement.dto.SettlementResponse;
import com.travelease.backend.settlement.dto.SettlementSummaryResponse;
import com.travelease.backend.settlement.service.SettlementService;
import com.travelease.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Trip Settlements", description = "Settlement recalculation and payment endpoints for trip participants")
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping("/api/trips/{tripId}/settlements/me")
    @Operation(summary = "Get my trip settlements", description = "ACCESS: AUTHENTICATED\nSCOPE: Accepted trip member only. Returns settlement rows where the current user participates as payer or receiver.\nIDENTITY: The current user is resolved from the JWT/email in the security context.")
    public ResponseEntity<ApiResponse<List<SettlementResponse>>> getMySettlements(
            @PathVariable UUID tripId,
            Authentication authentication
    ) {
        List<SettlementResponse> response = settlementService.getMySettlements(tripId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Settlement details retrieved"));
    }

    @GetMapping("/api/trips/{tripId}/settlements/summary")
    @Operation(summary = "Get trip settlement summary", description = "ACCESS: AUTHENTICATED\nSCOPE: Accepted trip member only. Recalculates and returns the trip settlement summary.\nIDENTITY: The current user is resolved from the JWT/email in the security context.")
    public ResponseEntity<ApiResponse<SettlementSummaryResponse>> getTripSettlementSummary(
            @PathVariable UUID tripId,
            Authentication authentication
    ) {
        SettlementSummaryResponse response = settlementService.getTripSummary(tripId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Settlement summary retrieved"));
    }

    @PatchMapping("/api/settlements/{settlementId}/paid")
    @Operation(summary = "Mark settlement as paid", description = "ACCESS: AUTHENTICATED\nSCOPE: Settlement participant only. The current user must be the payer or receiver of the settlement.\nIDENTITY: The current user is resolved from the JWT/email in the security context.")
    public ResponseEntity<ApiResponse<SettlementResponse>> markPaid(
            @PathVariable UUID settlementId,
            Authentication authentication
    ) {
        SettlementResponse response = settlementService.markPaid(settlementId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Settlement marked as paid"));
    }
}
