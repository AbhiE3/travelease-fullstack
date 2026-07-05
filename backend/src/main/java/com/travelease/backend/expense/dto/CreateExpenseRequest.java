package com.travelease.backend.expense.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateExpenseRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotBlank String category,
        @NotBlank String description,
        LocalDate expenseDate,
        @NotNull UUID payerId,
        @NotEmpty List<@NotNull UUID> participantIds,
        List<@Valid @NotNull ExpenseParticipantShareRequest> participantShares
) {
}
