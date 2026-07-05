package com.travelease.backend.budget.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BudgetSummaryResponse(
        UUID tripId,
        BigDecimal totalBudget,
        BigDecimal totalSpent,
        BigDecimal remainingBudget,
        BigDecimal utilizationPercentage,
        boolean overspent,
        List<BudgetMemberSummaryResponse> members
) {
}
