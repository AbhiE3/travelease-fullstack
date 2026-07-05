package com.travelease.backend.expense.repository;

import com.travelease.backend.expense.entity.Expense;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    @EntityGraph(attributePaths = {"payer", "participants", "participants.user"})
    List<Expense> findByTripIdOrderByCreatedAtDesc(UUID tripId);

    @EntityGraph(attributePaths = {"payer", "participants", "participants.user"})
    Optional<Expense> findByIdAndTripId(UUID id, UUID tripId);
}
