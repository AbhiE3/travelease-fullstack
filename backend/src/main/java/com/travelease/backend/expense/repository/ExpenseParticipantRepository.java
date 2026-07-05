package com.travelease.backend.expense.repository;

import com.travelease.backend.expense.entity.ExpenseParticipant;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExpenseParticipantRepository extends JpaRepository<ExpenseParticipant, UUID> {

    @EntityGraph(attributePaths = {"expense", "expense.payer", "user"})
    List<ExpenseParticipant> findByExpenseTripId(UUID tripId);
}
