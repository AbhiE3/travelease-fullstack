package com.travelease.backend.budget.controller;

import com.travelease.backend.budget.dto.BudgetResponse;
import com.travelease.backend.budget.dto.BudgetSummaryResponse;
import com.travelease.backend.budget.service.BudgetService;
import com.travelease.backend.shared.dto.ApiResponse;
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
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<BudgetResponse>> getMyBudget(
            @PathVariable UUID tripId,
            Authentication authentication
    ) {
        BudgetResponse response = budgetService.getMyBudget(tripId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Budget usage retrieved"));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<BudgetSummaryResponse>> getTripBudgetSummary(
            @PathVariable UUID tripId,
            Authentication authentication
    ) {
        BudgetSummaryResponse response = budgetService.getTripSummary(tripId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Trip budget summary retrieved"));
    }
}
