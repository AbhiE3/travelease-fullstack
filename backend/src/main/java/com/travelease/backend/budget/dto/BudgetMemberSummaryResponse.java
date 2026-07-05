package com.travelease.backend.budget.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetMemberSummaryResponse(
        UUID userId,
        String name,
        BigDecimal budgetAmount,
        BigDecimal spentAmount,
        BigDecimal remainingAmount,
        BigDecimal utilizationPercentage,
        boolean overspent
) {
}
