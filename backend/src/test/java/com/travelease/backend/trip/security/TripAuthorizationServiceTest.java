package com.travelease.backend.trip.security;

import com.travelease.backend.auth.entity.Role;
import com.travelease.backend.auth.entity.User;
import com.travelease.backend.shared.exception.InvalidRequestException;
import com.travelease.backend.trip.entity.Trip;
import com.travelease.backend.trip.entity.TravelerTripStatus;
import com.travelease.backend.trip.entity.TripMemberStatus;
import com.travelease.backend.trip.repository.TripMemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripAuthorizationServiceTest {

    @Mock
    private TripMemberRepository tripMemberRepository;

    @InjectMocks
    private TripAuthorizationService tripAuthorizationService;

    private User user(String email, Role role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setName(email);
        user.setRole(role);
        return user;
    }

    private Trip trip(User organizer) {
        Trip trip = new Trip();
        trip.setId(UUID.randomUUID());
        trip.setTripName("Goa Trip");
        trip.setOrganizer(organizer);
        trip.setSourceLocation("Mumbai");
        trip.setDestinationId(1);
        trip.setBudgetAmount(new BigDecimal("1000.00"));
        trip.setCategoryId(1);
        trip.setStartDate(LocalDate.now().plusDays(5));
        trip.setEndDate(LocalDate.now().plusDays(10));
        trip.setStatus(TravelerTripStatus.PLANNING);
        return trip;
    }

    @Test
    void organizerIsRecognizedByIsOrganizerAndIsMember() {
        User organizer = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(organizer);

        assertThat(tripAuthorizationService.isOrganizer(trip, organizer.getId())).isTrue();
        assertThat(tripAuthorizationService.isMember(trip, organizer.getId())).isTrue();
    }

    @Test
    void acceptedTripMemberIsRecognizedByIsMemberButNotIsOrganizer() {
        User organizer = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User member = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(organizer);
        when(tripMemberRepository.existsByTripIdAndUserIdAndMemberStatus(trip.getId(), member.getId(), TripMemberStatus.ACCEPTED))
                .thenReturn(true);

        assertThat(tripAuthorizationService.isMember(trip, member.getId())).isTrue();
        assertThat(tripAuthorizationService.isOrganizer(trip, member.getId())).isFalse();
    }

    @Test
    void invitedTripMemberIsNotYetAMember() {
        // An INVITED row exists, but existsByTripIdAndUserIdAndMemberStatus(..., ACCEPTED)
        // correctly returns false for it - isMember must not treat a pending invitation
        // as active membership.
        User organizer = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User invited = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(organizer);
        when(tripMemberRepository.existsByTripIdAndUserIdAndMemberStatus(trip.getId(), invited.getId(), TripMemberStatus.ACCEPTED))
                .thenReturn(false);

        assertThat(tripAuthorizationService.isMember(trip, invited.getId())).isFalse();
    }

    @Test
    void rejectedTripMemberIsNotAMember() {
        User organizer = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User rejected = user("cara@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(organizer);
        when(tripMemberRepository.existsByTripIdAndUserIdAndMemberStatus(trip.getId(), rejected.getId(), TripMemberStatus.ACCEPTED))
                .thenReturn(false);

        assertThat(tripAuthorizationService.isMember(trip, rejected.getId())).isFalse();
    }

    @Test
    void unrelatedTravelerIsNotAMember() {
        User organizer = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User unrelated = user("eve@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(organizer);
        when(tripMemberRepository.existsByTripIdAndUserIdAndMemberStatus(trip.getId(), unrelated.getId(), TripMemberStatus.ACCEPTED))
                .thenReturn(false);

        assertThat(tripAuthorizationService.isMember(trip, unrelated.getId())).isFalse();
    }

    @Test
    void requireOrganizerAllowsOrganizerAndDeniesEveryoneElse() {
        User organizer = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User member = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(organizer);

        assertThatCode(() -> tripAuthorizationService.requireOrganizer(trip, organizer.getId(), false))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> tripAuthorizationService.requireOrganizer(trip, member.getId(), false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void requireOrganizerAllowsAdminBypass() {
        User organizer = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(organizer);

        assertThatCode(() -> tripAuthorizationService.requireOrganizer(trip, UUID.randomUUID(), true))
                .doesNotThrowAnyException();
    }

    @Test
    void requireMemberAllowsMemberAndDeniesUnrelatedTraveler() {
        User organizer = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User member = user("bob@travelease.test", Role.ROLE_TRAVELER);
        User unrelated = user("eve@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(organizer);
        when(tripMemberRepository.existsByTripIdAndUserIdAndMemberStatus(trip.getId(), member.getId(), TripMemberStatus.ACCEPTED))
                .thenReturn(true);
        when(tripMemberRepository.existsByTripIdAndUserIdAndMemberStatus(trip.getId(), unrelated.getId(), TripMemberStatus.ACCEPTED))
                .thenReturn(false);

        assertThatCode(() -> tripAuthorizationService.requireMember(trip, member.getId(), false))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> tripAuthorizationService.requireMember(trip, unrelated.getId(), false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void requireMutableTripAllowsPlanningConfirmedAndOngoing() {
        User organizer = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(organizer);

        for (TravelerTripStatus status : new TravelerTripStatus[]{
                TravelerTripStatus.PLANNING, TravelerTripStatus.CONFIRMED, TravelerTripStatus.ONGOING}) {
            trip.setStatus(status);
            assertThatCode(() -> tripAuthorizationService.requireMutableTrip(trip)).doesNotThrowAnyException();
        }
    }

    @Test
    void requireMutableTripDeniesCompleted() {
        User organizer = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(organizer);
        trip.setStatus(TravelerTripStatus.COMPLETED);

        assertThatThrownBy(() -> tripAuthorizationService.requireMutableTrip(trip))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void requireMutableTripDeniesCancelled() {
        User organizer = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(organizer);
        trip.setStatus(TravelerTripStatus.CANCELLED);

        assertThatThrownBy(() -> tripAuthorizationService.requireMutableTrip(trip))
                .isInstanceOf(InvalidRequestException.class);
    }
}
