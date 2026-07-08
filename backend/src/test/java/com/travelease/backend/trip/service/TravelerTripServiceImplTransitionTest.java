package com.travelease.backend.trip.service;

import com.travelease.backend.auth.entity.Role;
import com.travelease.backend.auth.entity.User;
import com.travelease.backend.auth.repository.UserRepository;
import com.travelease.backend.shared.exception.InvalidRequestException;
import com.travelease.backend.trip.dto.TripStatusTransitionRequest;
import com.travelease.backend.trip.dto.TripResponse;
import com.travelease.backend.trip.entity.Trip;
import com.travelease.backend.trip.entity.TravelerTripStatus;
import com.travelease.backend.trip.mapper.TravelerTripMapper;
import com.travelease.backend.trip.repository.TripMemberRepository;
import com.travelease.backend.trip.repository.TripRepository;
import com.travelease.backend.trip.security.TripAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TravelerTripServiceImplTransitionTest {

    @Mock
    private TripRepository tripRepository;
    @Mock
    private TripMemberRepository tripMemberRepository;
    @Mock
    private UserRepository userRepository;

    private TravelerTripServiceImpl tripService;

    @BeforeEach
    void setUp() {
        tripService = new TravelerTripServiceImpl(
                tripRepository, tripMemberRepository, userRepository,
                new TripAuthorizationService(tripMemberRepository), new TravelerTripMapper());
    }

    private User user(String email, Role role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setName(email);
        user.setRole(role);
        return user;
    }

    private Trip trip(User organizer, TravelerTripStatus status, LocalDate startDate) {
        Trip trip = new Trip();
        trip.setId(UUID.randomUUID());
        trip.setTripName("Goa Trip");
        trip.setOrganizer(organizer);
        trip.setSourceLocation("Mumbai");
        trip.setDestinationId(1);
        trip.setBudgetAmount(new BigDecimal("1000.00"));
        trip.setCategoryId(1);
        trip.setStartDate(startDate);
        trip.setEndDate(startDate.plusDays(5));
        trip.setStatus(status);
        return trip;
    }

    private void stub(Trip trip, User user) {
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // --- valid transitions ---

    @Test
    void planningToConfirmedIsValid() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice, TravelerTripStatus.PLANNING, LocalDate.now().plusDays(10));
        stub(trip, alice);

        TripResponse response = tripService.transitionStatus(
                trip.getId(), new TripStatusTransitionRequest(TravelerTripStatus.CONFIRMED), alice.getEmail());

        assertThat(response.status()).isEqualTo(TravelerTripStatus.CONFIRMED);
    }

    @Test
    void confirmedToOngoingIsValidWhenStartDateHasArrived() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice, TravelerTripStatus.CONFIRMED, LocalDate.now().minusDays(1));
        stub(trip, alice);

        TripResponse response = tripService.transitionStatus(
                trip.getId(), new TripStatusTransitionRequest(TravelerTripStatus.ONGOING), alice.getEmail());

        assertThat(response.status()).isEqualTo(TravelerTripStatus.ONGOING);
    }

    @Test
    void confirmedToOngoingIsRejectedBeforeStartDate() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice, TravelerTripStatus.CONFIRMED, LocalDate.now().plusDays(3));
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));

        assertThatThrownBy(() -> tripService.transitionStatus(
                trip.getId(), new TripStatusTransitionRequest(TravelerTripStatus.ONGOING), alice.getEmail()))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void ongoingToCompletedIsValid() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice, TravelerTripStatus.ONGOING, LocalDate.now().minusDays(2));
        stub(trip, alice);

        TripResponse response = tripService.transitionStatus(
                trip.getId(), new TripStatusTransitionRequest(TravelerTripStatus.COMPLETED), alice.getEmail());

        assertThat(response.status()).isEqualTo(TravelerTripStatus.COMPLETED);
    }

    @Test
    void planningToCancelledIsValid() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice, TravelerTripStatus.PLANNING, LocalDate.now().plusDays(10));
        stub(trip, alice);

        TripResponse response = tripService.transitionStatus(
                trip.getId(), new TripStatusTransitionRequest(TravelerTripStatus.CANCELLED), alice.getEmail());

        assertThat(response.status()).isEqualTo(TravelerTripStatus.CANCELLED);
    }

    @Test
    void confirmedToCancelledIsValid() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice, TravelerTripStatus.CONFIRMED, LocalDate.now().plusDays(10));
        stub(trip, alice);

        TripResponse response = tripService.transitionStatus(
                trip.getId(), new TripStatusTransitionRequest(TravelerTripStatus.CANCELLED), alice.getEmail());

        assertThat(response.status()).isEqualTo(TravelerTripStatus.CANCELLED);
    }

    // --- invalid transitions ---

    @Test
    void planningToOngoingSkipIsRejected() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice, TravelerTripStatus.PLANNING, LocalDate.now().minusDays(1));
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));

        assertThatThrownBy(() -> tripService.transitionStatus(
                trip.getId(), new TripStatusTransitionRequest(TravelerTripStatus.ONGOING), alice.getEmail()))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void planningToCompletedSkipIsRejected() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice, TravelerTripStatus.PLANNING, LocalDate.now().plusDays(10));
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));

        assertThatThrownBy(() -> tripService.transitionStatus(
                trip.getId(), new TripStatusTransitionRequest(TravelerTripStatus.COMPLETED), alice.getEmail()))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void completedTripHasNoValidTransitions() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice, TravelerTripStatus.COMPLETED, LocalDate.now().minusDays(10));
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));

        for (TravelerTripStatus target : TravelerTripStatus.values()) {
            assertThatThrownBy(() -> tripService.transitionStatus(
                    trip.getId(), new TripStatusTransitionRequest(target), alice.getEmail()))
                    .isInstanceOf(InvalidRequestException.class);
        }
    }

    @Test
    void cancelledTripHasNoValidTransitions() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice, TravelerTripStatus.CANCELLED, LocalDate.now().minusDays(10));
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));

        for (TravelerTripStatus target : TravelerTripStatus.values()) {
            assertThatThrownBy(() -> tripService.transitionStatus(
                    trip.getId(), new TripStatusTransitionRequest(target), alice.getEmail()))
                    .isInstanceOf(InvalidRequestException.class);
        }
    }

    @Test
    void sameStatusTransitionIsRejectedNotIdempotent() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice, TravelerTripStatus.PLANNING, LocalDate.now().plusDays(10));
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));

        assertThatThrownBy(() -> tripService.transitionStatus(
                trip.getId(), new TripStatusTransitionRequest(TravelerTripStatus.PLANNING), alice.getEmail()))
                .isInstanceOf(InvalidRequestException.class);
    }

    // --- authorization ---

    @Test
    void acceptedMemberCannotTransitionStatus() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice, TravelerTripStatus.PLANNING, LocalDate.now().plusDays(10));
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(bob.getEmail())).thenReturn(Optional.of(bob));

        // requireOrganizer checks organizer identity only (isOrganizer), never
        // membership status, so no TripMember stub is needed here - being an
        // ACCEPTED member is exactly why this must still be denied.
        assertThatThrownBy(() -> tripService.transitionStatus(
                trip.getId(), new TripStatusTransitionRequest(TravelerTripStatus.CONFIRMED), bob.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unrelatedTravelerCannotTransitionStatus() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User eve = user("eve@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice, TravelerTripStatus.PLANNING, LocalDate.now().plusDays(10));
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(eve.getEmail())).thenReturn(Optional.of(eve));

        assertThatThrownBy(() -> tripService.transitionStatus(
                trip.getId(), new TripStatusTransitionRequest(TravelerTripStatus.CONFIRMED), eve.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void adminCanTransitionAnyTripButObeysSameGraph() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User admin = user("admin@travelease.test", Role.ROLE_ADMIN);
        Trip trip = trip(alice, TravelerTripStatus.COMPLETED, LocalDate.now().minusDays(10));
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));

        // Admin bypasses the organizer-identity check but not the transition graph.
        assertThatThrownBy(() -> tripService.transitionStatus(
                trip.getId(), new TripStatusTransitionRequest(TravelerTripStatus.ONGOING), admin.getEmail()))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void adminCanTransitionAnotherOrganizersTrip() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User admin = user("admin@travelease.test", Role.ROLE_ADMIN);
        Trip trip = trip(alice, TravelerTripStatus.PLANNING, LocalDate.now().plusDays(10));
        stub(trip, admin);

        TripResponse response = tripService.transitionStatus(
                trip.getId(), new TripStatusTransitionRequest(TravelerTripStatus.CONFIRMED), admin.getEmail());

        assertThat(response.status()).isEqualTo(TravelerTripStatus.CONFIRMED);
    }
}
