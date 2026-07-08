package com.travelease.backend.itinerary.service;

import com.travelease.backend.auth.entity.Role;
import com.travelease.backend.auth.entity.User;
import com.travelease.backend.auth.repository.UserRepository;
import com.travelease.backend.busbooking.security.SecurityUtil;
import com.travelease.backend.itinerary.dto.ActivityProviderRequest;
import com.travelease.backend.itinerary.dto.ActivitySlotRequest;
import com.travelease.backend.itinerary.entity.Activity;
import com.travelease.backend.itinerary.entity.ActivitySlot;
import com.travelease.backend.itinerary.repository.ActivityBookingRepository;
import com.travelease.backend.itinerary.repository.ActivityRepository;
import com.travelease.backend.itinerary.repository.ActivitySlotRepository;
import com.travelease.backend.shared.exception.InvalidRequestException;
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
 * Activity Provider tenant-isolation tests. Uses a real SecurityUtil (backed by
 * a mocked UserRepository) rather than mocking SecurityUtil itself, matching
 * the AccommodationServiceImplHotelProviderOwnershipTest convention - the point
 * is to verify the actual role-gate + ownership-assertion wiring.
 */
@ExtendWith(MockitoExtension.class)
class ActivityProviderServiceImplOwnershipTest {

    private static final Long PROVIDER_201 = 201L;
    private static final Long PROVIDER_202 = 202L;

    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private ActivitySlotRepository activitySlotRepository;
    @Mock
    private ActivityBookingRepository activityBookingRepository;
    @Mock
    private UserRepository userRepository;

    private ActivityProviderServiceImpl service;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void setUp() {
        SecurityUtil securityUtil = new SecurityUtil(userRepository);
        service = new ActivityProviderServiceImpl(activityRepository, activitySlotRepository, activityBookingRepository, securityUtil);
    }

