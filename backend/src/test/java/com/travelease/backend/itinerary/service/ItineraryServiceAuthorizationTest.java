package com.travelease.backend.itinerary.service;

import com.travelease.backend.auth.entity.Role;
import com.travelease.backend.auth.entity.User;
import com.travelease.backend.auth.repository.UserRepository;
import com.travelease.backend.itinerary.dto.ItineraryRequest;
import com.travelease.backend.itinerary.dto.ItineraryResponse;
import com.travelease.backend.itinerary.entity.Itinerary;
import com.travelease.backend.itinerary.mapper.ItineraryMapper;
import com.travelease.backend.itinerary.repository.ItineraryRepository;
import com.travelease.backend.shared.exception.InvalidRequestException;
import com.travelease.backend.shared.exception.ResourceNotFoundException;
import com.travelease.backend.trip.entity.Trip;
import com.travelease.backend.trip.entity.TravelerTripStatus;
import com.travelease.backend.trip.repository.TripMemberRepository;
import com.travelease.backend.trip.repository.TripRepository;
import com.travelease.backend.trip.security.TripAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItineraryServiceAuthorizationTest {

    @Mock
    private ItineraryRepository itineraryRepository;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private TripMemberRepository tripMemberRepository;
    @Mock
    private UserRepository userRepository;

    private ItineraryService itineraryService;

    @BeforeEach
    void setUp() {
        itineraryService = new ItineraryService();
        ReflectionTestUtils.setField(itineraryService, "itineraryRepository", itineraryRepository);
        ReflectionTestUtils.setField(itineraryService, "itineraryMapper", new ItineraryMapper());
        ReflectionTestUtils.setField(itineraryService, "tripRepository", tripRepository);
        ReflectionTestUtils.setField(itineraryService, "tripAuthorizationService",
                new TripAuthorizationService(tripMemberRepository));
        ReflectionTestUtils.setField(itineraryService, "userRepository", userRepository);
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

    private Itinerary itemFor(Trip trip) {
        Itinerary item = new Itinerary();
        item.setItineraryId(UUID.randomUUID().toString());
        item.setTripId(trip.getId().toString());
        item.setActivityId("ACT-1");
        item.setStatus("Pending");
        return item;
    }

    private void stubAccepted(Trip trip, User user, boolean accepted) {
        when(tripMemberRepository.existsByTripIdAndUserIdAndMemberStatus(
                trip.getId(), user.getId(), com.travelease.backend.trip.entity.TripMemberStatus.ACCEPTED))
                .thenReturn(accepted);
    }

    // --- READ (1-7) ---

    @Test
    void organizerCanRead() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));
        when(itineraryRepository.findByTripId(trip.getId().toString())).thenReturn(List.of(itemFor(trip)));

        List<ItineraryResponse> result = itineraryService.getByTripId(trip.getId().toString(), alice.getEmail());

        assertThat(result).hasSize(1);
    }

    @Test
    void acceptedMemberCanRead() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(bob.getEmail())).thenReturn(Optional.of(bob));
        stubAccepted(trip, bob, true);
        when(itineraryRepository.findByTripId(trip.getId().toString())).thenReturn(List.of(itemFor(trip)));

        List<ItineraryResponse> result = itineraryService.getByTripId(trip.getId().toString(), bob.getEmail());

        assertThat(result).hasSize(1);
    }

    @Test
    void invitedUserCannotRead() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(bob.getEmail())).thenReturn(Optional.of(bob));
        stubAccepted(trip, bob, false); // INVITED - not yet ACCEPTED

        assertThatThrownBy(() -> itineraryService.getByTripId(trip.getId().toString(), bob.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectedUserCannotRead() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User cara = user("cara@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(cara.getEmail())).thenReturn(Optional.of(cara));
        stubAccepted(trip, cara, false); // REJECTED

        assertThatThrownBy(() -> itineraryService.getByTripId(trip.getId().toString(), cara.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unrelatedTravelerCannotRead() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User eve = user("eve@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(eve.getEmail())).thenReturn(Optional.of(eve));
        stubAccepted(trip, eve, false);

        assertThatThrownBy(() -> itineraryService.getByTripId(trip.getId().toString(), eve.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    // Note: ROLE_PROVIDER exclusion is enforced declaratively at the controller
    // via @PreAuthorize("hasAnyRole('TRAVELER','ADMIN')") - identical to how
    // TripController/BudgetController/etc. exclude PROVIDER in this codebase -
    // not a service-layer concern, so it is not re-tested here at the service level.

    @Test
    void adminCanReadWithoutBeingATripMember() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User admin = user("admin@travelease.test", Role.ROLE_ADMIN);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(itineraryRepository.findByTripId(trip.getId().toString())).thenReturn(List.of(itemFor(trip)));

        List<ItineraryResponse> result = itineraryService.getByTripId(trip.getId().toString(), admin.getEmail());

        assertThat(result).hasSize(1);
    }

    // --- CREATE (8-12) ---

    @Test
    void organizerCanCreate() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));
        when(itineraryRepository.save(any(Itinerary.class))).thenAnswer(inv -> inv.getArgument(0));

        ItineraryRequest request = new ItineraryRequest();
        request.setTripId(trip.getId().toString());
        request.setActivityId("ACT-1");

        ItineraryResponse response = itineraryService.addItem(request, alice.getEmail());

        assertThat(response.getTripId()).isEqualTo(trip.getId().toString());
        assertThat(response.getStatus()).isEqualTo("Pending");
    }

    @Test
    void acceptedMemberCanCreate() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(bob.getEmail())).thenReturn(Optional.of(bob));
        stubAccepted(trip, bob, true);
        when(itineraryRepository.save(any(Itinerary.class))).thenAnswer(inv -> inv.getArgument(0));

        ItineraryRequest request = new ItineraryRequest();
        request.setTripId(trip.getId().toString());
        request.setActivityId("ACT-2");

        ItineraryResponse response = itineraryService.addItem(request, bob.getEmail());

        assertThat(response.getActivityId()).isEqualTo("ACT-2");
    }

    @Test
    void invitedUserCannotCreate() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(bob.getEmail())).thenReturn(Optional.of(bob));
        stubAccepted(trip, bob, false);

        ItineraryRequest request = new ItineraryRequest();
        request.setTripId(trip.getId().toString());

        assertThatThrownBy(() -> itineraryService.addItem(request, bob.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectedUserCannotCreate() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User cara = user("cara@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(cara.getEmail())).thenReturn(Optional.of(cara));
        stubAccepted(trip, cara, false);

        ItineraryRequest request = new ItineraryRequest();
        request.setTripId(trip.getId().toString());

        assertThatThrownBy(() -> itineraryService.addItem(request, cara.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unrelatedTravelerCannotCreate() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User eve = user("eve@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(eve.getEmail())).thenReturn(Optional.of(eve));
        stubAccepted(trip, eve, false);

        ItineraryRequest request = new ItineraryRequest();
        request.setTripId(trip.getId().toString());

        assertThatThrownBy(() -> itineraryService.addItem(request, eve.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    // --- UPDATE (13-17, including cross-trip mismatch = 15/50) ---

    @Test
    void organizerCanUpdate() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        Itinerary item = itemFor(trip);
        when(itineraryRepository.findById(item.getItineraryId())).thenReturn(Optional.of(item));
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));
        when(itineraryRepository.save(any(Itinerary.class))).thenAnswer(inv -> inv.getArgument(0));

        ItineraryRequest request = new ItineraryRequest();
        request.setStatus("Completed");

        ItineraryResponse response = itineraryService.updateItem(item.getItineraryId(), request, alice.getEmail());

        assertThat(response.getStatus()).isEqualTo("Completed");
    }

    @Test
    void acceptedMemberCanUpdate() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        Itinerary item = itemFor(trip);
        when(itineraryRepository.findById(item.getItineraryId())).thenReturn(Optional.of(item));
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(bob.getEmail())).thenReturn(Optional.of(bob));
        stubAccepted(trip, bob, true);
        when(itineraryRepository.save(any(Itinerary.class))).thenAnswer(inv -> inv.getArgument(0));

        ItineraryRequest request = new ItineraryRequest();
        request.setStatus("Completed");

        ItineraryResponse response = itineraryService.updateItem(item.getItineraryId(), request, bob.getEmail());

        assertThat(response.getStatus()).isEqualTo("Completed");
    }

    @Test
    void crossTripItemCannotBeUpdatedByUnrelatedTripsMember() {
        // Bob is an ACCEPTED member of Trip A, but the item actually belongs to
        // Trip B - the item's own persisted tripId is what determines
        // authorization, so Bob's membership in an unrelated Trip A is irrelevant.
        User aliceOrganizerOfTripA = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User caraOrganizerOfTripB = user("cara@travelease.test", Role.ROLE_TRAVELER);
        Trip tripA = trip(aliceOrganizerOfTripA);
        Trip tripB = trip(caraOrganizerOfTripB);
        Itinerary itemFromTripB = itemFor(tripB);

        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        when(itineraryRepository.findById(itemFromTripB.getItineraryId())).thenReturn(Optional.of(itemFromTripB));
        when(tripRepository.findById(tripB.getId())).thenReturn(Optional.of(tripB));
        when(userRepository.findByEmail(bob.getEmail())).thenReturn(Optional.of(bob));
        stubAccepted(tripB, bob, false); // bob is not a member of Trip B at all

        ItineraryRequest request = new ItineraryRequest();
        request.setStatus("Completed");

        assertThatThrownBy(() -> itineraryService.updateItem(itemFromTripB.getItineraryId(), request, bob.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void invitedUserCannotUpdate() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        Itinerary item = itemFor(trip);
        when(itineraryRepository.findById(item.getItineraryId())).thenReturn(Optional.of(item));
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(bob.getEmail())).thenReturn(Optional.of(bob));
        stubAccepted(trip, bob, false);

        ItineraryRequest request = new ItineraryRequest();
        request.setStatus("Completed");

        assertThatThrownBy(() -> itineraryService.updateItem(item.getItineraryId(), request, bob.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unrelatedTravelerCannotUpdate() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User eve = user("eve@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        Itinerary item = itemFor(trip);
        when(itineraryRepository.findById(item.getItineraryId())).thenReturn(Optional.of(item));
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(eve.getEmail())).thenReturn(Optional.of(eve));
        stubAccepted(trip, eve, false);

        ItineraryRequest request = new ItineraryRequest();
        request.setStatus("Completed");

        assertThatThrownBy(() -> itineraryService.updateItem(item.getItineraryId(), request, eve.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    // --- DELETE (18-23, organizer-only) ---

    @Test
    void organizerCanDelete() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        Itinerary item = itemFor(trip);
        when(itineraryRepository.findById(item.getItineraryId())).thenReturn(Optional.of(item));
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));

        itineraryService.deleteItem(item.getItineraryId(), alice.getEmail());
        // no exception - success
    }

    @Test
    void acceptedMemberCannotDelete() {
        // Deliberate design choice: deletion is organizer-only since it is
        // irreversible and items have no per-item creator field to scope a
        // narrower rule; an accepted (non-organizing) member is denied.
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        Itinerary item = itemFor(trip);
        when(itineraryRepository.findById(item.getItineraryId())).thenReturn(Optional.of(item));
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(bob.getEmail())).thenReturn(Optional.of(bob));

        assertThatThrownBy(() -> itineraryService.deleteItem(item.getItineraryId(), bob.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void crossTripItemCannotBeDeletedByUnrelatedTripsOrganizer() {
        User aliceOrganizerOfTripA = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User caraOrganizerOfTripB = user("cara@travelease.test", Role.ROLE_TRAVELER);
        Trip tripB = trip(caraOrganizerOfTripB);
        Itinerary itemFromTripB = itemFor(tripB);

        when(itineraryRepository.findById(itemFromTripB.getItineraryId())).thenReturn(Optional.of(itemFromTripB));
        when(tripRepository.findById(tripB.getId())).thenReturn(Optional.of(tripB));
        when(userRepository.findByEmail(aliceOrganizerOfTripA.getEmail())).thenReturn(Optional.of(aliceOrganizerOfTripA));

        assertThatThrownBy(() -> itineraryService.deleteItem(itemFromTripB.getItineraryId(), aliceOrganizerOfTripA.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void invitedUserCannotDelete() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        Itinerary item = itemFor(trip);
        when(itineraryRepository.findById(item.getItineraryId())).thenReturn(Optional.of(item));
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(bob.getEmail())).thenReturn(Optional.of(bob));

        assertThatThrownBy(() -> itineraryService.deleteItem(item.getItineraryId(), bob.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectedUserCannotDelete() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User cara = user("cara@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        Itinerary item = itemFor(trip);
        when(itineraryRepository.findById(item.getItineraryId())).thenReturn(Optional.of(item));
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(cara.getEmail())).thenReturn(Optional.of(cara));

        assertThatThrownBy(() -> itineraryService.deleteItem(item.getItineraryId(), cara.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unrelatedTravelerCannotDelete() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User eve = user("eve@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        Itinerary item = itemFor(trip);
        when(itineraryRepository.findById(item.getItineraryId())).thenReturn(Optional.of(item));
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(eve.getEmail())).thenReturn(Optional.of(eve));

        assertThatThrownBy(() -> itineraryService.deleteItem(item.getItineraryId(), eve.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    // --- PROGRESS (24-28) ---

    @Test
    void organizerCanViewProgress() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));
        when(itineraryRepository.findByTripId(trip.getId().toString())).thenReturn(List.of(itemFor(trip)));

        var progress = itineraryService.getProgress(trip.getId().toString(), alice.getEmail());

        assertThat(progress.get("totalActivities")).isEqualTo(1L);
    }

    @Test
    void acceptedMemberCanViewProgress() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(bob.getEmail())).thenReturn(Optional.of(bob));
        stubAccepted(trip, bob, true);
        when(itineraryRepository.findByTripId(trip.getId().toString())).thenReturn(List.of());

        var progress = itineraryService.getProgress(trip.getId().toString(), bob.getEmail());

        assertThat(progress.get("totalActivities")).isEqualTo(0L);
    }

    @Test
    void invitedUserCannotViewProgress() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(bob.getEmail())).thenReturn(Optional.of(bob));
        stubAccepted(trip, bob, false);

        assertThatThrownBy(() -> itineraryService.getProgress(trip.getId().toString(), bob.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectedUserCannotViewProgress() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User cara = user("cara@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(cara.getEmail())).thenReturn(Optional.of(cara));
        stubAccepted(trip, cara, false);

        assertThatThrownBy(() -> itineraryService.getProgress(trip.getId().toString(), cara.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unrelatedTravelerCannotViewProgress() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User eve = user("eve@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(eve.getEmail())).thenReturn(Optional.of(eve));
        stubAccepted(trip, eve, false);

        assertThatThrownBy(() -> itineraryService.getProgress(trip.getId().toString(), eve.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    // --- type/resource safety (48, 49) ---

    @Test
    void invalidTripIdFormatProducesControlled400() {
        assertThatThrownBy(() -> itineraryService.getByTripId("not-a-uuid", "alice@travelease.test"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void nonexistentTripProducesNotFound() {
        UUID randomTripId = UUID.randomUUID();
        when(tripRepository.findById(randomTripId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itineraryService.getByTripId(randomTripId.toString(), "alice@travelease.test"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- Traveler Trip lifecycle lock (completed/cancelled trips are read-only) ---

    @Test
    void cannotAddItemToCompletedTrip() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        trip.setStatus(TravelerTripStatus.COMPLETED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));

        ItineraryRequest request = new ItineraryRequest();
        request.setTripId(trip.getId().toString());
        request.setActivityId("ACT-1");

        assertThatThrownBy(() -> itineraryService.addItem(request, alice.getEmail()))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void cannotUpdateItemOnCancelledTrip() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        trip.setStatus(TravelerTripStatus.CANCELLED);
        Itinerary item = itemFor(trip);
        when(itineraryRepository.findById(item.getItineraryId())).thenReturn(Optional.of(item));
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));

        ItineraryRequest request = new ItineraryRequest();
        request.setStatus("Completed");

        assertThatThrownBy(() -> itineraryService.updateItem(item.getItineraryId(), request, alice.getEmail()))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void cannotDeleteItemOnCompletedTrip() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        trip.setStatus(TravelerTripStatus.COMPLETED);
        Itinerary item = itemFor(trip);
        when(itineraryRepository.findById(item.getItineraryId())).thenReturn(Optional.of(item));
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));

        assertThatThrownBy(() -> itineraryService.deleteItem(item.getItineraryId(), alice.getEmail()))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void readingItineraryOfCompletedTripStillWorks() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        trip.setStatus(TravelerTripStatus.COMPLETED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));
        when(itineraryRepository.findByTripId(trip.getId().toString())).thenReturn(List.of(itemFor(trip)));

        List<ItineraryResponse> result = itineraryService.getByTripId(trip.getId().toString(), alice.getEmail());

        assertThat(result).hasSize(1);
    }
}
