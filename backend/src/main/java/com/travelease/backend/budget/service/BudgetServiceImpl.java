package com.travelease.backend.budget.service;

import com.travelease.backend.budget.dto.BudgetResponse;
import com.travelease.backend.budget.dto.BudgetSummaryResponse;
import com.travelease.backend.budget.mapper.BudgetMapper;
import com.travelease.backend.shared.exception.ResourceNotFoundException;
import com.travelease.backend.trip.entity.TripMember;
import com.travelease.backend.trip.entity.TripMemberStatus;
import com.travelease.backend.trip.repository.TripMemberRepository;
import com.travelease.backend.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final BudgetMapper budgetMapper;

    @Override
    @Transactional(readOnly = true)
    public BudgetResponse getMyBudget(UUID tripId, String currentUserEmail) {
        ensureTripExists(tripId);
        TripMember member = tripMemberRepository
                .findByTripIdAndUserEmailAndMemberStatus(tripId, currentUserEmail, TripMemberStatus.ACCEPTED)
                .orElseThrow(() -> new AccessDeniedException("Current user is not a member of this trip"));

        return budgetMapper.toResponse(
                tripId,
                member.getUser().getId(),
                member.getBudgetAmount(),
                member.getSpentAmount()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetSummaryResponse getTripSummary(UUID tripId, String currentUserEmail) {
        ensureTripExists(tripId);
        ensureCurrentUserIsMember(tripId, currentUserEmail);

        List<TripMember> acceptedMembers = tripMemberRepository.findByTripIdAndMemberStatus(
                tripId,
                TripMemberStatus.ACCEPTED
        );
        BigDecimal totalBudget = acceptedMembers.stream()
                .map(TripMember::getBudgetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSpent = acceptedMembers.stream()
                .map(TripMember::getSpentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = totalBudget.subtract(totalSpent);

        return new BudgetSummaryResponse(
                tripId,
                totalBudget,
                totalSpent,
                remaining,
                budgetMapper.utilization(totalBudget, totalSpent),
                remaining.signum() < 0,
                acceptedMembers.stream().map(budgetMapper::toMemberSummary).toList()
        );
    }

    private void ensureTripExists(UUID tripId) {
        if (!tripRepository.existsById(tripId)) {
            throw new ResourceNotFoundException("Trip with id " + tripId + " not found");
        }
    }

    private void ensureCurrentUserIsMember(UUID tripId, String email) {
        if (!tripMemberRepository.existsByTripIdAndUserEmailAndMemberStatus(tripId, email, TripMemberStatus.ACCEPTED)) {
            throw new AccessDeniedException("Current user is not a member of this trip");
        }
    }
}
