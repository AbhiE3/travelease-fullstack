package com.travelease.backend.budget.service;

import com.travelease.backend.budget.dto.BudgetResponse;
import com.travelease.backend.budget.dto.BudgetSummaryResponse;

import java.util.UUID;

public interface BudgetService {

    BudgetResponse getMyBudget(UUID tripId, String currentUserEmail);

    BudgetSummaryResponse getTripSummary(UUID tripId, String currentUserEmail);
}
