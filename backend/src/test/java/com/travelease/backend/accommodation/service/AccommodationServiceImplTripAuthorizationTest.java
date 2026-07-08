package com.travelease.backend.accommodation.service;

import com.travelease.backend.accommodation.dto.AccommodationSummaryResponse;
import com.travelease.backend.accommodation.dto.AttachHotelBookingRequest;
import com.travelease.backend.accommodation.dto.HotelBookingResponse;
import com.travelease.backend.accommodation.entity.Hotel;
import com.travelease.backend.accommodation.entity.HotelBooking;
import com.travelease.backend.accommodation.repository.HotelBookingRepository;
import com.travelease.backend.accommodation.repository.HotelRepository;
import com.travelease.backend.accommodation.repository.HotelReviewRepository;
import com.travelease.backend.accommodation.repository.RoomRepository;
import com.travelease.backend.auth.entity.Role;
import com.travelease.backend.auth.entity.User;
import com.travelease.backend.auth.repository.UserRepository;
import com.travelease.backend.busbooking.security.SecurityUtil;
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
class AccommodationServiceImplTripAuthorizationTest {

    @Mock
    private HotelRepository hotelRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private HotelBookingRepository bookingRepository;
    @Mock
    private HotelReviewRepository reviewRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private TripMemberRepository tripMemberRepository;
    @Mock
    private SecurityUtil securityUtil;

    private AccommodationServiceImpl accommodationService;

