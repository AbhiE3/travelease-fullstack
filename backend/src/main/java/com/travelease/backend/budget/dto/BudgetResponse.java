package com.travelease.backend.budget.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetResponse(
        UUID tripId,
        UUID userId,
        BigDecimal budgetAmount,
        BigDecimal spentAmount,
        BigDecimal remainingAmount,
        BigDecimal utilizationPercentage,
        boolean overspent
) {
}
