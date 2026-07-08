package com.travelease.backend.busbooking.service.impl;

import com.travelease.backend.auth.entity.Role;
import com.travelease.backend.auth.entity.User;
import com.travelease.backend.busbooking.dto.request.AttachBusBookingRequest;
import com.travelease.backend.busbooking.dto.response.TripBusBookingResponse;
import com.travelease.backend.busbooking.dto.response.TripBusBookingSummaryResponse;
import com.travelease.backend.busbooking.entity.Booking;
import com.travelease.backend.busbooking.entity.BusSchedule;
import com.travelease.backend.busbooking.entity.Route;
import com.travelease.backend.busbooking.entity.enums.BookingStatus;
import com.travelease.backend.busbooking.exception.BookingException;
import com.travelease.backend.busbooking.exception.ResourceNotFoundException;
import com.travelease.backend.busbooking.repository.BookingRepository;
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
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Bus Booking to Traveler Trip attachment authorization. Only the attach/
 * detach/list dependencies are exercised, so every other BookingServiceImpl
 * dependency is passed as null - none of it is touched by these code paths
 * (matching how BookingServiceImplOwnershipTest already tolerates a partial
 * dependency set for the methods it exercises).
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceImplTripAuthorizationTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private SecurityUtil securityUtil;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private TripMemberRepository tripMemberRepository;

    private BookingServiceImpl bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingServiceImpl(
                bookingRepository, null, null, null, null, null,
                securityUtil, null, null, null,
                tripRepository, new TripAuthorizationService(tripMemberRepository));
    }

    private User user(UUID id) {
        User user = new User();
        user.setId(id);
        user.setRole(Role.ROLE_TRAVELER);
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

    private Booking booking(UUID ownerId, BookingStatus status) {
        Route route = new Route();
        route.setSource("Mumbai");
        route.setDestination("Goa");
        BusSchedule schedule = BusSchedule.builder()
                .id(500L)
                .route(route)
                .travelDate(LocalDate.now().plusDays(6))
                .build();
        return Booking.builder()
                .id(1L)
                .userId(ownerId)
                .schedule(schedule)
                .bookingReference("BK1")
                .status(status)
                .totalFare(500.0)
                .build();
    }

    private void stubAccepted(Trip trip, User user, boolean accepted) {
        when(tripMemberRepository.existsByTripIdAndUserIdAndMemberStatus(
                trip.getId(), user.getId(), com.travelease.backend.trip.entity.TripMemberStatus.ACCEPTED))
                .thenReturn(accepted);
    }

    // --- attach ---

    @Test
    void organizerAttachesOwnConfirmedBooking() {
        User alice = user(UUID.randomUUID());
        Trip trip = trip(alice);
        Booking booking = booking(alice.getId(), BookingStatus.CONFIRMED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(securityUtil.getCurrentUserId()).thenReturn(alice.getId());
        when(securityUtil.getCurrentUserRoles()).thenReturn(Set.of("ROLE_TRAVELER"));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        TripBusBookingResponse response = bookingService.attachBookingToTrip(trip.getId(), new AttachBusBookingRequest(1L));

        assertThat(response.travelerTripId()).isEqualTo(trip.getId());
    }

    @Test
    void acceptedMemberAttachesOwnCompletedBooking() {
        User alice = user(UUID.randomUUID());
        User bob = user(UUID.randomUUID());
        Trip trip = trip(alice);
        Booking booking = booking(bob.getId(), BookingStatus.COMPLETED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(securityUtil.getCurrentUserId()).thenReturn(bob.getId());
        when(securityUtil.getCurrentUserRoles()).thenReturn(Set.of("ROLE_TRAVELER"));
        stubAccepted(trip, bob, true);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        TripBusBookingResponse response = bookingService.attachBookingToTrip(trip.getId(), new AttachBusBookingRequest(1L));

        assertThat(response.travelerTripId()).isEqualTo(trip.getId());
    }

    @Test
    void organizerCannotAttachAcceptedMembersBooking() {
        User alice = user(UUID.randomUUID());
        User bob = user(UUID.randomUUID());
        Trip trip = trip(alice);
        Booking bobsBooking = booking(bob.getId(), BookingStatus.CONFIRMED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(securityUtil.getCurrentUserId()).thenReturn(alice.getId());
        when(securityUtil.getCurrentUserRoles()).thenReturn(Set.of("ROLE_TRAVELER"));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(bobsBooking));

        assertThatThrownBy(() -> bookingService.attachBookingToTrip(trip.getId(), new AttachBusBookingRequest(1L)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void acceptedMemberCannotAttachOrganizersBooking() {
        User alice = user(UUID.randomUUID());
        User bob = user(UUID.randomUUID());
        Trip trip = trip(alice);
        Booking alicesBooking = booking(alice.getId(), BookingStatus.CONFIRMED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(securityUtil.getCurrentUserId()).thenReturn(bob.getId());
        when(securityUtil.getCurrentUserRoles()).thenReturn(Set.of("ROLE_TRAVELER"));
        stubAccepted(trip, bob, true);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(alicesBooking));

        assertThatThrownBy(() -> bookingService.attachBookingToTrip(trip.getId(), new AttachBusBookingRequest(1L)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void invitedMemberCannotAttach() {
        User alice = user(UUID.randomUUID());
        User dan = user(UUID.randomUUID());
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(securityUtil.getCurrentUserId()).thenReturn(dan.getId());
        when(securityUtil.getCurrentUserRoles()).thenReturn(Set.of("ROLE_TRAVELER"));
        stubAccepted(trip, dan, false);

        assertThatThrownBy(() -> bookingService.attachBookingToTrip(trip.getId(), new AttachBusBookingRequest(1L)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectedMemberCannotAttach() {
        User alice = user(UUID.randomUUID());
        User cara = user(UUID.randomUUID());
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(securityUtil.getCurrentUserId()).thenReturn(cara.getId());
        when(securityUtil.getCurrentUserRoles()).thenReturn(Set.of("ROLE_TRAVELER"));
        stubAccepted(trip, cara, false);

        assertThatThrownBy(() -> bookingService.attachBookingToTrip(trip.getId(), new AttachBusBookingRequest(1L)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unrelatedTravelerCannotAttach() {
        User alice = user(UUID.randomUUID());
        User eve = user(UUID.randomUUID());
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(securityUtil.getCurrentUserId()).thenReturn(eve.getId());
        when(securityUtil.getCurrentUserRoles()).thenReturn(Set.of("ROLE_TRAVELER"));
        stubAccepted(trip, eve, false);

        assertThatThrownBy(() -> bookingService.attachBookingToTrip(trip.getId(), new AttachBusBookingRequest(1L)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void pendingBookingCannotBeAttached() {
        User alice = user(UUID.randomUUID());
        Trip trip = trip(alice);
        Booking booking = booking(alice.getId(), BookingStatus.PENDING);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(securityUtil.getCurrentUserId()).thenReturn(alice.getId());
        when(securityUtil.getCurrentUserRoles()).thenReturn(Set.of("ROLE_TRAVELER"));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.attachBookingToTrip(trip.getId(), new AttachBusBookingRequest(1L)))
                .isInstanceOf(BookingException.class);
    }

    @Test
    void cancelledBookingCannotBeAttached() {
        User alice = user(UUID.randomUUID());
        Trip trip = trip(alice);
        Booking booking = booking(alice.getId(), BookingStatus.CANCELLED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(securityUtil.getCurrentUserId()).thenReturn(alice.getId());
        when(securityUtil.getCurrentUserRoles()).thenReturn(Set.of("ROLE_TRAVELER"));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.attachBookingToTrip(trip.getId(), new AttachBusBookingRequest(1L)))
                .isInstanceOf(BookingException.class);
    }

    @Test
    void reattachingToSameTripIsIdempotent() {
        User alice = user(UUID.randomUUID());
        Trip trip = trip(alice);
        Booking booking = booking(alice.getId(), BookingStatus.CONFIRMED);
        booking.setTravelerTripId(trip.getId());
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(securityUtil.getCurrentUserId()).thenReturn(alice.getId());
        when(securityUtil.getCurrentUserRoles()).thenReturn(Set.of("ROLE_TRAVELER"));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        TripBusBookingResponse response = bookingService.attachBookingToTrip(trip.getId(), new AttachBusBookingRequest(1L));

        assertThat(response.travelerTripId()).isEqualTo(trip.getId());
    }

    @Test
    void attachingToADifferentTripIsRejected() {
        User alice = user(UUID.randomUUID());
        Trip tripA = trip(alice);
        Trip tripB = trip(alice);
        Booking booking = booking(alice.getId(), BookingStatus.CONFIRMED);
        booking.setTravelerTripId(tripB.getId());
        when(tripRepository.findById(tripA.getId())).thenReturn(Optional.of(tripA));
        when(securityUtil.getCurrentUserId()).thenReturn(alice.getId());
        when(securityUtil.getCurrentUserRoles()).thenReturn(Set.of("ROLE_TRAVELER"));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.attachBookingToTrip(tripA.getId(), new AttachBusBookingRequest(1L)))
                .isInstanceOf(BookingException.class);
    }

    @Test
    void nonexistentTripReturns404() {
        UUID tripId = UUID.randomUUID();
        when(tripRepository.findById(tripId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.attachBookingToTrip(tripId, new AttachBusBookingRequest(1L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- detach ---

    @Test
    void ownerCanDetachOwnBooking() {
        User alice = user(UUID.randomUUID());
        Trip trip = trip(alice);
        Booking booking = booking(alice.getId(), BookingStatus.CONFIRMED);
        booking.setTravelerTripId(trip.getId());
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(securityUtil.getCurrentUserId()).thenReturn(alice.getId());
        when(securityUtil.getCurrentUserRoles()).thenReturn(Set.of("ROLE_TRAVELER"));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        bookingService.removeBookingFromTrip(trip.getId(), 1L);

        assertThat(booking.getTravelerTripId()).isNull();
    }

    @Test
    void organizerCannotDetachAcceptedMembersBookingEvenAsOrganizer() {
        User alice = user(UUID.randomUUID());
        User bob = user(UUID.randomUUID());
        Trip trip = trip(alice); // alice organizes
        Booking bobsBooking = booking(bob.getId(), BookingStatus.CONFIRMED);
        bobsBooking.setTravelerTripId(trip.getId());
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(securityUtil.getCurrentUserId()).thenReturn(alice.getId());
        when(securityUtil.getCurrentUserRoles()).thenReturn(Set.of("ROLE_TRAVELER"));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(bobsBooking));

        assertThatThrownBy(() -> bookingService.removeBookingFromTrip(trip.getId(), 1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void detachingBookingNotAttachedToThatTripIsRejected() {
        User alice = user(UUID.randomUUID());
        Trip trip = trip(alice);
        Booking booking = booking(alice.getId(), BookingStatus.CONFIRMED); // travelerTripId null
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(securityUtil.getCurrentUserId()).thenReturn(alice.getId());
        when(securityUtil.getCurrentUserRoles()).thenReturn(Set.of("ROLE_TRAVELER"));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.removeBookingFromTrip(trip.getId(), 1L))
                .isInstanceOf(BookingException.class);
    }

    // --- list / shared visibility ---

    @Test
    void organizerCanViewTripBusBookings() {
        User alice = user(UUID.randomUUID());
        User bob = user(UUID.randomUUID());
        Trip trip = trip(alice);
        Booking bobsBooking = booking(bob.getId(), BookingStatus.CONFIRMED);
        bobsBooking.setTravelerTripId(trip.getId());
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(securityUtil.getCurrentUserId()).thenReturn(alice.getId());
        when(securityUtil.getCurrentUserRoles()).thenReturn(Set.of("ROLE_TRAVELER"));
        when(bookingRepository.findByTravelerTripId(trip.getId())).thenReturn(List.of(bobsBooking));

        TripBusBookingSummaryResponse response = bookingService.getTripBusBookings(trip.getId());

        assertThat(response.bookingCount()).isEqualTo(1);
        assertThat(response.bookings().get(0).bookedByUserId()).isEqualTo(bob.getId());
    }

    @Test
    void unrelatedTravelerCannotViewTripBusBookings() {
        User alice = user(UUID.randomUUID());
        User eve = user(UUID.randomUUID());
        Trip trip = trip(alice);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(securityUtil.getCurrentUserId()).thenReturn(eve.getId());
        when(securityUtil.getCurrentUserRoles()).thenReturn(Set.of("ROLE_TRAVELER"));
        stubAccepted(trip, eve, false);

        assertThatThrownBy(() -> bookingService.getTripBusBookings(trip.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void adminCanAttachAcrossOwnership() {
        User alice = user(UUID.randomUUID());
        User bob = user(UUID.randomUUID());
        Trip trip = trip(alice);
        Booking bobsBooking = booking(bob.getId(), BookingStatus.CONFIRMED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(securityUtil.getCurrentUserRoles()).thenReturn(Set.of("ROLE_ADMIN"));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(bobsBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        TripBusBookingResponse response = bookingService.attachBookingToTrip(trip.getId(), new AttachBusBookingRequest(1L));

        assertThat(response.travelerTripId()).isEqualTo(trip.getId());
    }

    // --- Traveler Trip lifecycle lock (completed/cancelled trips are read-only) ---

    @Test
    void cannotAttachBusBookingToCompletedTrip() {
        User alice = user(UUID.randomUUID());
        Trip trip = trip(alice);
        trip.setStatus(com.travelease.backend.trip.entity.TravelerTripStatus.COMPLETED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(securityUtil.getCurrentUserId()).thenReturn(alice.getId());
        when(securityUtil.getCurrentUserRoles()).thenReturn(Set.of("ROLE_TRAVELER"));
        // requireMutableTrip rejects before the booking is even looked up.

        assertThatThrownBy(() -> bookingService.attachBookingToTrip(trip.getId(), new AttachBusBookingRequest(1L)))
                .isInstanceOf(com.travelease.backend.shared.exception.InvalidRequestException.class);
    }

    @Test
    void cannotDetachBusBookingFromCancelledTrip() {
        User alice = user(UUID.randomUUID());
        Trip trip = trip(alice);
        trip.setStatus(com.travelease.backend.trip.entity.TravelerTripStatus.CANCELLED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(securityUtil.getCurrentUserId()).thenReturn(alice.getId());
        when(securityUtil.getCurrentUserRoles()).thenReturn(Set.of("ROLE_TRAVELER"));
        // requireMutableTrip rejects before the booking is even looked up.

        assertThatThrownBy(() -> bookingService.removeBookingFromTrip(trip.getId(), 1L))
                .isInstanceOf(com.travelease.backend.shared.exception.InvalidRequestException.class);
    }

    @Test
    void tripBusBookingsStillReadableForCompletedTrip() {
        User alice = user(UUID.randomUUID());
        Trip trip = trip(alice);
        trip.setStatus(com.travelease.backend.trip.entity.TravelerTripStatus.COMPLETED);
        Booking booking = booking(alice.getId(), BookingStatus.CONFIRMED);
        booking.setTravelerTripId(trip.getId());
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(securityUtil.getCurrentUserId()).thenReturn(alice.getId());
        when(securityUtil.getCurrentUserRoles()).thenReturn(Set.of("ROLE_TRAVELER"));
        when(bookingRepository.findByTravelerTripId(trip.getId())).thenReturn(List.of(booking));

        TripBusBookingSummaryResponse response = bookingService.getTripBusBookings(trip.getId());

        assertThat(response.bookingCount()).isEqualTo(1);
    }
}
