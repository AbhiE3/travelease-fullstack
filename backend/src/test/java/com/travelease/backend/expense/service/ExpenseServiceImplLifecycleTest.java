package com.travelease.backend.expense.service;

import com.travelease.backend.auth.entity.Role;
import com.travelease.backend.auth.entity.User;
import com.travelease.backend.auth.repository.UserRepository;
import com.travelease.backend.expense.dto.CreateExpenseRequest;
import com.travelease.backend.expense.mapper.ExpenseMapper;
import com.travelease.backend.expense.repository.ExpenseRepository;
import com.travelease.backend.shared.exception.InvalidRequestException;
import com.travelease.backend.trip.entity.Trip;
import com.travelease.backend.trip.entity.TravelerTripStatus;
import com.travelease.backend.trip.entity.TripMemberStatus;
import com.travelease.backend.trip.repository.TripMemberRepository;
import com.travelease.backend.trip.repository.TripRepository;
import com.travelease.backend.trip.security.TripAuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Verifies createSharedExpense respects TripAuthorizationService.requireMutableTrip
 * - a completed/cancelled Trip is historical and does not accept new expenses.
 */
@ExtendWith(MockitoExtension.class)
class ExpenseServiceImplLifecycleTest {

    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private TripMemberRepository tripMemberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ExpenseMapper expenseMapper;

    private User user(String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setName(email);
        user.setRole(Role.ROLE_TRAVELER);
        return user;
    }

    private Trip trip(User organizer, TravelerTripStatus status) {
        Trip trip = new Trip();
        trip.setId(UUID.randomUUID());
        trip.setTripName("Goa Trip");
        trip.setOrganizer(organizer);
        trip.setSourceLocation("Mumbai");
        trip.setDestinationId(1);
        trip.setBudgetAmount(new BigDecimal("1000.00"));
        trip.setCategoryId(1);
        trip.setStartDate(LocalDate.now().minusDays(5));
        trip.setEndDate(LocalDate.now().plusDays(5));
        trip.setStatus(status);
        return trip;
    }

    @Test
    void createSharedExpenseIsRejectedOnCompletedTrip() {
        // Real TripAuthorizationService (no admin bypass on requireMutableTrip,
        // per its own javadoc) so the lock is exercised end-to-end, not mocked away.
        TripAuthorizationService realAuth = new TripAuthorizationService(tripMemberRepository);
        ExpenseServiceImpl service = new ExpenseServiceImpl(
                expenseRepository, tripRepository, tripMemberRepository, userRepository, expenseMapper, realAuth);

        User alice = user("alice@travelease.test");
        Trip trip = trip(alice, TravelerTripStatus.COMPLETED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(tripMemberRepository.existsByTripIdAndUserEmailAndMemberStatus(
                trip.getId(), alice.getEmail(), TripMemberStatus.ACCEPTED)).thenReturn(true);

        CreateExpenseRequest request = new CreateExpenseRequest(
                new BigDecimal("100.00"), "Food", "Dinner", LocalDate.now(),
                alice.getId(), List.of(alice.getId()), null);

        assertThatThrownBy(() -> service.createSharedExpense(trip.getId(), request, alice.getEmail()))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void createSharedExpenseIsRejectedOnCancelledTrip() {
        TripAuthorizationService realAuth = new TripAuthorizationService(tripMemberRepository);
        ExpenseServiceImpl service = new ExpenseServiceImpl(
                expenseRepository, tripRepository, tripMemberRepository, userRepository, expenseMapper, realAuth);

        User alice = user("alice@travelease.test");
        Trip trip = trip(alice, TravelerTripStatus.CANCELLED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(tripMemberRepository.existsByTripIdAndUserEmailAndMemberStatus(
                trip.getId(), alice.getEmail(), TripMemberStatus.ACCEPTED)).thenReturn(true);

        CreateExpenseRequest request = new CreateExpenseRequest(
                new BigDecimal("100.00"), "Food", "Dinner", LocalDate.now(),
                alice.getId(), List.of(alice.getId()), null);

        assertThatThrownBy(() -> service.createSharedExpense(trip.getId(), request, alice.getEmail()))
                .isInstanceOf(InvalidRequestException.class);
    }
}
