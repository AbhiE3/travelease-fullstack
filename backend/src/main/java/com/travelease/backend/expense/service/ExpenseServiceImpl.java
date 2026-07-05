package com.travelease.backend.expense.service;

import com.travelease.backend.auth.entity.User;
import com.travelease.backend.auth.repository.UserRepository;
import com.travelease.backend.expense.dto.CreateExpenseRequest;
import com.travelease.backend.expense.dto.ExpenseResponse;
import com.travelease.backend.expense.dto.ExpenseParticipantShareRequest;
import com.travelease.backend.expense.entity.Expense;
import com.travelease.backend.expense.entity.ExpenseParticipant;
import com.travelease.backend.expense.mapper.ExpenseMapper;
import com.travelease.backend.expense.repository.ExpenseRepository;
import com.travelease.backend.shared.exception.InvalidRequestException;
import com.travelease.backend.shared.exception.ResourceNotFoundException;
import com.travelease.backend.trip.entity.Trip;
import com.travelease.backend.trip.entity.TripMember;
import com.travelease.backend.trip.repository.TripMemberRepository;
import com.travelease.backend.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final UserRepository userRepository;
    private final ExpenseMapper expenseMapper;

    @Override
    @Transactional
    public ExpenseResponse createSharedExpense(UUID tripId, CreateExpenseRequest request, String currentUserEmail) {
        Trip trip = findTrip(tripId);
        ensureCurrentUserIsMember(tripId, currentUserEmail);

        Set<UUID> participantIds = new LinkedHashSet<>(request.participantIds());
        if (participantIds.isEmpty()) {
            throw new InvalidRequestException("At least one participant is required");
        }
        if (!tripMemberRepository.existsByTripIdAndUserId(tripId, request.payerId())) {
            throw new InvalidRequestException("Payer must be a trip member");
        }

        List<TripMember> participantMembers = tripMemberRepository.findByTripIdAndUserIdIn(tripId, participantIds);
        if (participantMembers.size() != participantIds.size()) {
            throw new InvalidRequestException("All participants must be trip members");
        }

        User payer = userRepository.findById(request.payerId())
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + request.payerId() + " not found"));
        Map<UUID, User> participantsById = participantMembers.stream()
                .map(TripMember::getUser)
                .collect(Collectors.toMap(User::getId, Function.identity()));

        Expense expense = new Expense();
        expense.setTrip(trip);
        expense.setPayer(payer);
        expense.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
        expense.setCategory(request.category());
        expense.setExpenseDate(request.expenseDate() == null ? LocalDate.now() : request.expenseDate());
        expense.setDescription(request.description());

        List<UUID> orderedParticipantIds = new ArrayList<>(participantIds);
        Map<UUID, BigDecimal> sharesByParticipantId = resolveShares(expense.getAmount(), orderedParticipantIds, request);
        for (int i = 0; i < orderedParticipantIds.size(); i++) {
            User participant = participantsById.get(orderedParticipantIds.get(i));
            BigDecimal share = sharesByParticipantId.get(participant.getId());

            ExpenseParticipant expenseParticipant = new ExpenseParticipant();
            expenseParticipant.setExpense(expense);
            expenseParticipant.setUser(participant);
            expenseParticipant.setShareAmount(share);
            expense.getParticipants().add(expenseParticipant);

            TripMember tripMember = participantMembers.stream()
                    .filter(member -> member.getUser().getId().equals(participant.getId()))
                    .findFirst()
                    .orElseThrow(() -> new InvalidRequestException("Participant must be a trip member"));
            tripMember.setSpentAmount(tripMember.getSpentAmount().add(share));
            tripMemberRepository.save(tripMember);
        }

        Expense saved = expenseRepository.save(expense);
        log.info("Shared expense {} created for trip {}", saved.getId(), tripId);
        return expenseMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getTripExpenses(UUID tripId, String currentUserEmail) {
        findTrip(tripId);
        ensureCurrentUserIsMember(tripId, currentUserEmail);
        return expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId).stream()
                .map(expenseMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseResponse getTripExpense(UUID tripId, UUID expenseId, String currentUserEmail) {
        findTrip(tripId);
        ensureCurrentUserIsMember(tripId, currentUserEmail);
        Expense expense = expenseRepository.findByIdAndTripId(expenseId, tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense with id " + expenseId + " not found"));
        return expenseMapper.toResponse(expense);
    }

    private Trip findTrip(UUID tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip with id " + tripId + " not found"));
    }

    private void ensureCurrentUserIsMember(UUID tripId, String email) {
        if (!tripMemberRepository.existsByTripIdAndUserEmail(tripId, email)) {
            throw new AccessDeniedException("Current user is not a member of this trip");
        }
    }

    private Map<UUID, BigDecimal> resolveShares(
            BigDecimal amount,
            List<UUID> orderedParticipantIds,
            CreateExpenseRequest request
    ) {
        if (request.participantShares() == null || request.participantShares().isEmpty()) {
            List<BigDecimal> equalShares = splitAmount(amount, orderedParticipantIds.size());
            return toShareMap(orderedParticipantIds, equalShares);
        }

        Map<UUID, BigDecimal> sharesByParticipantId = request.participantShares().stream()
                .collect(Collectors.toMap(
                        ExpenseParticipantShareRequest::userId,
                        share -> share.shareAmount().setScale(2, RoundingMode.HALF_UP),
                        (left, right) -> {
                            throw new InvalidRequestException("Duplicate participant share provided");
                        }
                ));

        Set<UUID> participantIds = new LinkedHashSet<>(orderedParticipantIds);
        if (!sharesByParticipantId.keySet().equals(participantIds)) {
            throw new InvalidRequestException("Custom shares must be provided for exactly the selected participants");
        }

        BigDecimal totalShares = sharesByParticipantId.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalShares.compareTo(amount) != 0) {
            throw new InvalidRequestException("Custom shares must add up to the expense amount");
        }

        return sharesByParticipantId;
    }

    private Map<UUID, BigDecimal> toShareMap(List<UUID> participantIds, List<BigDecimal> shares) {
        return java.util.stream.IntStream.range(0, participantIds.size())
                .boxed()
                .collect(Collectors.toMap(participantIds::get, shares::get));
    }

    private List<BigDecimal> splitAmount(BigDecimal amount, int participantCount) {
        BigDecimal baseShare = amount.divide(BigDecimal.valueOf(participantCount), 2, RoundingMode.DOWN);
        BigDecimal allocated = baseShare.multiply(BigDecimal.valueOf(participantCount));
        BigDecimal remainder = amount.subtract(allocated);

        List<BigDecimal> shares = new ArrayList<>();
        for (int i = 0; i < participantCount; i++) {
            BigDecimal share = i == 0 ? baseShare.add(remainder) : baseShare;
            shares.add(share.setScale(2, RoundingMode.HALF_UP));
        }
        return shares;
    }
}
