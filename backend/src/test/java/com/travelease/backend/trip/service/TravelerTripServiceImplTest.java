package com.travelease.backend.trip.service;

import com.travelease.backend.auth.entity.Role;
import com.travelease.backend.auth.entity.User;
import com.travelease.backend.auth.repository.UserRepository;
import com.travelease.backend.shared.exception.InvalidRequestException;
import com.travelease.backend.trip.dto.AddTripMemberRequest;
import com.travelease.backend.trip.dto.CreateTripRequest;
import com.travelease.backend.trip.dto.PendingInvitationResponse;
import com.travelease.backend.trip.dto.TripMemberResponse;
import com.travelease.backend.trip.dto.TripResponse;
import com.travelease.backend.trip.dto.UpdateTripRequest;
import com.travelease.backend.trip.entity.Trip;
import com.travelease.backend.trip.entity.TravelerTripStatus;
import com.travelease.backend.trip.entity.TripMember;
import com.travelease.backend.trip.entity.TripMemberStatus;
import com.travelease.backend.trip.mapper.TravelerTripMapper;
import com.travelease.backend.trip.repository.TripMemberRepository;
import com.travelease.backend.trip.repository.TripRepository;
import com.travelease.backend.trip.security.TripAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TravelerTripServiceImplTest {

    @Mock
    private TripRepository tripRepository;
    @Mock
    private TripMemberRepository tripMemberRepository;
    @Mock
    private UserRepository userRepository;

    private TripAuthorizationService tripAuthorizationService;
    private TravelerTripServiceImpl tripService;

    @BeforeEach
    void setUp() {
        tripAuthorizationService = new TripAuthorizationService(tripMemberRepository);
        tripService = new TravelerTripServiceImpl(
                tripRepository, tripMemberRepository, userRepository,
                tripAuthorizationService, new TravelerTripMapper());
    }

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

    private TripMember membership(UUID id, Trip trip, User user, TripMemberStatus status) {
        TripMember member = new TripMember();
        member.setId(id);
        member.setTrip(trip);
        member.setUser(user);
        member.setMemberStatus(status);
        return member;
    }

    // --- creation (requirements 1, 2, 3) ---

    @Test
    void createTripRequestHasNoClientControlledOrganizerField() {
        assertThat(CreateTripRequest.class.getRecordComponents())
                .extracting(RecordComponent::getName)
                .doesNotContain("organizerId", "organizer");
    }

    @Test
    void createTripAssignsAuthenticatedCallerAsOrganizerAndSeedsOrganizerMembership() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        when(userRepository.findByEmail("alice@travelease.test")).thenReturn(Optional.of(alice));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tripMemberRepository.save(any(TripMember.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateTripRequest request = new CreateTripRequest(
                "Goa Trip", "Mumbai", 1, new BigDecimal("1000.00"), 1,
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(10));

        TripResponse response = tripService.createTrip(request, "alice@travelease.test");

        assertThat(response.organizer().userId()).isEqualTo(alice.getId());
        assertThat(response.viewerRole()).isEqualTo("ORGANIZER");
        assertThat(response.status()).isEqualTo(TravelerTripStatus.PLANNING);

        ArgumentCaptor<TripMember> captor = ArgumentCaptor.forClass(TripMember.class);
        verify(tripMemberRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(alice);
        assertThat(captor.getValue().getMemberStatus()).isEqualTo(TripMemberStatus.ACCEPTED);
    }

    @Test
    void createTripRejectsStartDateAfterEndDate() {
        CreateTripRequest request = new CreateTripRequest(
                "Goa Trip", "Mumbai", 1, new BigDecimal("1000.00"), 1,
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(5));

        assertThatThrownBy(() -> tripService.createTrip(request, "alice@travelease.test"))
                .isInstanceOf(InvalidRequestException.class);
        verify(tripRepository, never()).save(any());
    }

    // --- update authorization (requirements 8, 9) ---

    @Test
    void organizerCanUpdateTripMetadataWithoutAffectingStatus() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateTripRequest request = new UpdateTripRequest(
                "Updated Trip", "Pune", 2, new BigDecimal("2000.00"), 2,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

        TripResponse response = tripService.updateTrip(trip.getId(), request, alice.getEmail());

        assertThat(response.tripName()).isEqualTo("Updated Trip");
        // Generic metadata update has no status field at all - lifecycle
        // transitions have exactly one path, transitionStatus.
        assertThat(response.status()).isEqualTo(TravelerTripStatus.PLANNING);
    }

    @Test
    void nonOrganizerMemberCannotUpdateTrip() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(bob.getEmail())).thenReturn(Optional.of(bob));

        UpdateTripRequest request = new UpdateTripRequest(
                "Updated Trip", "Pune", 2, new BigDecimal("2000.00"), 2,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

        assertThatThrownBy(() -> tripService.updateTrip(trip.getId(), request, bob.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
        verify(tripRepository, never()).save(any());
    }

    // --- view authorization (requirement 10, plus invitation-lifecycle requirements 27-29) ---

    @Test
    void unrelatedTravelerCannotViewTrip() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User eve = user("eve@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(eve.getEmail())).thenReturn(Optional.of(eve));
        when(tripMemberRepository.existsByTripIdAndUserIdAndMemberStatus(trip.getId(), eve.getId(), TripMemberStatus.ACCEPTED))
                .thenReturn(false);

        assertThatThrownBy(() -> tripService.getTripById(trip.getId(), eve.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void invitedUserCannotAccessTripDetailBeforeAcceptance() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(bob.getEmail())).thenReturn(Optional.of(bob));
        when(tripMemberRepository.existsByTripIdAndUserIdAndMemberStatus(trip.getId(), bob.getId(), TripMemberStatus.ACCEPTED))
                .thenReturn(false);

        assertThatThrownBy(() -> tripService.getTripById(trip.getId(), bob.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void invitedUserCanAccessTripDetailAfterAcceptance() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(bob.getEmail())).thenReturn(Optional.of(bob));
        when(tripMemberRepository.existsByTripIdAndUserIdAndMemberStatus(trip.getId(), bob.getId(), TripMemberStatus.ACCEPTED))
                .thenReturn(true);

        TripResponse response = tripService.getTripById(trip.getId(), bob.getEmail());
        assertThat(response.viewerRole()).isEqualTo("MEMBER");
    }

    @Test
    void rejectedUserCannotAccessTripDetail() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User cara = user("cara@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(cara.getEmail())).thenReturn(Optional.of(cara));
        // A REJECTED row exists, but the ACCEPTED-only existence check correctly returns false for it.
        when(tripMemberRepository.existsByTripIdAndUserIdAndMemberStatus(trip.getId(), cara.getId(), TripMemberStatus.ACCEPTED))
                .thenReturn(false);

        assertThatThrownBy(() -> tripService.getTripById(trip.getId(), cara.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    // --- invitation creation (requirements 1-7 of the invitation lifecycle) ---

    @Test
    void organizerInvitationCreatesInvitedTripMemberWithoutJoinedDate() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));
        when(userRepository.findByEmail(bob.getEmail())).thenReturn(Optional.of(bob));
        when(tripMemberRepository.findByTripIdAndUserId(trip.getId(), bob.getId())).thenReturn(Optional.empty());
        when(tripMemberRepository.save(any(TripMember.class))).thenAnswer(inv -> inv.getArgument(0));

        TripMemberResponse response = tripService.addTripMember(
                trip.getId(), new AddTripMemberRequest(bob.getEmail()), alice.getEmail());

        assertThat(response.email()).isEqualTo(bob.getEmail());
        assertThat(response.memberStatus()).isEqualTo(TripMemberStatus.INVITED);
        assertThat(response.joinedDate()).isNull();
    }

    @Test
    void organizerCannotInviteThemselves() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));

        assertThatThrownBy(() -> tripService.addTripMember(
                trip.getId(), new AddTripMemberRequest(alice.getEmail()), alice.getEmail()))
                .isInstanceOf(InvalidRequestException.class);
        verify(tripMemberRepository, never()).save(any());
    }

    @Test
    void nonOrganizerMemberCannotInviteParticipant() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(bob.getEmail())).thenReturn(Optional.of(bob));

        assertThatThrownBy(() -> tripService.addTripMember(
                trip.getId(), new AddTripMemberRequest("cara@travelease.test"), bob.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unrelatedTravelerCannotInviteParticipant() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User eve = user("eve@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(eve.getEmail())).thenReturn(Optional.of(eve));

        assertThatThrownBy(() -> tripService.addTripMember(
                trip.getId(), new AddTripMemberRequest("cara@travelease.test"), eve.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void acceptedMemberCannotBeInvitedAgain() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        TripMember existing = membership(UUID.randomUUID(), trip, bob, TripMemberStatus.ACCEPTED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));
        when(userRepository.findByEmail(bob.getEmail())).thenReturn(Optional.of(bob));
        when(tripMemberRepository.findByTripIdAndUserId(trip.getId(), bob.getId())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> tripService.addTripMember(
                trip.getId(), new AddTripMemberRequest(bob.getEmail()), alice.getEmail()))
                .isInstanceOf(InvalidRequestException.class);
        verify(tripMemberRepository, never()).save(any());
    }

    @Test
    void invitedMemberCannotBeInvitedAgain() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        TripMember existing = membership(UUID.randomUUID(), trip, bob, TripMemberStatus.INVITED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));
        when(userRepository.findByEmail(bob.getEmail())).thenReturn(Optional.of(bob));
        when(tripMemberRepository.findByTripIdAndUserId(trip.getId(), bob.getId())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> tripService.addTripMember(
                trip.getId(), new AddTripMemberRequest(bob.getEmail()), alice.getEmail()))
                .isInstanceOf(InvalidRequestException.class);
        verify(tripMemberRepository, never()).save(any());
    }

    @Test
    void rejectedMemberIsReInvitedByReusingTheSameRow() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User cara = user("cara@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        TripMember existing = membership(UUID.randomUUID(), trip, cara, TripMemberStatus.REJECTED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));
        when(userRepository.findByEmail(cara.getEmail())).thenReturn(Optional.of(cara));
        when(tripMemberRepository.findByTripIdAndUserId(trip.getId(), cara.getId())).thenReturn(Optional.of(existing));
        when(tripMemberRepository.save(any(TripMember.class))).thenAnswer(inv -> inv.getArgument(0));

        TripMemberResponse response = tripService.addTripMember(
                trip.getId(), new AddTripMemberRequest(cara.getEmail()), alice.getEmail());

        // Same row reused (same tripMemberId), not a new one - required by the
        // trip_id+user_id unique constraint.
        assertThat(response.tripMemberId()).isEqualTo(existing.getId());
        assertThat(response.memberStatus()).isEqualTo(TripMemberStatus.INVITED);
        assertThat(response.joinedDate()).isNull();
    }

    @Test
    void nonTravelerCannotBeInvitedAsParticipant() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User provider = user("provider@travelease.test", Role.ROLE_PROVIDER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));

        assertThatThrownBy(() -> tripService.addTripMember(
                trip.getId(), new AddTripMemberRequest(provider.getEmail()), alice.getEmail()))
                .isInstanceOf(InvalidRequestException.class);
        verify(tripMemberRepository, never()).save(any());
    }

    // --- accept invitation (requirements 12-17, 20) ---

    @Test
    void invitedUserCanAcceptOwnInvitation() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        TripMember invitation = membership(UUID.randomUUID(), trip, bob, TripMemberStatus.INVITED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(bob.getEmail())).thenReturn(Optional.of(bob));
        when(tripMemberRepository.findByIdAndTripId(invitation.getId(), trip.getId())).thenReturn(Optional.of(invitation));
        when(tripMemberRepository.save(any(TripMember.class))).thenAnswer(inv -> inv.getArgument(0));

        TripMemberResponse response = tripService.acceptTripMemberInvitation(trip.getId(), invitation.getId(), bob.getEmail());

        assertThat(response.memberStatus()).isEqualTo(TripMemberStatus.ACCEPTED);
        assertThat(response.joinedDate()).isNotNull();
    }

    @Test
    void organizerCannotAcceptOnBehalfOfInvitee() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        TripMember invitation = membership(UUID.randomUUID(), trip, bob, TripMemberStatus.INVITED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));
        when(tripMemberRepository.findByIdAndTripId(invitation.getId(), trip.getId())).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> tripService.acceptTripMemberInvitation(trip.getId(), invitation.getId(), alice.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
        verify(tripMemberRepository, never()).save(any());
    }

    @Test
    void anotherTravelerCannotAcceptSomeoneElsesInvitation() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        User eve = user("eve@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        TripMember invitation = membership(UUID.randomUUID(), trip, bob, TripMemberStatus.INVITED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(eve.getEmail())).thenReturn(Optional.of(eve));
        when(tripMemberRepository.findByIdAndTripId(invitation.getId(), trip.getId())).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> tripService.acceptTripMemberInvitation(trip.getId(), invitation.getId(), eve.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
        verify(tripMemberRepository, never()).save(any());
    }

    @Test
    void rejectedInvitationCannotBeDirectlyAccepted() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User cara = user("cara@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        TripMember rejected = membership(UUID.randomUUID(), trip, cara, TripMemberStatus.REJECTED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(cara.getEmail())).thenReturn(Optional.of(cara));
        when(tripMemberRepository.findByIdAndTripId(rejected.getId(), trip.getId())).thenReturn(Optional.of(rejected));

        assertThatThrownBy(() -> tripService.acceptTripMemberInvitation(trip.getId(), rejected.getId(), cara.getEmail()))
                .isInstanceOf(InvalidRequestException.class);
        verify(tripMemberRepository, never()).save(any());
    }

    // --- reject invitation (requirements 18-19) ---

    @Test
    void invitedUserCanRejectOwnInvitationAndJoinedDateStaysNull() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User cara = user("cara@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        TripMember invitation = membership(UUID.randomUUID(), trip, cara, TripMemberStatus.INVITED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(cara.getEmail())).thenReturn(Optional.of(cara));
        when(tripMemberRepository.findByIdAndTripId(invitation.getId(), trip.getId())).thenReturn(Optional.of(invitation));
        when(tripMemberRepository.save(any(TripMember.class))).thenAnswer(inv -> inv.getArgument(0));

        TripMemberResponse response = tripService.rejectTripMemberInvitation(trip.getId(), invitation.getId(), cara.getEmail());

        assertThat(response.memberStatus()).isEqualTo(TripMemberStatus.REJECTED);
        assertThat(response.joinedDate()).isNull();
        verify(tripMemberRepository, never()).delete(any());
    }

    // --- pending invitation discovery (requirements 25-26) ---

    @Test
    void pendingInvitationDiscoveryReturnsOnlyTheCallersOwnInvitations() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        TripMember invitation = membership(UUID.randomUUID(), trip, bob, TripMemberStatus.INVITED);
        when(userRepository.findByEmail(bob.getEmail())).thenReturn(Optional.of(bob));
        when(tripMemberRepository.findByUserIdAndMemberStatus(bob.getId(), TripMemberStatus.INVITED))
                .thenReturn(List.of(invitation));

        List<PendingInvitationResponse> invitations = tripService.getMyPendingInvitations(bob.getEmail());

        assertThat(invitations).hasSize(1);
        PendingInvitationResponse response = invitations.get(0);
        assertThat(response.tripMemberId()).isEqualTo(invitation.getId());
        assertThat(response.tripId()).isEqualTo(trip.getId());
        assertThat(response.organizer().userId()).isEqualTo(alice.getId());
        assertThat(response.memberStatus()).isEqualTo(TripMemberStatus.INVITED);
    }

    // --- participant removal (requirements 30-31) ---

    @Test
    void organizerCannotRemoveThemselvesThroughMemberRemoval() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        TripMember organizerMembership = membership(UUID.randomUUID(), trip, alice, TripMemberStatus.ACCEPTED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));
        when(tripMemberRepository.findByIdAndTripId(organizerMembership.getId(), trip.getId()))
                .thenReturn(Optional.of(organizerMembership));

        assertThatThrownBy(() -> tripService.removeTripMember(trip.getId(), organizerMembership.getId(), alice.getEmail()))
                .isInstanceOf(InvalidRequestException.class);
        verify(tripMemberRepository, never()).delete(any());
    }

    @Test
    void organizerCanCancelPendingInvitationByRemovingIt() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        TripMember bobInvitation = membership(UUID.randomUUID(), trip, bob, TripMemberStatus.INVITED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));
        when(tripMemberRepository.findByIdAndTripId(bobInvitation.getId(), trip.getId()))
                .thenReturn(Optional.of(bobInvitation));

        tripService.removeTripMember(trip.getId(), bobInvitation.getId(), alice.getEmail());

        verify(tripMemberRepository).delete(bobInvitation);
    }

    @Test
    void organizerCanRemovePermittedParticipant() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        TripMember bobMembership = membership(UUID.randomUUID(), trip, bob, TripMemberStatus.ACCEPTED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));
        when(tripMemberRepository.findByIdAndTripId(bobMembership.getId(), trip.getId()))
                .thenReturn(Optional.of(bobMembership));

        tripService.removeTripMember(trip.getId(), bobMembership.getId(), alice.getEmail());

        verify(tripMemberRepository).delete(bobMembership);
    }

    // --- my trips (requirements 21-24) ---

    @Test
    void myTripsReturnsOrganizedAndAcceptedParticipatingTripsOnly() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip organizedTrip = trip(alice);
        Trip participatingTrip = trip(bob);
        TripMember aliceMembershipInBobsTrip = membership(UUID.randomUUID(), participatingTrip, alice, TripMemberStatus.ACCEPTED);

        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));
        when(tripRepository.findByOrganizerId(alice.getId())).thenReturn(List.of(organizedTrip));
        when(tripMemberRepository.findByUserIdAndMemberStatus(alice.getId(), TripMemberStatus.ACCEPTED))
                .thenReturn(List.of(aliceMembershipInBobsTrip));
        when(tripMemberRepository.existsByTripIdAndUserIdAndMemberStatus(
                participatingTrip.getId(), alice.getId(), TripMemberStatus.ACCEPTED)).thenReturn(true);

        List<TripResponse> trips = tripService.getMyTrips(alice.getEmail());

        // Only the organized trip and the trip alice is an ACCEPTED participant of are
        // returned - findByUserIdAndMemberStatus(..., ACCEPTED) structurally excludes
        // any INVITED/REJECTED relationship from ever appearing here.
        assertThat(trips).hasSize(2);
        assertThat(trips).extracting(TripResponse::tripId)
                .containsExactlyInAnyOrder(organizedTrip.getId(), participatingTrip.getId());

        TripResponse organizedResponse = trips.stream()
                .filter(t -> t.tripId().equals(organizedTrip.getId())).findFirst().orElseThrow();
        TripResponse participatingResponse = trips.stream()
                .filter(t -> t.tripId().equals(participatingTrip.getId())).findFirst().orElseThrow();

        assertThat(organizedResponse.viewerRole()).isEqualTo("ORGANIZER");
        assertThat(participatingResponse.viewerRole()).isEqualTo("MEMBER");
    }

    // --- Traveler Trip lifecycle lock on membership mutation (completed/cancelled trips) ---

    @Test
    void cannotInviteMemberToCompletedTrip() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        trip.setStatus(TravelerTripStatus.COMPLETED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));

        assertThatThrownBy(() -> tripService.addTripMember(
                trip.getId(), new AddTripMemberRequest("dan@travelease.test"), alice.getEmail()))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void cannotAcceptInvitationIntoACancelledTrip() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        trip.setStatus(TravelerTripStatus.CANCELLED);
        TripMember invitation = membership(UUID.randomUUID(), trip, bob, TripMemberStatus.INVITED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(bob.getEmail())).thenReturn(Optional.of(bob));
        when(tripMemberRepository.findByIdAndTripId(invitation.getId(), trip.getId())).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> tripService.acceptTripMemberInvitation(trip.getId(), invitation.getId(), bob.getEmail()))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void cannotRemoveMemberFromCompletedTrip() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        trip.setStatus(TravelerTripStatus.COMPLETED);
        TripMember bobMembership = membership(UUID.randomUUID(), trip, bob, TripMemberStatus.ACCEPTED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));

        assertThatThrownBy(() -> tripService.removeTripMember(trip.getId(), bobMembership.getId(), alice.getEmail()))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void tripDetailStillReadableAfterCompletion() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        trip.setStatus(TravelerTripStatus.COMPLETED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));

        TripResponse response = tripService.getTripById(trip.getId(), alice.getEmail());

        assertThat(response.status()).isEqualTo(TravelerTripStatus.COMPLETED);
    }
}
