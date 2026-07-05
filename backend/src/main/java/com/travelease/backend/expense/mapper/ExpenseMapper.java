package com.travelease.backend.expense.mapper;

import com.travelease.backend.expense.dto.ExpenseResponse;
import com.travelease.backend.expense.dto.ExpenseParticipantResponse;
import com.travelease.backend.expense.entity.Expense;
import com.travelease.backend.expense.entity.ExpenseParticipant;
import org.springframework.stereotype.Component;

@Component
public class ExpenseMapper {

    public ExpenseResponse toResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getTrip().getId(),
                expense.getAmount(),
                expense.getCategory(),
                expense.getExpenseDate(),
                expense.getDescription(),
                expense.getPayer().getId(),
                expense.getPayer().getName(),
                expense.getParticipants().stream().map(this::toParticipantResponse).toList(),
                expense.getCreatedAt()
        );
    }

    private ExpenseParticipantResponse toParticipantResponse(ExpenseParticipant participant) {
        return new ExpenseParticipantResponse(
                participant.getUser().getId(),
                participant.getUser().getName(),
                participant.getShareAmount()
        );
    }
}
