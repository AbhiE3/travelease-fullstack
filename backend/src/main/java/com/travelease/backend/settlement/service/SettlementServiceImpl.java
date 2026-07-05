package com.travelease.backend.settlement.service;

import com.travelease.backend.auth.entity.User;
import com.travelease.backend.expense.entity.ExpenseParticipant;
import com.travelease.backend.expense.repository.ExpenseParticipantRepository;
import com.travelease.backend.settlement.dto.SettlementResponse;
import com.travelease.backend.settlement.dto.SettlementSummaryResponse;
import com.travelease.backend.settlement.entity.Settlement;
import com.travelease.backend.settlement.entity.SettlementStatus;
import com.travelease.backend.settlement.mapper.SettlementMapper;
import com.travelease.backend.settlement.repository.SettlementRepository;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementServiceImpl implements SettlementService {

    private final SettlementRepository settlementRepository;
    private final ExpenseParticipantRepository expenseParticipantRepository;
    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final SettlementMapper settlementMapper;

    @Override
    @Transactional
    public List<SettlementResponse> getMySettlements(UUID tripId, String currentUserEmail) {
        recalculateSettlements(tripId, currentUserEmail);
        return settlementRepository.findParticipantSettlements(tripId, currentUserEmail)
                .stream()
                .filter(settlement -> settlement.getAmount().signum() > 0)
                .sorted(Comparator.comparing(Settlement::getId))
                .map(settlementMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public SettlementSummaryResponse getTripSummary(UUID tripId, String currentUserEmail) {
        List<Settlement> settlements = recalculateSettlements(tripId, currentUserEmail);
        List<SettlementResponse> responses = settlements.stream()
                .filter(settlement -> settlement.getAmount().signum() > 0)
                .sorted(Comparator.comparing(Settlement::getId))
                .map(settlementMapper::toResponse)
                .toList();
        BigDecimal totalPayable = responses.stream()
                .map(SettlementResponse::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SettlementSummaryResponse(tripId, totalPayable, totalPayable, responses);
    }

    @Override
    @Transactional
    public SettlementResponse markPaid(UUID settlementId, String currentUserEmail) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement with id " + settlementId + " not found"));
        boolean participant = settlement.getPayer().getEmail().equals(currentUserEmail)
                || settlement.getReceiver().getEmail().equals(currentUserEmail);
        if (!participant) {
            throw new AccessDeniedException("Current user cannot update this settlement");
        }

        settlement.setStatus(SettlementStatus.PAID);
        Settlement saved = settlementRepository.save(settlement);
        log.info("Settlement {} marked as paid", saved.getId());
        return settlementMapper.toResponse(saved);
    }

    private List<Settlement> recalculateSettlements(UUID tripId, String currentUserEmail) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip with id " + tripId + " not found"));
        ensureCurrentUserIsMember(tripId, currentUserEmail);

        Map<PairKey, BigDecimal> directDebts = calculateDirectDebts(tripId);
        Map<PairKey, BigDecimal> netDebts = netReciprocalDebts(directDebts);

        Map<UUID, User> usersById = tripMemberRepository.findByTripId(tripId).stream()
                .map(TripMember::getUser)
                .collect(Collectors.toMap(User::getId, Function.identity()));

        Set<PairKey> currentKeys = new HashSet<>();
        for (Map.Entry<PairKey, BigDecimal> entry : netDebts.entrySet()) {
            PairKey key = entry.getKey();
            BigDecimal amount = entry.getValue();
            if (amount.signum() <= 0) {
                continue;
            }
            currentKeys.add(key);
            Settlement settlement = settlementRepository.findByTripIdAndPayerIdAndReceiverId(
                            tripId, key.payerId(), key.receiverId()
                    )
                    .orElseGet(() -> newSettlement(trip, usersById.get(key.payerId()), usersById.get(key.receiverId())));

            if (settlement.getAmount() == null || settlement.getAmount().compareTo(amount) != 0) {
                settlement.setAmount(amount);
                settlement.setStatus(SettlementStatus.PENDING);
            }
            settlementRepository.save(settlement);
        }

        List<Settlement> existing = settlementRepository.findByTripId(tripId);
        for (Settlement settlement : existing) {
            PairKey key = new PairKey(settlement.getPayer().getId(), settlement.getReceiver().getId());
            if (!currentKeys.contains(key) && settlement.getStatus() == SettlementStatus.PENDING) {
                settlementRepository.delete(settlement);
            }
        }

        return settlementRepository.findByTripId(tripId);
    }

    private Map<PairKey, BigDecimal> calculateDirectDebts(UUID tripId) {
        Map<PairKey, BigDecimal> debts = new HashMap<>();
        for (ExpenseParticipant member : expenseParticipantRepository.findByExpenseTripId(tripId)) {
            UUID payerId = member.getExpense().getPayer().getId();
            UUID participantId = member.getUser().getId();
            if (payerId.equals(participantId)) {
                continue;
            }
            PairKey key = new PairKey(participantId, payerId);
            debts.merge(key, member.getShareAmount(), BigDecimal::add);
        }
        return debts;
    }

    private Map<PairKey, BigDecimal> netReciprocalDebts(Map<PairKey, BigDecimal> directDebts) {
        Map<PairKey, BigDecimal> netDebts = new HashMap<>();
        Set<PairKey> visited = new HashSet<>();

        for (Map.Entry<PairKey, BigDecimal> entry : directDebts.entrySet()) {
            PairKey key = entry.getKey();
            if (visited.contains(key)) {
                continue;
            }
            PairKey reverse = new PairKey(key.receiverId(), key.payerId());
            BigDecimal forward = entry.getValue();
            BigDecimal backward = directDebts.getOrDefault(reverse, BigDecimal.ZERO);
            BigDecimal net = forward.subtract(backward);
            if (net.signum() > 0) {
                netDebts.put(key, net);
            } else if (net.signum() < 0) {
                netDebts.put(reverse, net.abs());
            }
            visited.add(key);
            visited.add(reverse);
        }
        return netDebts;
    }

    private Settlement newSettlement(Trip trip, User payer, User receiver) {
        Settlement settlement = new Settlement();
        settlement.setTrip(trip);
        settlement.setPayer(payer);
        settlement.setReceiver(receiver);
        settlement.setAmount(BigDecimal.ZERO);
        settlement.setStatus(SettlementStatus.PENDING);
        return settlement;
    }

    private void ensureCurrentUserIsMember(UUID tripId, String email) {
        if (!tripMemberRepository.existsByTripIdAndUserEmail(tripId, email)) {
            throw new AccessDeniedException("Current user is not a member of this trip");
        }
    }

    private record PairKey(UUID payerId, UUID receiverId) {
    }
}
