package com.travelease.backend.itinerary.service;

import com.travelease.backend.auth.entity.Role;
import com.travelease.backend.auth.entity.User;
import com.travelease.backend.auth.repository.UserRepository;
import com.travelease.backend.busbooking.security.SecurityUtil;
import com.travelease.backend.itinerary.dto.ActivityBookingResponse;
import com.travelease.backend.itinerary.dto.AttachActivityBookingRequest;
import com.travelease.backend.itinerary.entity.Activity;
import com.travelease.backend.itinerary.entity.ActivityBooking;
import com.travelease.backend.itinerary.entity.ActivityBookingStatus;
import com.travelease.backend.itinerary.entity.ActivitySlot;
import com.travelease.backend.itinerary.repository.ActivityBookingRepository;
import com.travelease.backend.itinerary.repository.ActivityRepository;
import com.travelease.backend.itinerary.repository.ActivitySlotRepository;
import com.travelease.backend.shared.exception.InvalidRequestException;
import com.travelease.backend.trip.entity.Trip;
import com.travelease.backend.trip.entity.TravelerTripStatus;
import com.travelease.backend.trip.entity.TripMemberStatus;
import com.travelease.backend.trip.repository.TripMemberRepository;
import com.travelease.backend.trip.repository.TripRepository;
import com.travelease.backend.trip.security.TripAuthorizationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Phase 3 - Activity Booking <-> Traveler Trip integration tests. Mirrors the
 * real-SecurityUtil/real-TripAuthorizationService-backed-by-mocks convention
 * already used by ActivityBookingServiceImplTest and
 * ActivityProviderServiceImplOwnershipTest, so the actual authorization wiring
 * is exercised end-to-end rather than mocked away.
 */
@ExtendWith(MockitoExtension.class)
class ActivityBookingTripIntegrationTest {

    private static final UUID ORGANIZER_ID = UUID.randomUUID();
    private static final UUID ACCEPTED_MEMBER_ID = UUID.randomUUID();
    private static final UUID INVITED_MEMBER_ID = UUID.randomUUID();
    private static final UUID REJECTED_MEMBER_ID = UUID.randomUUID();
    private static final UUID UNRELATED_TRAVELER_ID = UUID.randomUUID();

    @Mock
    private ActivityBookingRepository activityBookingRepository;
    @Mock
    private ActivitySlotRepository activitySlotRepository;
    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private TripMemberRepository tripMemberRepository;

    private ActivityBookingServiceImpl service;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void setUp() {
        SecurityUtil securityUtil = new SecurityUtil(userRepository);
        TripAuthorizationService tripAuthorizationService = new TripAuthorizationService(tripMemberRepository);
        service = new ActivityBookingServiceImpl(
                activityBookingRepository, activitySlotRepository, activityRepository, userRepository,
                tripRepository, tripAuthorizationService, securityUtil);
    }

