package com.travelease.backend.itinerary.service;

import com.travelease.backend.auth.entity.Role;
import com.travelease.backend.auth.entity.User;
import com.travelease.backend.auth.repository.UserRepository;
import com.travelease.backend.busbooking.exception.SeatUnavailableException;
import com.travelease.backend.busbooking.security.SecurityUtil;
import com.travelease.backend.itinerary.dto.ActivityBookingResponse;
import com.travelease.backend.itinerary.dto.CreateActivityBookingRequest;
import com.travelease.backend.itinerary.entity.Activity;
import com.travelease.backend.itinerary.entity.ActivityBooking;
import com.travelease.backend.itinerary.entity.ActivityBookingStatus;
import com.travelease.backend.itinerary.entity.ActivitySlot;
import com.travelease.backend.itinerary.repository.ActivityBookingRepository;
import com.travelease.backend.itinerary.repository.ActivityRepository;
import com.travelease.backend.itinerary.repository.ActivitySlotRepository;
import com.travelease.backend.shared.exception.InvalidRequestException;
import com.travelease.backend.shared.exception.ResourceNotFoundException;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Activity Booking authorization/lifecycle tests. Follows the same real-
 * SecurityUtil-backed-by-mocked-UserRepository convention as
 * ActivityProviderServiceImplOwnershipTest, so the actual role/ownership
 * wiring is exercised rather than a mocked SecurityUtil.
 */
@ExtendWith(MockitoExtension.class)
class ActivityBookingServiceImplTest {

    private static final Long PROVIDER_201 = 201L;
    private static final Long PROVIDER_202 = 202L;
    private static final UUID TRAVELER_ID = UUID.randomUUID();
    private static final UUID OTHER_TRAVELER_ID = UUID.randomUUID();

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