    private void authenticateAs(String email, String role) {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, authorities));
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
        activity.setDestinationId(1);
        activity.setActivityName("Test Activity " + providerId);
        activity.setDurationHours(2.0);
        activity.setStartTime("09:00");
        activity.setEndTime("11:00");
        return activity;
    }

    private ActivitySlot slotOf(Activity activity) {
        ActivitySlot slot = new ActivitySlot();
        slot.setActivity(activity);
        slot.setActivityDate(LocalDate.now().plusDays(5));
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setPrice(new BigDecimal("500.00"));
        slot.setCapacity(10);
        return slot;
    }

    private ActivityProviderRequest requestFor(Long providerId) {
        return new ActivityProviderRequest(1, "Updated Activity", 2.0, "09:00", "11:00", "desc", providerId);
    }

    private ActivitySlotRequest slotRequest() {
        return new ActivitySlotRequest(LocalDate.now().plusDays(6), LocalTime.of(10, 0), LocalTime.of(12, 0),
                new BigDecimal("600.00"), 8);
    }

    // --- createActivity ---

    @Test
    void activityProviderCreatesActivityWithOwnProviderIdAssignedServerSide() {
        setUp();
        User provider = activityProviderUser("activityprovider1@travelease.com", PROVIDER_201);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_ACTIVITY_PROVIDER");
        when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.createActivity(requestFor(null));

        assertThat(response.providerId()).isEqualTo(PROVIDER_201);
    }

    @Test
    void activityProviderCannotCreateActivityForAnotherProviderId() {
        setUp();
        User provider = activityProviderUser("activityprovider1@travelease.com", PROVIDER_201);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_ACTIVITY_PROVIDER");

        assertThatThrownBy(() -> service.createActivity(requestFor(PROVIDER_202)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void adminCreatingActivityWithoutExplicitProviderIdIsRejectedRatherThanDefaulted() {
        setUp();
        authenticateAs("admin@travelease.com", "ROLE_ADMIN");

        assertThatThrownBy(() -> service.createActivity(requestFor(null)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void adminCreatingActivityWithExplicitProviderIdWorks() {
        setUp();
        authenticateAs("admin@travelease.com", "ROLE_ADMIN");
        when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.createActivity(requestFor(PROVIDER_201));

        assertThat(response.providerId()).isEqualTo(PROVIDER_201);
    }

    // --- activity ownership: get/update ---

    @Test
    void activityProviderCanAccessOwnActivity() {
        setUp();
        User provider = activityProviderUser("activityprovider1@travelease.com", PROVIDER_201);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_ACTIVITY_PROVIDER");
        Activity activity = activityOwnedBy(PROVIDER_201);
        when(activityRepository.findById(activity.getActivityId())).thenReturn(Optional.of(activity));

        var response = service.getProviderActivity(activity.getActivityId());

        assertThat(response.activityId()).isEqualTo(activity.getActivityId());
    }

    @Test
    void activityProviderCannotAccessAnotherProvidersActivity() {
        setUp();
        User provider = activityProviderUser("activityprovider1@travelease.com", PROVIDER_201);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_ACTIVITY_PROVIDER");
        Activity othersActivity = activityOwnedBy(PROVIDER_202);
        when(activityRepository.findById(othersActivity.getActivityId())).thenReturn(Optional.of(othersActivity));

        assertThatThrownBy(() -> service.getProviderActivity(othersActivity.getActivityId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void activityProviderCannotUpdateAnotherProvidersActivity() {
        setUp();
        User provider = activityProviderUser("activityprovider1@travelease.com", PROVIDER_201);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_ACTIVITY_PROVIDER");
        Activity othersActivity = activityOwnedBy(PROVIDER_202);
        when(activityRepository.findById(othersActivity.getActivityId())).thenReturn(Optional.of(othersActivity));

        assertThatThrownBy(() -> service.updateActivity(othersActivity.getActivityId(), requestFor(null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void adminBypassesActivityOwnershipForGetAndUpdate() {
        setUp();
        authenticateAs("admin@travelease.com", "ROLE_ADMIN");
        Activity activity = activityOwnedBy(PROVIDER_201);
        when(activityRepository.findById(activity.getActivityId())).thenReturn(Optional.of(activity));
        when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));

        var details = service.getProviderActivity(activity.getActivityId());
        assertThat(details.activityId()).isEqualTo(activity.getActivityId());

        var updated = service.updateActivity(activity.getActivityId(), requestFor(null));
        assertThat(updated.activityName()).isEqualTo("Updated Activity");
    }

    // --- slot ownership through activity ---

    @Test
    void activityProviderCanCreateSlotUnderOwnActivity() {
        setUp();
        User provider = activityProviderUser("activityprovider1@travelease.com", PROVIDER_201);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_ACTIVITY_PROVIDER");
        Activity activity = activityOwnedBy(PROVIDER_201);
        when(activityRepository.findById(activity.getActivityId())).thenReturn(Optional.of(activity));
        when(activitySlotRepository.save(any(ActivitySlot.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.createSlot(activity.getActivityId(), slotRequest());

        assertThat(response.activityId()).isEqualTo(activity.getActivityId());
    }

    @Test
    void activityProviderCannotCreateSlotUnderAnotherProvidersActivity() {
        setUp();
        User provider = activityProviderUser("activityprovider1@travelease.com", PROVIDER_201);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_ACTIVITY_PROVIDER");
        Activity othersActivity = activityOwnedBy(PROVIDER_202);
        when(activityRepository.findById(othersActivity.getActivityId())).thenReturn(Optional.of(othersActivity));

        assertThatThrownBy(() -> service.createSlot(othersActivity.getActivityId(), slotRequest()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void activityProviderCanReadOwnActivitySlots() {
        setUp();
        User provider = activityProviderUser("activityprovider1@travelease.com", PROVIDER_201);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_ACTIVITY_PROVIDER");
        Activity activity = activityOwnedBy(PROVIDER_201);
        when(activityRepository.findById(activity.getActivityId())).thenReturn(Optional.of(activity));
        when(activitySlotRepository.findByActivity_ActivityId(activity.getActivityId()))
                .thenReturn(List.of(slotOf(activity)));

        var slots = service.getSlots(activity.getActivityId());

        assertThat(slots).hasSize(1);
    }

    @Test
    void activityProviderCannotReadAnotherProvidersActivitySlots() {
        setUp();
        User provider = activityProviderUser("activityprovider1@travelease.com", PROVIDER_201);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_ACTIVITY_PROVIDER");
        Activity othersActivity = activityOwnedBy(PROVIDER_202);
        when(activityRepository.findById(othersActivity.getActivityId())).thenReturn(Optional.of(othersActivity));

        assertThatThrownBy(() -> service.getSlots(othersActivity.getActivityId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void activityProviderCanUpdateOwnSlot() {
        setUp();
        User provider = activityProviderUser("activityprovider1@travelease.com", PROVIDER_201);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_ACTIVITY_PROVIDER");
        Activity activity = activityOwnedBy(PROVIDER_201);
        ActivitySlot slot = slotOf(activity);
        UUID slotId = UUID.randomUUID();
        setSlotId(slot, slotId);
        when(activityRepository.findById(activity.getActivityId())).thenReturn(Optional.of(activity));
        when(activitySlotRepository.findById(slotId)).thenReturn(Optional.of(slot));
        when(activitySlotRepository.save(any(ActivitySlot.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.updateSlot(activity.getActivityId(), slotId, slotRequest());

        assertThat(response.capacity()).isEqualTo(8);
    }

    @Test
    void activityProviderCannotUpdateAnotherProvidersSlot() {
        setUp();
        User provider = activityProviderUser("activityprovider1@travelease.com", PROVIDER_201);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_ACTIVITY_PROVIDER");
        Activity othersActivity = activityOwnedBy(PROVIDER_202);
        UUID slotId = UUID.randomUUID();
        when(activityRepository.findById(othersActivity.getActivityId())).thenReturn(Optional.of(othersActivity));

        assertThatThrownBy(() -> service.updateSlot(othersActivity.getActivityId(), slotId, slotRequest()))
                .isInstanceOf(AccessDeniedException.class);
    }

    // --- slot-update safety: capacity floor + date/time lock once bookings exist ---

    @Test
    void slotCapacityCannotBeReducedBelowAlreadyBookedParticipants() {
        setUp();
        User provider = activityProviderUser("activityprovider1@travelease.com", PROVIDER_201);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_ACTIVITY_PROVIDER");
        Activity activity = activityOwnedBy(PROVIDER_201);
        ActivitySlot slot = slotOf(activity);
        slot.setCapacity(10);
        UUID slotId = UUID.randomUUID();
        setSlotId(slot, slotId);
        when(activityRepository.findById(activity.getActivityId())).thenReturn(Optional.of(activity));
        when(activitySlotRepository.findById(slotId)).thenReturn(Optional.of(slot));
        when(activityBookingRepository.sumParticipantsByActivitySlotIdAndStatusIn(eq(slotId), anyList()))
                .thenReturn(6);

        ActivitySlotRequest reducedCapacityBelowBooked = new ActivitySlotRequest(
                slot.getActivityDate(), slot.getStartTime(), slot.getEndTime(), new BigDecimal("600.00"), 5);

        assertThatThrownBy(() -> service.updateSlot(activity.getActivityId(), slotId, reducedCapacityBelowBooked))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void slotDateTimeCannotBeChangedOnceBookingsExist() {
        setUp();
        User provider = activityProviderUser("activityprovider1@travelease.com", PROVIDER_201);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_ACTIVITY_PROVIDER");
        Activity activity = activityOwnedBy(PROVIDER_201);
        ActivitySlot slot = slotOf(activity);
        UUID slotId = UUID.randomUUID();
        setSlotId(slot, slotId);
        when(activityRepository.findById(activity.getActivityId())).thenReturn(Optional.of(activity));
        when(activitySlotRepository.findById(slotId)).thenReturn(Optional.of(slot));
        when(activityBookingRepository.sumParticipantsByActivitySlotIdAndStatusIn(eq(slotId), anyList()))
                .thenReturn(2);

        ActivitySlotRequest changedDate = new ActivitySlotRequest(
                slot.getActivityDate().plusDays(1), slot.getStartTime(), slot.getEndTime(),
                new BigDecimal("600.00"), slot.getCapacity());

        assertThatThrownBy(() -> service.updateSlot(activity.getActivityId(), slotId, changedDate))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void slotPriceAndCapacityIncreaseAllowedWhenBookingsExist() {
        setUp();
        User provider = activityProviderUser("activityprovider1@travelease.com", PROVIDER_201);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_ACTIVITY_PROVIDER");
        Activity activity = activityOwnedBy(PROVIDER_201);
        ActivitySlot slot = slotOf(activity);
        slot.setCapacity(10);
        UUID slotId = UUID.randomUUID();
        setSlotId(slot, slotId);
        when(activityRepository.findById(activity.getActivityId())).thenReturn(Optional.of(activity));
        when(activitySlotRepository.findById(slotId)).thenReturn(Optional.of(slot));
        when(activityBookingRepository.sumParticipantsByActivitySlotIdAndStatusIn(eq(slotId), anyList()))
                .thenReturn(6);
        when(activitySlotRepository.save(any(ActivitySlot.class))).thenAnswer(inv -> inv.getArgument(0));

        ActivitySlotRequest sameDateHigherCapacity = new ActivitySlotRequest(
                slot.getActivityDate(), slot.getStartTime(), slot.getEndTime(), new BigDecimal("750.00"), 12);

        var response = service.updateSlot(activity.getActivityId(), slotId, sameDateHigherCapacity);

        assertThat(response.price()).isEqualByComparingTo("750.00");
        assertThat(response.capacity()).isEqualTo(12);
    }

    // --- transport / hotel provider must not gain Activity Provider access ---

    @Test
    void transportProviderCannotAccessActivityProviderManagement() {
        setUp();
        authenticateAs("provider1@travelease.com", "ROLE_PROVIDER");
        Activity activity = activityOwnedBy(PROVIDER_201);
        when(activityRepository.findById(activity.getActivityId())).thenReturn(Optional.of(activity));

        assertThatThrownBy(() -> service.getProviderActivity(activity.getActivityId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void hotelProviderCannotAccessActivityProviderManagement() {
        setUp();
        authenticateAs("hotelprovider1@travelease.com", "ROLE_HOTEL_PROVIDER");
        Activity activity = activityOwnedBy(PROVIDER_201);
        when(activityRepository.findById(activity.getActivityId())).thenReturn(Optional.of(activity));

        assertThatThrownBy(() -> service.getProviderActivity(activity.getActivityId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    private void setSlotId(ActivitySlot slot, UUID id) {
        try {
            var field = com.travelease.backend.shared.entity.BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(slot, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