    @BeforeEach
    void setUp() {
        accommodationService = new AccommodationServiceImpl(
                hotelRepository, roomRepository, bookingRepository, reviewRepository, userRepository, tripRepository,
                new TripAuthorizationService(tripMemberRepository), securityUtil);
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

    private Hotel hotel() {
        Hotel hotel = new Hotel();
        hotel.setDestinationId(1);
        hotel.setHotelName("Test Hotel");
        hotel.setAddress("Somewhere");
        hotel.setPricePerNight(new BigDecimal("100.00"));
        hotel.setStatus("ACTIVE");
        return hotel;
    }

    private HotelBooking bookingOwnedBy(User owner) {
        HotelBooking booking = new HotelBooking();
        booking.setHotel(hotel());
        booking.setBookedBy(owner);
        booking.setCheckInDate(LocalDate.now().plusDays(1));
        booking.setCheckOutDate(LocalDate.now().plusDays(3));
        booking.setRoomType("DELUXE");
        booking.setTotalAmount(new BigDecimal("200.00"));
        booking.setBookingStatus("CONFIRMED");
        return booking;
    }

    private void stubAccepted(Trip trip, User user, boolean accepted) {
        when(tripMemberRepository.existsByTripIdAndUserIdAndMemberStatus(
                trip.getId(), user.getId(), com.travelease.backend.trip.entity.TripMemberStatus.ACCEPTED))
                .thenReturn(accepted);
    }

    // --- Accommodation summary (29-33) ---

    @Test
    void organizerCanViewSummary() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));
        when(bookingRepository.findByTripId(trip.getId())).thenReturn(List.of());

        AccommodationSummaryResponse response = accommodationService.getAccommodationSummary(trip.getId(), alice.getEmail());

        assertThat(response.tripId()).isEqualTo(trip.getId());
    }

    @Test
    void acceptedMemberCanViewSummary() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(bob.getEmail())).thenReturn(Optional.of(bob));
        stubAccepted(trip, bob, true);
        when(bookingRepository.findByTripId(trip.getId())).thenReturn(List.of());

        AccommodationSummaryResponse response = accommodationService.getAccommodationSummary(trip.getId(), bob.getEmail());

        assertThat(response.tripId()).isEqualTo(trip.getId());
    }

    @Test
    void invitedUserCannotViewSummary() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(bob.getEmail())).thenReturn(Optional.of(bob));
        stubAccepted(trip, bob, false);

        assertThatThrownBy(() -> accommodationService.getAccommodationSummary(trip.getId(), bob.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectedUserCannotViewSummary() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User cara = user("cara@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(cara.getEmail())).thenReturn(Optional.of(cara));
        stubAccepted(trip, cara, false);

        assertThatThrownBy(() -> accommodationService.getAccommodationSummary(trip.getId(), cara.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unrelatedTravelerCannotViewSummary() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User eve = user("eve@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(eve.getEmail())).thenReturn(Optional.of(eve));
        stubAccepted(trip, eve, false);

        assertThatThrownBy(() -> accommodationService.getAccommodationSummary(trip.getId(), eve.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    // --- Attach hotel booking (34-41) ---

    @Test
    void organizerCanAttachOwnHotelBooking() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        HotelBooking booking = bookingOwnedBy(alice);
        UUID bookingId = booking.getId();
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(HotelBooking.class))).thenAnswer(inv -> inv.getArgument(0));

        HotelBookingResponse response = accommodationService.attachBookingToTrip(
                trip.getId(), new AttachHotelBookingRequest(bookingId), alice.getEmail());

        assertThat(response.tripId()).isEqualTo(trip.getId());
    }

    @Test
    void acceptedMemberCanAttachOwnHotelBooking() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        HotelBooking booking = bookingOwnedBy(bob);
        UUID bookingId = booking.getId();
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(bob.getEmail())).thenReturn(Optional.of(bob));
        stubAccepted(trip, bob, true);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(HotelBooking.class))).thenAnswer(inv -> inv.getArgument(0));

        HotelBookingResponse response = accommodationService.attachBookingToTrip(
                trip.getId(), new AttachHotelBookingRequest(bookingId), bob.getEmail());

        assertThat(response.tripId()).isEqualTo(trip.getId());
    }

    @Test
    void memberCannotAttachAnotherMembersHotelBooking() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        HotelBooking bobsBooking = bookingOwnedBy(bob); // owned by bob, not alice
        UUID bookingId = bobsBooking.getId();
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(bobsBooking));

        assertThatThrownBy(() -> accommodationService.attachBookingToTrip(
                trip.getId(), new AttachHotelBookingRequest(bookingId), alice.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void organizerCannotAttachAnotherMembersHotelBookingEither() {
        // Confirms organizer status alone is not sufficient - HotelBooking
        // ownership is still required, matching "do not weaken" the IDOR fix.
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice); // alice organizes
        HotelBooking bobsBooking = bookingOwnedBy(bob);
        UUID bookingId = bobsBooking.getId();
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(bobsBooking));

        assertThatThrownBy(() -> accommodationService.attachBookingToTrip(
                trip.getId(), new AttachHotelBookingRequest(bookingId), alice.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unrelatedTravelerCannotAttach() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User eve = user("eve@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(eve.getEmail())).thenReturn(Optional.of(eve));
        stubAccepted(trip, eve, false);

        assertThatThrownBy(() -> accommodationService.attachBookingToTrip(
                trip.getId(), new AttachHotelBookingRequest(UUID.randomUUID()), eve.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void invitedUserCannotAttach() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(bob.getEmail())).thenReturn(Optional.of(bob));
        stubAccepted(trip, bob, false);

        assertThatThrownBy(() -> accommodationService.attachBookingToTrip(
                trip.getId(), new AttachHotelBookingRequest(UUID.randomUUID()), bob.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectedUserCannotAttach() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User cara = user("cara@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(cara.getEmail())).thenReturn(Optional.of(cara));
        stubAccepted(trip, cara, false);

        assertThatThrownBy(() -> accommodationService.attachBookingToTrip(
                trip.getId(), new AttachHotelBookingRequest(UUID.randomUUID()), cara.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    // Note: ROLE_PROVIDER exclusion is enforced declaratively at the controller
    // via @PreAuthorize("hasAnyRole('TRAVELER','ADMIN')"), matching the rest of
    // the codebase's established pattern - not re-tested at the service level.

    // --- Remove attachment (42-47) ---

    @Test
    void bookingOwnerCanRemoveOwnAttachment() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        HotelBooking booking = bookingOwnedBy(bob);
        booking.setTripId(trip.getId());
        UUID bookingId = booking.getId();
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(bob.getEmail())).thenReturn(Optional.of(bob));
        stubAccepted(trip, bob, true);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(HotelBooking.class))).thenAnswer(inv -> inv.getArgument(0));

        accommodationService.removeBookingFromTrip(trip.getId(), bookingId, bob.getEmail());

        assertThat(booking.getTripId()).isNull();
    }

    @Test
    void organizerCanRemoveAnotherMembersAttachment() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice); // alice organizes
        HotelBooking booking = bookingOwnedBy(bob); // bob's booking, attached to alice's trip
        booking.setTripId(trip.getId());
        UUID bookingId = booking.getId();
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(HotelBooking.class))).thenAnswer(inv -> inv.getArgument(0));

        accommodationService.removeBookingFromTrip(trip.getId(), bookingId, alice.getEmail());

        assertThat(booking.getTripId()).isNull();
    }

    @Test
    void unrelatedAcceptedMemberCannotRemoveAnotherMembersAttachment() {
        // Bob owns the booking, Alice organizes the trip - a third accepted
        // member (Cara) with neither role must not remove Bob's attachment.
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        User cara = user("cara@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        HotelBooking booking = bookingOwnedBy(bob);
        booking.setTripId(trip.getId());
        UUID bookingId = booking.getId();
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(cara.getEmail())).thenReturn(Optional.of(cara));
        stubAccepted(trip, cara, true);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> accommodationService.removeBookingFromTrip(trip.getId(), bookingId, cara.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void invitedUserCannotRemove() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User bob = user("bob@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(bob.getEmail())).thenReturn(Optional.of(bob));
        stubAccepted(trip, bob, false);

        assertThatThrownBy(() -> accommodationService.removeBookingFromTrip(trip.getId(), UUID.randomUUID(), bob.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectedUserCannotRemove() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User cara = user("cara@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(cara.getEmail())).thenReturn(Optional.of(cara));
        stubAccepted(trip, cara, false);

        assertThatThrownBy(() -> accommodationService.removeBookingFromTrip(trip.getId(), UUID.randomUUID(), cara.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unrelatedTravelerCannotRemove() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User eve = user("eve@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(eve.getEmail())).thenReturn(Optional.of(eve));
        stubAccepted(trip, eve, false);

        assertThatThrownBy(() -> accommodationService.removeBookingFromTrip(trip.getId(), UUID.randomUUID(), eve.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    // --- cross-trip / ownership regression safety (51, 52) ---

    @Test
    void crossTripAttachmentCannotBeRemovedThroughUnrelatedTrip() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        User cara = user("cara@travelease.test", Role.ROLE_TRAVELER);
        Trip tripA = trip(alice);
        Trip tripB = trip(cara);
        HotelBooking bookingAttachedToTripB = bookingOwnedBy(cara);
        bookingAttachedToTripB.setTripId(tripB.getId());
        UUID bookingId = bookingAttachedToTripB.getId();

        when(tripRepository.findById(tripA.getId())).thenReturn(Optional.of(tripA));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(bookingAttachedToTripB));

        assertThatThrownBy(() -> accommodationService.removeBookingFromTrip(tripA.getId(), bookingId, alice.getEmail()))
                .isInstanceOf(com.travelease.backend.shared.exception.InvalidRequestException.class);
    }

    @Test
    void hotelBookingOwnershipEnforcementRemainsIntactForDirectBookingEndpoints() {
        // Sanity check that the prior IDOR fix (ensureBookingOwner on
        // getBooking/updateBooking/cancelBooking/getBill) is untouched by this
        // task's changes.
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        HotelBooking booking = bookingOwnedBy(alice);
        UUID bookingId = booking.getId();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> accommodationService.getBooking(bookingId, "bob@travelease.test"))
                .isInstanceOf(AccessDeniedException.class);
    }

    // --- Traveler Trip lifecycle lock (completed/cancelled trips are read-only) ---

    @Test
    void cannotAttachHotelBookingToCompletedTrip() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        trip.setStatus(TravelerTripStatus.COMPLETED);
        HotelBooking booking = bookingOwnedBy(alice);
        UUID bookingId = booking.getId();
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));

        assertThatThrownBy(() -> accommodationService.attachBookingToTrip(
                trip.getId(), new AttachHotelBookingRequest(bookingId), alice.getEmail()))
                .isInstanceOf(com.travelease.backend.shared.exception.InvalidRequestException.class);
    }

    @Test
    void cannotDetachHotelBookingFromCancelledTrip() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        trip.setStatus(TravelerTripStatus.CANCELLED);
        HotelBooking booking = bookingOwnedBy(alice);
        booking.setTripId(trip.getId());
        UUID bookingId = booking.getId();
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));

        assertThatThrownBy(() -> accommodationService.removeBookingFromTrip(trip.getId(), bookingId, alice.getEmail()))
                .isInstanceOf(com.travelease.backend.shared.exception.InvalidRequestException.class);
    }

    @Test
    void accommodationSummaryStillReadableForCompletedTrip() {
        User alice = user("alice@travelease.test", Role.ROLE_TRAVELER);
        Trip trip = trip(alice);
        trip.setStatus(TravelerTripStatus.COMPLETED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));
        when(bookingRepository.findByTripId(trip.getId())).thenReturn(List.of());

        AccommodationSummaryResponse response = accommodationService.getAccommodationSummary(trip.getId(), alice.getEmail());

        assertThat(response.tripId()).isEqualTo(trip.getId());
    }
}
