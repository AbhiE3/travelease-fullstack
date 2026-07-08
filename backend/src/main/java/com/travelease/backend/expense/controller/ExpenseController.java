package com.travelease.backend.expense.controller;

import com.travelease.backend.expense.dto.CreateExpenseRequest;
import com.travelease.backend.expense.dto.ExpenseResponse;
import com.travelease.backend.expense.service.ExpenseService;
import com.travelease.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/trips/{tripId}/expenses")
@RequiredArgsConstructor
@Tag(name = "Trip Expenses", description = "Shared expense endpoints for accepted trip members")
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    @Operation(summary = "Create a shared expense", description = "ACCESS: AUTHENTICATED\nSCOPE: Accepted trip member only. Creates a trip expense and allocates shares to accepted trip participants.\nLIFECYCLE: Mutation is rejected once the Trip is COMPLETED or CANCELLED.\nIDENTITY: The current user is resolved from the JWT/email in the security context.")
    public ResponseEntity<ApiResponse<ExpenseResponse>> createSharedExpense(
            @PathVariable UUID tripId,
            @Valid @RequestBody CreateExpenseRequest request,
            Authentication authentication
    ) {
        ExpenseResponse response = expenseService.createSharedExpense(tripId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Shared expense recorded successfully"));
    }

    @GetMapping
    @Operation(summary = "List trip expenses", description = "ACCESS: AUTHENTICATED\nSCOPE: Accepted trip member only. Returns expenses recorded on the trip.\nIDENTITY: The current user is resolved from the JWT/email in the security context.")
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> getTripExpenses(
            @PathVariable UUID tripId,
            Authentication authentication
    ) {
        List<ExpenseResponse> response = expenseService.getTripExpenses(tripId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Trip expenses retrieved"));
    }

    @GetMapping("/{expenseId}")
    @Operation(summary = "Get trip expense by ID", description = "ACCESS: AUTHENTICATED\nSCOPE: Accepted trip member only. Returns a specific expense that belongs to the trip.\nIDENTITY: The current user is resolved from the JWT/email in the security context.")
    public ResponseEntity<ApiResponse<ExpenseResponse>> getTripExpense(
            @PathVariable UUID tripId,
            @PathVariable UUID expenseId,
            Authentication authentication
    ) {
        ExpenseResponse response = expenseService.getTripExpense(tripId, expenseId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Expense retrieved"));
    }
}
