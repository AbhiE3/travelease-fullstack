package com.travelease.backend.budget.mapper;

import com.travelease.backend.budget.dto.BudgetMemberSummaryResponse;
import com.travelease.backend.budget.dto.BudgetResponse;
import com.travelease.backend.trip.entity.TripMember;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Component
public class BudgetMapper {

    public BudgetResponse toResponse(UUID tripId, UUID userId, BigDecimal budgetAmount, BigDecimal spentAmount) {
        BigDecimal remaining = budgetAmount.subtract(spentAmount);
        return new BudgetResponse(
                tripId,
                userId,
                budgetAmount,
                spentAmount,
                remaining,
                utilization(budgetAmount, spentAmount),
                remaining.signum() < 0
        );
    }

    public BudgetMemberSummaryResponse toMemberSummary(TripMember member) {
        BigDecimal remaining = member.getBudgetAmount().subtract(member.getSpentAmount());
        return new BudgetMemberSummaryResponse(
                member.getUser().getId(),
                member.getUser().getName(),
                member.getBudgetAmount(),
                member.getSpentAmount(),
                remaining,
                utilization(member.getBudgetAmount(), member.getSpentAmount()),
                remaining.signum() < 0
        );
    }

    public BigDecimal utilization(BigDecimal budgetAmount, BigDecimal spentAmount) {
        if (budgetAmount == null || budgetAmount.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return spentAmount.multiply(BigDecimal.valueOf(100))
                .divide(budgetAmount, 2, RoundingMode.HALF_UP);
    }
}
