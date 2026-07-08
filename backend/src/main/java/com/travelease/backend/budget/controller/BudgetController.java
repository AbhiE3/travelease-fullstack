package com.travelease.backend.budget.controller;

import com.travelease.backend.budget.dto.BudgetResponse;
import com.travelease.backend.budget.dto.BudgetSummaryResponse;
import com.travelease.backend.budget.service.BudgetService;
import com.travelease.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/trips/{tripId}/budget")
@RequiredArgsConstructor
@Tag(name = "Trip Budget", description = "Trip budget endpoints for the authenticated trip member")
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping("/me")
    @Operation(summary = "Get my budget share", description = "ACCESS: AUTHENTICATED\nSCOPE: Trip member only. Returns the current user's accepted budget allocation for the trip.\nIDENTITY: The current user is resolved from the JWT/email in the security context.")
    public ResponseEntity<ApiResponse<BudgetResponse>> getMyBudget(
            @PathVariable UUID tripId,
            Authentication authentication
    ) {
        BudgetResponse response = budgetService.getMyBudget(tripId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Budget usage retrieved"));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get trip budget summary", description = "ACCESS: AUTHENTICATED\nSCOPE: Accepted trip member only. Returns trip-wide budget totals for the current trip members.\nIDENTITY: The current user is resolved from the JWT/email in the security context.")
    public ResponseEntity<ApiResponse<BudgetSummaryResponse>> getTripBudgetSummary(
            @PathVariable UUID tripId,
            Authentication authentication
    ) {
        BudgetSummaryResponse response = budgetService.getTripSummary(tripId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Trip budget summary retrieved"));
    }
}