    private void authenticateAs(User user, String role) {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user.getEmail(), null, List.of(new SimpleGrantedAuthority(role))));
    }

    private User traveler(UUID id, String email) {
        User user = new User();
        setId(user, id);
        user.setEmail(email);
        user.setRole(Role.ROLE_TRAVELER);
        return user;
    }

    private Trip tripOrganizedBy(User organizer, TravelerTripStatus status) {
        Trip trip = new Trip();
        trip.setOrganizer(organizer);
        trip.setStatus(status);
        return trip;
    }

    private void mockMemberStatus(Trip trip, UUID userId, TripMemberStatus status) {
        when(tripMemberRepository.existsByTripIdAndUserIdAndMemberStatus(trip.getId(), userId, TripMemberStatus.ACCEPTED))
                .thenReturn(status == TripMemberStatus.ACCEPTED);
    }

    private ActivityBooking confirmedBookingOf(User owner, ActivityBookingStatus status) {
        Activity activity = new Activity();
        activity.setActivityId(UUID.randomUUID().toString());
        activity.setProviderId(201L);
        activity.setActivityName("Mumbai Heritage Walking Tour");
        ActivitySlot slot = new ActivitySlot();
        setId(slot, UUID.randomUUID());
        slot.setActivity(activity);
        slot.setActivityDate(LocalDate.now().plusDays(5));
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setPrice(new BigDecimal("500.00"));
        slot.setCapacity(10);

        ActivityBooking booking = new ActivityBooking();
        setId(booking, UUID.randomUUID());
        booking.setActivitySlot(slot);
        booking.setBookedBy(owner);
        booking.setParticipantCount(2);
        booking.setPricePerParticipant(slot.getPrice());
        booking.setTotalAmount(slot.getPrice().multiply(BigDecimal.valueOf(2)));
        booking.setStatus(status);
        return booking;
    }

    // --- attach: relationship authorization ---

    @Test
    void organizerAttachesOwnConfirmedBookingToOwnMutableTrip() {
        setUp();
        User organizer = traveler(ORGANIZER_ID, "organizer@travelease.com");
        authenticateAs(organizer, "ROLE_TRAVELER");
        Trip trip = tripOrganizedBy(organizer, TravelerTripStatus.PLANNING);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        ActivityBooking booking = confirmedBookingOf(organizer, ActivityBookingStatus.CONFIRMED);
        when(activityBookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(activityBookingRepository.save(any(ActivityBooking.class))).thenAnswer(inv -> inv.getArgument(0));

        ActivityBookingResponse response = service.attachBookingToTrip(trip.getId(), new AttachActivityBookingRequest(booking.getId()));

        assertThat(response.bookingId()).isEqualTo(booking.getId());
        assertThat(booking.getTripId()).isEqualTo(trip.getId());
    }

    @Test
    void acceptedMemberAttachesOwnConfirmedBooking() {
        setUp();
        User organizer = traveler(ORGANIZER_ID, "organizer@travelease.com");
        User member = traveler(ACCEPTED_MEMBER_ID, "accepted@travelease.com");
        authenticateAs(member, "ROLE_TRAVELER");
        Trip trip = tripOrganizedBy(organizer, TravelerTripStatus.PLANNING);
        mockMemberStatus(trip, ACCEPTED_MEMBER_ID, TripMemberStatus.ACCEPTED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        ActivityBooking booking = confirmedBookingOf(member, ActivityBookingStatus.CONFIRMED);
        when(activityBookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(activityBookingRepository.save(any(ActivityBooking.class))).thenAnswer(inv -> inv.getArgument(0));

        ActivityBookingResponse response = service.attachBookingToTrip(trip.getId(), new AttachActivityBookingRequest(booking.getId()));

        assertThat(response.bookingId()).isEqualTo(booking.getId());
    }

    @Test
    void invitedMemberDeniedAttach() {
        setUp();
        User organizer = traveler(ORGANIZER_ID, "organizer@travelease.com");
        User invited = traveler(INVITED_MEMBER_ID, "invited@travelease.com");
        authenticateAs(invited, "ROLE_TRAVELER");
        Trip trip = tripOrganizedBy(organizer, TravelerTripStatus.PLANNING);
        mockMemberStatus(trip, INVITED_MEMBER_ID, TripMemberStatus.INVITED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> service.attachBookingToTrip(trip.getId(), new AttachActivityBookingRequest(UUID.randomUUID())))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectedMemberDeniedAttach() {
        setUp();
        User organizer = traveler(ORGANIZER_ID, "organizer@travelease.com");
        User rejected = traveler(REJECTED_MEMBER_ID, "rejected@travelease.com");
        authenticateAs(rejected, "ROLE_TRAVELER");
        Trip trip = tripOrganizedBy(organizer, TravelerTripStatus.PLANNING);
        mockMemberStatus(trip, REJECTED_MEMBER_ID, TripMemberStatus.REJECTED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> service.attachBookingToTrip(trip.getId(), new AttachActivityBookingRequest(UUID.randomUUID())))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unrelatedTravelerDeniedAttach() {
        setUp();
        User organizer = traveler(ORGANIZER_ID, "organizer@travelease.com");
        User unrelated = traveler(UNRELATED_TRAVELER_ID, "unrelated@travelease.com");
        authenticateAs(unrelated, "ROLE_TRAVELER");
        Trip trip = tripOrganizedBy(organizer, TravelerTripStatus.PLANNING);
        mockMemberStatus(trip, UNRELATED_TRAVELER_ID, null);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> service.attachBookingToTrip(trip.getId(), new AttachActivityBookingRequest(UUID.randomUUID())))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void adminPassesTripAccessButStillCannotAttachBookingItDoesNotOwn() {
        setUp();
        User organizer = traveler(ORGANIZER_ID, "organizer@travelease.com");
        User admin = traveler(UUID.randomUUID(), "admin@travelease.com");
        authenticateAs(admin, "ROLE_ADMIN");
        Trip trip = tripOrganizedBy(organizer, TravelerTripStatus.PLANNING);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        ActivityBooking booking = confirmedBookingOf(organizer, ActivityBookingStatus.CONFIRMED);
        when(activityBookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        // Admin bypasses requireMember/requireMutableTrip (Trip-level authority)
        // but booking ownership is a separate axis with no admin bypass in this
        // module - matches Hotel's ensureBookingOwner precedent, not Bus's
        // admin-bypassing ensureOwnership (see Phase 3 decision record).
        assertThatThrownBy(() -> service.attachBookingToTrip(trip.getId(), new AttachActivityBookingRequest(booking.getId())))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void travelerCannotAttachAnotherTravelersBookingEvenAsOrganizer() {
        setUp();
        User organizer = traveler(ORGANIZER_ID, "organizer@travelease.com");
        User otherTraveler = traveler(UNRELATED_TRAVELER_ID, "other@travelease.com");
        authenticateAs(organizer, "ROLE_TRAVELER");
        Trip trip = tripOrganizedBy(organizer, TravelerTripStatus.PLANNING);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        ActivityBooking othersBooking = confirmedBookingOf(otherTraveler, ActivityBookingStatus.CONFIRMED);
        when(activityBookingRepository.findById(othersBooking.getId())).thenReturn(Optional.of(othersBooking));

        assertThatThrownBy(() -> service.attachBookingToTrip(trip.getId(), new AttachActivityBookingRequest(othersBooking.getId())))
                .isInstanceOf(AccessDeniedException.class);
    }

    // --- attach: status eligibility ---

    @Test
    void cancelledBookingCannotBeNewlyAttached() {
        setUp();
        User organizer = traveler(ORGANIZER_ID, "organizer@travelease.com");
        authenticateAs(organizer, "ROLE_TRAVELER");
        Trip trip = tripOrganizedBy(organizer, TravelerTripStatus.PLANNING);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        ActivityBooking cancelled = confirmedBookingOf(organizer, ActivityBookingStatus.CANCELLED);
        when(activityBookingRepository.findById(cancelled.getId())).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> service.attachBookingToTrip(trip.getId(), new AttachActivityBookingRequest(cancelled.getId())))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void attendedBookingCanBeNewlyAttachedAsHistorical() {
        setUp();
        User organizer = traveler(ORGANIZER_ID, "organizer@travelease.com");
        authenticateAs(organizer, "ROLE_TRAVELER");
        Trip trip = tripOrganizedBy(organizer, TravelerTripStatus.PLANNING);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        ActivityBooking attended = confirmedBookingOf(organizer, ActivityBookingStatus.ATTENDED);
        when(activityBookingRepository.findById(attended.getId())).thenReturn(Optional.of(attended));
        when(activityBookingRepository.save(any(ActivityBooking.class))).thenAnswer(inv -> inv.getArgument(0));

        ActivityBookingResponse response = service.attachBookingToTrip(trip.getId(), new AttachActivityBookingRequest(attended.getId()));

        assertThat(response.status()).isEqualTo(ActivityBookingStatus.ATTENDED);
    }

    @Test
    void noShowBookingCanBeNewlyAttachedAsHistorical() {
        setUp();
        User organizer = traveler(ORGANIZER_ID, "organizer@travelease.com");
        authenticateAs(organizer, "ROLE_TRAVELER");
        Trip trip = tripOrganizedBy(organizer, TravelerTripStatus.PLANNING);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        ActivityBooking noShow = confirmedBookingOf(organizer, ActivityBookingStatus.NO_SHOW);
        when(activityBookingRepository.findById(noShow.getId())).thenReturn(Optional.of(noShow));
        when(activityBookingRepository.save(any(ActivityBooking.class))).thenAnswer(inv -> inv.getArgument(0));

        ActivityBookingResponse response = service.attachBookingToTrip(trip.getId(), new AttachActivityBookingRequest(noShow.getId()));

        assertThat(response.status()).isEqualTo(ActivityBookingStatus.NO_SHOW);
    }

    // --- attach: duplicate / reattachment rules ---

    @Test
    void duplicateSameTripAttachIsIdempotent() {
        setUp();
        User organizer = traveler(ORGANIZER_ID, "organizer@travelease.com");
        authenticateAs(organizer, "ROLE_TRAVELER");
        Trip trip = tripOrganizedBy(organizer, TravelerTripStatus.PLANNING);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        ActivityBooking booking = confirmedBookingOf(organizer, ActivityBookingStatus.CONFIRMED);
        booking.setTripId(trip.getId());
        when(activityBookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        ActivityBookingResponse response = service.attachBookingToTrip(trip.getId(), new AttachActivityBookingRequest(booking.getId()));

        assertThat(response.bookingId()).isEqualTo(booking.getId());
    }

    @Test
    void crossTripReattachmentRejected() {
        setUp();
        User organizer = traveler(ORGANIZER_ID, "organizer@travelease.com");
        authenticateAs(organizer, "ROLE_TRAVELER");
        Trip tripA = tripOrganizedBy(organizer, TravelerTripStatus.PLANNING);
        Trip tripB = tripOrganizedBy(organizer, TravelerTripStatus.PLANNING);
        when(tripRepository.findById(tripB.getId())).thenReturn(Optional.of(tripB));
        ActivityBooking booking = confirmedBookingOf(organizer, ActivityBookingStatus.CONFIRMED);
        booking.setTripId(tripA.getId());
        when(activityBookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.attachBookingToTrip(tripB.getId(), new AttachActivityBookingRequest(booking.getId())))
                .isInstanceOf(InvalidRequestException.class);
    }

    // --- attach: Trip lifecycle ---

    @Test
    void attachDeniedOnCompletedTrip() {
        setUp();
        User organizer = traveler(ORGANIZER_ID, "organizer@travelease.com");
        authenticateAs(organizer, "ROLE_TRAVELER");
        Trip trip = tripOrganizedBy(organizer, TravelerTripStatus.COMPLETED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> service.attachBookingToTrip(trip.getId(), new AttachActivityBookingRequest(UUID.randomUUID())))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void attachDeniedOnCancelledTrip() {
        setUp();
        User organizer = traveler(ORGANIZER_ID, "organizer@travelease.com");
        authenticateAs(organizer, "ROLE_TRAVELER");
        Trip trip = tripOrganizedBy(organizer, TravelerTripStatus.CANCELLED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> service.attachBookingToTrip(trip.getId(), new AttachActivityBookingRequest(UUID.randomUUID())))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void attachAllowedOnOngoingTrip() {
        setUp();
        User organizer = traveler(ORGANIZER_ID, "organizer@travelease.com");
        authenticateAs(organizer, "ROLE_TRAVELER");
        Trip trip = tripOrganizedBy(organizer, TravelerTripStatus.ONGOING);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        ActivityBooking booking = confirmedBookingOf(organizer, ActivityBookingStatus.CONFIRMED);
        when(activityBookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(activityBookingRepository.save(any(ActivityBooking.class))).thenAnswer(inv -> inv.getArgument(0));

        ActivityBookingResponse response = service.attachBookingToTrip(trip.getId(), new AttachActivityBookingRequest(booking.getId()));

        assertThat(response).isNotNull();
    }

    // --- detach ---

    @Test
    void ownerDetachesSuccessfullyWithoutSideEffects() {
        setUp();
        User organizer = traveler(ORGANIZER_ID, "organizer@travelease.com");
        authenticateAs(organizer, "ROLE_TRAVELER");
        Trip trip = tripOrganizedBy(organizer, TravelerTripStatus.PLANNING);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        ActivityBooking booking = confirmedBookingOf(organizer, ActivityBookingStatus.CONFIRMED);
        booking.setTripId(trip.getId());
        when(activityBookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(activityBookingRepository.save(any(ActivityBooking.class))).thenAnswer(inv -> inv.getArgument(0));
        Integer originalParticipants = booking.getParticipantCount();
        BigDecimal originalPrice = booking.getTotalAmount();

        service.removeBookingFromTrip(trip.getId(), booking.getId());

        assertThat(booking.getTripId()).isNull();
        assertThat(booking.getStatus()).isEqualTo(ActivityBookingStatus.CONFIRMED);
        assertThat(booking.getParticipantCount()).isEqualTo(originalParticipants);
        assertThat(booking.getTotalAmount()).isEqualByComparingTo(originalPrice);
    }

    @Test
    void nonOwnerCannotDetachEvenAsOrganizer() {
        setUp();
        User organizer = traveler(ORGANIZER_ID, "organizer@travelease.com");
        User member = traveler(ACCEPTED_MEMBER_ID, "accepted@travelease.com");
        authenticateAs(organizer, "ROLE_TRAVELER");
        Trip trip = tripOrganizedBy(organizer, TravelerTripStatus.PLANNING);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        ActivityBooking membersBooking = confirmedBookingOf(member, ActivityBookingStatus.CONFIRMED);
        membersBooking.setTripId(trip.getId());
        when(activityBookingRepository.findById(membersBooking.getId())).thenReturn(Optional.of(membersBooking));

        // Deliberately owner-only detach (Bus precedent), not organizer-or-owner
        // (Hotel precedent) - see Phase 3 decision record.
        assertThatThrownBy(() -> service.removeBookingFromTrip(trip.getId(), membersBooking.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void detachDeniedOnTerminalTrip() {
        setUp();
        User organizer = traveler(ORGANIZER_ID, "organizer@travelease.com");
        authenticateAs(organizer, "ROLE_TRAVELER");
        Trip trip = tripOrganizedBy(organizer, TravelerTripStatus.CANCELLED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> service.removeBookingFromTrip(trip.getId(), UUID.randomUUID()))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void detachRejectedWhenBookingNotAttachedToThisTrip() {
        setUp();
        User organizer = traveler(ORGANIZER_ID, "organizer@travelease.com");
        authenticateAs(organizer, "ROLE_TRAVELER");
        Trip trip = tripOrganizedBy(organizer, TravelerTripStatus.PLANNING);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        ActivityBooking unattached = confirmedBookingOf(organizer, ActivityBookingStatus.CONFIRMED);
        when(activityBookingRepository.findById(unattached.getId())).thenReturn(Optional.of(unattached));

        assertThatThrownBy(() -> service.removeBookingFromTrip(trip.getId(), unattached.getId()))
                .isInstanceOf(InvalidRequestException.class);
    }

    // --- list / shared visibility ---

    @Test
    void organizerCanListTripActivityBookings() {
        setUp();
        User organizer = traveler(ORGANIZER_ID, "organizer@travelease.com");
        authenticateAs(organizer, "ROLE_TRAVELER");
        Trip trip = tripOrganizedBy(organizer, TravelerTripStatus.PLANNING);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        ActivityBooking booking = confirmedBookingOf(organizer, ActivityBookingStatus.CONFIRMED);
        booking.setTripId(trip.getId());
        when(activityBookingRepository.findByTripId(trip.getId())).thenReturn(List.of(booking));

        List<ActivityBookingResponse> responses = service.getTripActivityBookings(trip.getId());

        assertThat(responses).hasSize(1);
    }

    @Test
    void acceptedMemberCanListTripActivityBookings() {
        setUp();
        User organizer = traveler(ORGANIZER_ID, "organizer@travelease.com");
        User member = traveler(ACCEPTED_MEMBER_ID, "accepted@travelease.com");
        authenticateAs(member, "ROLE_TRAVELER");
        Trip trip = tripOrganizedBy(organizer, TravelerTripStatus.PLANNING);
        mockMemberStatus(trip, ACCEPTED_MEMBER_ID, TripMemberStatus.ACCEPTED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        ActivityBooking booking = confirmedBookingOf(organizer, ActivityBookingStatus.CONFIRMED);
        booking.setTripId(trip.getId());
        when(activityBookingRepository.findByTripId(trip.getId())).thenReturn(List.of(booking));

        List<ActivityBookingResponse> responses = service.getTripActivityBookings(trip.getId());

        assertThat(responses).hasSize(1);
    }

    @Test
    void invitedMemberDeniedListingTripActivityBookings() {
        setUp();
        User organizer = traveler(ORGANIZER_ID, "organizer@travelease.com");
        User invited = traveler(INVITED_MEMBER_ID, "invited@travelease.com");
        authenticateAs(invited, "ROLE_TRAVELER");
        Trip trip = tripOrganizedBy(organizer, TravelerTripStatus.PLANNING);
        mockMemberStatus(trip, INVITED_MEMBER_ID, TripMemberStatus.INVITED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> service.getTripActivityBookings(trip.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unrelatedTravelerDeniedListingTripActivityBookings() {
        setUp();
        User organizer = traveler(ORGANIZER_ID, "organizer@travelease.com");
        User unrelated = traveler(UNRELATED_TRAVELER_ID, "unrelated@travelease.com");
        authenticateAs(unrelated, "ROLE_TRAVELER");
        Trip trip = tripOrganizedBy(organizer, TravelerTripStatus.PLANNING);
        mockMemberStatus(trip, UNRELATED_TRAVELER_ID, null);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> service.getTripActivityBookings(trip.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void listWorksOnCompletedTripHistoricalReadNotBlocked() {
        setUp();
        User organizer = traveler(ORGANIZER_ID, "organizer@travelease.com");
        authenticateAs(organizer, "ROLE_TRAVELER");
        Trip trip = tripOrganizedBy(organizer, TravelerTripStatus.COMPLETED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        ActivityBooking booking = confirmedBookingOf(organizer, ActivityBookingStatus.ATTENDED);
        booking.setTripId(trip.getId());
        when(activityBookingRepository.findByTripId(trip.getId())).thenReturn(List.of(booking));

        List<ActivityBookingResponse> responses = service.getTripActivityBookings(trip.getId());

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).status()).isEqualTo(ActivityBookingStatus.ATTENDED);
    }

    @Test
    void listWorksOnCancelledTripHistoricalReadNotBlocked() {
        setUp();
        User organizer = traveler(ORGANIZER_ID, "organizer@travelease.com");
        authenticateAs(organizer, "ROLE_TRAVELER");
        Trip trip = tripOrganizedBy(organizer, TravelerTripStatus.CANCELLED);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        ActivityBooking booking = confirmedBookingOf(organizer, ActivityBookingStatus.CANCELLED);
        booking.setTripId(trip.getId());
        when(activityBookingRepository.findByTripId(trip.getId())).thenReturn(List.of(booking));

        List<ActivityBookingResponse> responses = service.getTripActivityBookings(trip.getId());

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).status()).isEqualTo(ActivityBookingStatus.CANCELLED);
    }

    private void setId(Object entity, UUID id) {
        try {
            var field = com.travelease.backend.shared.entity.BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
