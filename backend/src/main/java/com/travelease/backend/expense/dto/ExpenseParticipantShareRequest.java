package com.travelease.backend.expense.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ExpenseParticipantShareRequest(
        @NotNull UUID userId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal shareAmount
) {
}
