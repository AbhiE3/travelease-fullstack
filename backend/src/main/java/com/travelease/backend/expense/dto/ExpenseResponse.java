package com.travelease.backend.expense.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        UUID tripId,
        BigDecimal amount,
        String category,
        LocalDate expenseDate,
        String description,
        UUID payerId,
        String payerName,
        List<ExpenseParticipantResponse> participants,
        LocalDateTime createdAt
) {
}