    private void authenticateAs(String email, String role) {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, authorities));
    }

    private User travelerUser(UUID id, String email) {
        User user = new User();
        setId(user, id);
        user.setEmail(email);
        user.setRole(Role.ROLE_TRAVELER);
        return user;
    }

    private User activityProviderUser(String email, Long providerId) {
        User user = new User();
        user.setEmail(email);
        user.setRole(Role.ROLE_ACTIVITY_PROVIDER);
        user.setProviderId(providerId);
        return user;
    }

    private Activity activityOwnedBy(Long providerId) {
        Activity activity = new Activity();
        activity.setActivityId(UUID.randomUUID().toString());
        activity.setProviderId(providerId);
        activity.setActivityName("Test Activity " + providerId);
        return activity;
    }

    private ActivitySlot futureSlot(Activity activity, int capacity) {
        ActivitySlot slot = new ActivitySlot();
        setId(slot, UUID.randomUUID());
        slot.setActivity(activity);
        slot.setActivityDate(LocalDate.now().plusDays(5));
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setPrice(new BigDecimal("500.00"));
        slot.setCapacity(capacity);
        return slot;
    }

    private ActivitySlot pastSlot(Activity activity, int capacity) {
        ActivitySlot slot = new ActivitySlot();
        setId(slot, UUID.randomUUID());
        slot.setActivity(activity);
        slot.setActivityDate(LocalDate.now().minusDays(1));
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setPrice(new BigDecimal("500.00"));
        slot.setCapacity(capacity);
        return slot;
    }

    private ActivityBooking confirmedBookingOf(ActivitySlot slot, User bookedBy, int participants) {
        ActivityBooking booking = new ActivityBooking();
        setId(booking, UUID.randomUUID());
        booking.setActivitySlot(slot);
        booking.setBookedBy(bookedBy);
        booking.setParticipantCount(participants);
        booking.setPricePerParticipant(slot.getPrice());
        booking.setTotalAmount(slot.getPrice().multiply(BigDecimal.valueOf(participants)));
        booking.setStatus(ActivityBookingStatus.CONFIRMED);
        return booking;
    }

    // --- createBooking ---

    @Test
    void travelerCreatesBookingWithPriceSnapshotAndRemainingCapacity() {
        setUp();
        User traveler = travelerUser(TRAVELER_ID, "traveler@travelease.com");
        when(userRepository.findByEmail(traveler.getEmail())).thenReturn(Optional.of(traveler));
        when(userRepository.getReferenceById(TRAVELER_ID)).thenReturn(traveler);
        authenticateAs(traveler.getEmail(), "ROLE_TRAVELER");

        Activity activity = activityOwnedBy(PROVIDER_201);
        ActivitySlot slot = futureSlot(activity, 10);
        when(activitySlotRepository.findByIdForUpdate(slot.getId())).thenReturn(Optional.of(slot));
        when(activityBookingRepository.sumParticipantsByActivitySlotIdAndStatusIn(eq(slot.getId()), anyList()))
                .thenReturn(0);
        when(activityBookingRepository.saveAndFlush(any(ActivityBooking.class))).thenAnswer(inv -> inv.getArgument(0));

        ActivityBookingResponse response = service.createBooking(new CreateActivityBookingRequest(slot.getId(), 3));

        assertThat(response.status()).isEqualTo(ActivityBookingStatus.CONFIRMED);
        assertThat(response.pricePerParticipant()).isEqualByComparingTo("500.00");
        assertThat(response.totalAmount()).isEqualByComparingTo("1500.00");
        assertThat(response.participantCount()).isEqualTo(3);
        assertThat(response.bookedByUserId()).isEqualTo(TRAVELER_ID);
    }

    @Test
    void bookingRejectedWhenParticipantCountExceedsRemainingCapacity() {
        setUp();
        authenticateAs("traveler@travelease.com", "ROLE_TRAVELER");

        Activity activity = activityOwnedBy(PROVIDER_201);
        ActivitySlot slot = futureSlot(activity, 5);
        when(activitySlotRepository.findByIdForUpdate(slot.getId())).thenReturn(Optional.of(slot));
        when(activityBookingRepository.sumParticipantsByActivitySlotIdAndStatusIn(eq(slot.getId()), anyList()))
                .thenReturn(4);

        assertThatThrownBy(() -> service.createBooking(new CreateActivityBookingRequest(slot.getId(), 2)))
                .isInstanceOf(SeatUnavailableException.class);
    }

    @Test
    void bookingRejectedForSlotThatHasAlreadyStarted() {
        setUp();
        authenticateAs("traveler@travelease.com", "ROLE_TRAVELER");

        Activity activity = activityOwnedBy(PROVIDER_201);
        ActivitySlot slot = pastSlot(activity, 10);
        when(activitySlotRepository.findByIdForUpdate(slot.getId())).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> service.createBooking(new CreateActivityBookingRequest(slot.getId(), 1)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void bookingRejectedForUnknownSlot() {
        setUp();
        authenticateAs("traveler@travelease.com", "ROLE_TRAVELER");
        UUID missingSlotId = UUID.randomUUID();
        when(activitySlotRepository.findByIdForUpdate(missingSlotId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createBooking(new CreateActivityBookingRequest(missingSlotId, 1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- ownership: getMyBooking / cancelBooking ---

    @Test
    void travelerCannotReadAnotherTravelersBooking() {
        setUp();
        User otherTraveler = travelerUser(OTHER_TRAVELER_ID, "other@travelease.com");
        when(userRepository.findByEmail("traveler@travelease.com"))
                .thenReturn(Optional.of(travelerUser(TRAVELER_ID, "traveler@travelease.com")));
        authenticateAs("traveler@travelease.com", "ROLE_TRAVELER");

        Activity activity = activityOwnedBy(PROVIDER_201);
        ActivitySlot slot = futureSlot(activity, 10);
        ActivityBooking booking = confirmedBookingOf(slot, otherTraveler, 2);
        when(activityBookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.getMyBooking(booking.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void travelerCanCancelOwnConfirmedFutureBooking() {
        setUp();
        User traveler = travelerUser(TRAVELER_ID, "traveler@travelease.com");
        when(userRepository.findByEmail(traveler.getEmail())).thenReturn(Optional.of(traveler));
        authenticateAs(traveler.getEmail(), "ROLE_TRAVELER");

        Activity activity = activityOwnedBy(PROVIDER_201);
        ActivitySlot slot = futureSlot(activity, 10);
        ActivityBooking booking = confirmedBookingOf(slot, traveler, 2);
        when(activityBookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(activityBookingRepository.save(any(ActivityBooking.class))).thenAnswer(inv -> inv.getArgument(0));

        ActivityBookingResponse response = service.cancelBooking(booking.getId());

        assertThat(response.status()).isEqualTo(ActivityBookingStatus.CANCELLED);
    }

    @Test
    void travelerCannotCancelAnotherTravelersBooking() {
        setUp();
        User otherTraveler = travelerUser(OTHER_TRAVELER_ID, "other@travelease.com");
        when(userRepository.findByEmail("traveler@travelease.com"))
                .thenReturn(Optional.of(travelerUser(TRAVELER_ID, "traveler@travelease.com")));
        authenticateAs("traveler@travelease.com", "ROLE_TRAVELER");

        Activity activity = activityOwnedBy(PROVIDER_201);
        ActivitySlot slot = futureSlot(activity, 10);
        ActivityBooking booking = confirmedBookingOf(slot, otherTraveler, 2);
        when(activityBookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.cancelBooking(booking.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void cannotCancelAlreadyCancelledBooking() {
        setUp();
        User traveler = travelerUser(TRAVELER_ID, "traveler@travelease.com");
        when(userRepository.findByEmail(traveler.getEmail())).thenReturn(Optional.of(traveler));
        authenticateAs(traveler.getEmail(), "ROLE_TRAVELER");

        Activity activity = activityOwnedBy(PROVIDER_201);
        ActivitySlot slot = futureSlot(activity, 10);
        ActivityBooking booking = confirmedBookingOf(slot, traveler, 2);
        booking.setStatus(ActivityBookingStatus.CANCELLED);
        when(activityBookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.cancelBooking(booking.getId()))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void cannotCancelBookingAfterActivityHasStarted() {
        setUp();
        User traveler = travelerUser(TRAVELER_ID, "traveler@travelease.com");
        when(userRepository.findByEmail(traveler.getEmail())).thenReturn(Optional.of(traveler));
        authenticateAs(traveler.getEmail(), "ROLE_TRAVELER");

        Activity activity = activityOwnedBy(PROVIDER_201);
        ActivitySlot slot = pastSlot(activity, 10);
        ActivityBooking booking = confirmedBookingOf(slot, traveler, 2);
        when(activityBookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.cancelBooking(booking.getId()))
                .isInstanceOf(InvalidRequestException.class);
    }

    // --- provider visibility / attendance ---

    @Test
    void activityProviderCanViewBookingsForOwnActivity() {
        setUp();
        User provider = activityProviderUser("activityprovider1@travelease.com", PROVIDER_201);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_ACTIVITY_PROVIDER");

        Activity activity = activityOwnedBy(PROVIDER_201);
        when(activityRepository.findById(activity.getActivityId())).thenReturn(Optional.of(activity));
        ActivitySlot slot = futureSlot(activity, 10);
        User traveler = travelerUser(TRAVELER_ID, "traveler@travelease.com");
        ActivityBooking booking = confirmedBookingOf(slot, traveler, 2);
        when(activityBookingRepository.findByActivitySlot_Activity_ActivityId(activity.getActivityId()))
                .thenReturn(List.of(booking));

        List<ActivityBookingResponse> responses = service.getBookingsForActivity(activity.getActivityId());

        assertThat(responses).hasSize(1);
    }

    @Test
    void activityProviderCannotViewBookingsForAnotherProvidersActivity() {
        setUp();
        User provider = activityProviderUser("activityprovider1@travelease.com", PROVIDER_201);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_ACTIVITY_PROVIDER");

        Activity othersActivity = activityOwnedBy(PROVIDER_202);
        when(activityRepository.findById(othersActivity.getActivityId())).thenReturn(Optional.of(othersActivity));

        assertThatThrownBy(() -> service.getBookingsForActivity(othersActivity.getActivityId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void hotelOrTransportProviderCannotViewActivityBookings() {
        setUp();
        authenticateAs("hotelprovider1@travelease.com", "ROLE_HOTEL_PROVIDER");

        Activity activity = activityOwnedBy(PROVIDER_201);
        when(activityRepository.findById(activity.getActivityId())).thenReturn(Optional.of(activity));

        assertThatThrownBy(() -> service.getBookingsForActivity(activity.getActivityId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void activityProviderCanMarkAttendanceAfterSlotStarts() {
        setUp();
        User provider = activityProviderUser("activityprovider1@travelease.com", PROVIDER_201);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_ACTIVITY_PROVIDER");

        Activity activity = activityOwnedBy(PROVIDER_201);
        ActivitySlot slot = pastSlot(activity, 10);
        User traveler = travelerUser(TRAVELER_ID, "traveler@travelease.com");
        ActivityBooking booking = confirmedBookingOf(slot, traveler, 2);
        when(activityBookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(activityBookingRepository.save(any(ActivityBooking.class))).thenAnswer(inv -> inv.getArgument(0));

        ActivityBookingResponse response = service.markAttendance(booking.getId(), ActivityBookingStatus.ATTENDED);

        assertThat(response.status()).isEqualTo(ActivityBookingStatus.ATTENDED);
    }

    @Test
    void attendanceCannotBeMarkedBeforeSlotStarts() {
        setUp();
        User provider = activityProviderUser("activityprovider1@travelease.com", PROVIDER_201);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_ACTIVITY_PROVIDER");

        Activity activity = activityOwnedBy(PROVIDER_201);
        ActivitySlot slot = futureSlot(activity, 10);
        User traveler = travelerUser(TRAVELER_ID, "traveler@travelease.com");
        ActivityBooking booking = confirmedBookingOf(slot, traveler, 2);
        when(activityBookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.markAttendance(booking.getId(), ActivityBookingStatus.ATTENDED))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void attendanceCannotBeSetToCancelledThroughProviderEndpoint() {
        setUp();
        authenticateAs("activityprovider1@travelease.com", "ROLE_ACTIVITY_PROVIDER");

        assertThatThrownBy(() -> service.markAttendance(UUID.randomUUID(), ActivityBookingStatus.CANCELLED))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void anotherProviderCannotMarkAttendanceOnDifferentProvidersBooking() {
        setUp();
        User provider = activityProviderUser("activityprovider2@travelease.com", PROVIDER_202);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_ACTIVITY_PROVIDER");

        Activity activity = activityOwnedBy(PROVIDER_201);
        ActivitySlot slot = pastSlot(activity, 10);
        User traveler = travelerUser(TRAVELER_ID, "traveler@travelease.com");
        ActivityBooking booking = confirmedBookingOf(slot, traveler, 2);
        when(activityBookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.markAttendance(booking.getId(), ActivityBookingStatus.ATTENDED))
                .isInstanceOf(AccessDeniedException.class);
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
