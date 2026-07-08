package com.travelease.backend.itinerary.service;

import com.travelease.backend.auth.entity.User;
import com.travelease.backend.auth.repository.UserRepository;
import com.travelease.backend.busbooking.exception.SeatUnavailableException;
import com.travelease.backend.busbooking.security.SecurityUtil;
import com.travelease.backend.itinerary.dto.ActivityBookingResponse;
import com.travelease.backend.itinerary.dto.ActivitySlotResponse;
import com.travelease.backend.itinerary.dto.AttachActivityBookingRequest;
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
import com.travelease.backend.trip.entity.Trip;
import com.travelease.backend.trip.repository.TripRepository;
import com.travelease.backend.trip.security.TripAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityBookingServiceImpl implements ActivityBookingService {

    private static final List<ActivityBookingStatus> CAPACITY_CONSUMING_STATUSES =
            List.of(ActivityBookingStatus.CONFIRMED, ActivityBookingStatus.ATTENDED, ActivityBookingStatus.NO_SHOW);

    /**
     * Statuses eligible for a NEW Trip attachment - mirrors busbooking's
     * TRIP_ATTACHABLE_STATUSES (CONFIRMED or COMPLETED, never CANCELLED),
     * extended to this domain's two "the service occurred" terminal statuses
     * (ATTENDED/NO_SHOW are this domain's equivalent of Bus's COMPLETED).
     */
    private static final Set<ActivityBookingStatus> TRIP_ATTACHABLE_STATUSES =
            Set.of(ActivityBookingStatus.CONFIRMED, ActivityBookingStatus.ATTENDED, ActivityBookingStatus.NO_SHOW);

    private final ActivityBookingRepository activityBookingRepository;
    private final ActivitySlotRepository activitySlotRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final TripAuthorizationService tripAuthorizationService;
    private final SecurityUtil securityUtil;

    @Override
    @Transactional(readOnly = true)
    public List<ActivitySlotResponse> getBookableSlots(String activityId) {
        activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity with id " + activityId + " not found"));

        LocalDateTime now = LocalDateTime.now();
        return activitySlotRepository.findByActivity_ActivityId(activityId).stream()
                .filter(slot -> LocalDateTime.of(slot.getActivityDate(), slot.getStartTime()).isAfter(now))
                .map(this::toSlotResponse)
                .toList();
    }

    @Override
    @Transactional
    public ActivityBookingResponse createBooking(CreateActivityBookingRequest request) {
        // Pessimistic lock acquired first so the capacity sum computed below is
        // authoritative against any other concurrent booking for this slot.
        ActivitySlot slot = activitySlotRepository.findByIdForUpdate(request.activitySlotId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Activity slot with id " + request.activitySlotId() + " not found"));

        LocalDateTime slotStart = LocalDateTime.of(slot.getActivityDate(), slot.getStartTime());
        if (slotStart.isBefore(LocalDateTime.now())) {
            throw new InvalidRequestException("Cannot book a slot that has already started");
        }

        int alreadyBooked = activityBookingRepository.sumParticipantsByActivitySlotIdAndStatusIn(
                slot.getId(), CAPACITY_CONSUMING_STATUSES);
        if (alreadyBooked + request.participantCount() > slot.getCapacity()) {
            throw new SeatUnavailableException(
                    "Only " + (slot.getCapacity() - alreadyBooked) + " participant slot(s) remaining for this activity slot");
        }

        UUID currentUserId = securityUtil.getCurrentUserId();
        User currentUser = userRepository.getReferenceById(currentUserId);

        ActivityBooking booking = new ActivityBooking();
        booking.setActivitySlot(slot);
        booking.setBookedBy(currentUser);
        booking.setParticipantCount(request.participantCount());
        booking.setPricePerParticipant(slot.getPrice());
        booking.setTotalAmount(slot.getPrice().multiply(BigDecimal.valueOf(request.participantCount())));
        booking.setStatus(ActivityBookingStatus.CONFIRMED);

        // saveAndFlush (not save): @CreationTimestamp is only populated by
        // Hibernate when the INSERT is actually generated, which a deferred
        // flush would otherwise push past this method's return, leaving
        // bookedAt null in the response DTO built below.
        return toResponse(activityBookingRepository.saveAndFlush(booking));
    }

    @Override
    @Transactional
    public ActivityBookingResponse cancelBooking(UUID bookingId) {
        ActivityBooking booking = getBooking(bookingId);
        assertOwnsBooking(booking);

        if (booking.getStatus() != ActivityBookingStatus.CONFIRMED) {
            throw new InvalidRequestException(
                    "Only a CONFIRMED booking can be cancelled (current status: " + booking.getStatus() + ")");
        }

        ActivitySlot slot = booking.getActivitySlot();
        LocalDateTime slotStart = LocalDateTime.of(slot.getActivityDate(), slot.getStartTime());
        if (slotStart.isBefore(LocalDateTime.now())) {
            throw new InvalidRequestException("Cannot cancel a booking after the activity has started");
        }

        booking.setStatus(ActivityBookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        return toResponse(activityBookingRepository.save(booking));
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityBookingResponse getMyBooking(UUID bookingId) {
        ActivityBooking booking = getBooking(bookingId);
        assertOwnsBooking(booking);
        return toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityBookingResponse> getMyBookings() {
        UUID currentUserId = securityUtil.getCurrentUserId();
        return activityBookingRepository.findByBookedBy_Id(currentUserId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityBookingResponse> getBookingsForActivity(String activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity with id " + activityId + " not found"));
        securityUtil.resolveEffectiveActivityProviderId(activity.getProviderId());

        return activityBookingRepository.findByActivitySlot_Activity_ActivityId(activityId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityBookingResponse getProviderBooking(UUID bookingId) {
        ActivityBooking booking = getBooking(bookingId);
        assertProviderOwnsBooking(booking);
        return toResponse(booking);
    }

    @Override
    @Transactional
    public ActivityBookingResponse markAttendance(UUID bookingId, ActivityBookingStatus status) {
        if (status != ActivityBookingStatus.ATTENDED && status != ActivityBookingStatus.NO_SHOW) {
            throw new InvalidRequestException("Attendance can only be marked as ATTENDED or NO_SHOW");
        }

        ActivityBooking booking = getBooking(bookingId);
        assertProviderOwnsBooking(booking);

        if (booking.getStatus() != ActivityBookingStatus.CONFIRMED) {
            throw new InvalidRequestException(
                    "Only a CONFIRMED booking can have attendance marked (current status: " + booking.getStatus() + ")");
        }

        ActivitySlot slot = booking.getActivitySlot();
        LocalDateTime slotStart = LocalDateTime.of(slot.getActivityDate(), slot.getStartTime());
        if (slotStart.isAfter(LocalDateTime.now())) {
            throw new InvalidRequestException("Attendance cannot be marked before the activity slot starts");
        }

        booking.setStatus(status);
        booking.setAttendanceMarkedAt(LocalDateTime.now());
        return toResponse(activityBookingRepository.save(booking));
    }

    // ────────────────────────────────────────────────────────────────
    // TRAVELER TRIP INTEGRATION - attach/detach/list only
    // ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ActivityBookingResponse attachBookingToTrip(UUID tripId, AttachActivityBookingRequest request) {
        Trip trip = getTrip(tripId);
        requireTripAccess(trip);
        tripAuthorizationService.requireMutableTrip(trip);

        ActivityBooking booking = getBooking(request.bookingId());
        // Trip access alone is not enough: the caller must also own the
        // ActivityBooking being attached, otherwise an accepted member could
        // attach another traveler's private booking merely by knowing its id.
        assertOwnsBooking(booking);

        if (booking.getTripId() != null && booking.getTripId().equals(tripId)) {
            // Already attached to this exact trip - idempotent, not an error.
            return toResponse(booking);
        }
        if (booking.getTripId() != null) {
            throw new InvalidRequestException("Activity booking is already attached to a different trip");
        }
        if (!TRIP_ATTACHABLE_STATUSES.contains(booking.getStatus())) {
            throw new InvalidRequestException(
                    "Activity booking status " + booking.getStatus() + " cannot be attached to a trip; "
                            + "only CONFIRMED, ATTENDED, or NO_SHOW bookings are eligible");
        }

        booking.setTripId(tripId);
        return toResponse(activityBookingRepository.save(booking));
    }

    @Override
    @Transactional
    public void removeBookingFromTrip(UUID tripId, UUID bookingId) {
        Trip trip = getTrip(tripId);
        requireTripAccess(trip);
        tripAuthorizationService.requireMutableTrip(trip);

        ActivityBooking booking = getBooking(bookingId);
        // Deliberately owner-only (not organizer-or-owner): organizer status is
        // a Trip-level authority, not a Booking-level one - matches the Bus
        // Booking Trip-integration precedent rather than Hotel's looser
        // organizer-or-owner detach rule (see Phase 3 decision record).
        assertOwnsBooking(booking);

        if (!tripId.equals(booking.getTripId())) {
            throw new InvalidRequestException("Activity booking is not attached to trip " + tripId);
        }

        booking.setTripId(null);
        activityBookingRepository.save(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityBookingResponse> getTripActivityBookings(UUID tripId) {
        Trip trip = getTrip(tripId);
        requireTripAccess(trip);

        return activityBookingRepository.findByTripId(tripId).stream()
                .map(this::toResponse)
                .toList();
    }

    private Trip getTrip(UUID tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip with id " + tripId + " not found"));
    }

    private void requireTripAccess(Trip trip) {
        UUID currentUserId = securityUtil.getCurrentUserId();
        boolean isAdmin = securityUtil.getCurrentUserRoles().contains("ROLE_ADMIN");
        tripAuthorizationService.requireMember(trip, currentUserId, isAdmin);
    }

    private ActivityBooking getBooking(UUID bookingId) {
        return activityBookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity booking with id " + bookingId + " not found"));
    }

    private void assertOwnsBooking(ActivityBooking booking) {
        UUID currentUserId = securityUtil.getCurrentUserId();
        if (!booking.getBookedBy().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not own this activity booking");
        }
    }

    /**
     * Ownership is always derived through activitySlot -> activity -> providerId
     * (see ActivityBooking's javadoc) rather than a duplicated column - mirrors
     * ActivityProviderServiceImpl.assertOwnsActivity's use of the same resolver.
     */
    private void assertProviderOwnsBooking(ActivityBooking booking) {
        Activity activity = booking.getActivitySlot().getActivity();
        securityUtil.resolveEffectiveActivityProviderId(activity.getProviderId());
    }

    private ActivitySlotResponse toSlotResponse(ActivitySlot slot) {
        int bookedParticipants = activityBookingRepository.sumParticipantsByActivitySlotIdAndStatusIn(
                slot.getId(), CAPACITY_CONSUMING_STATUSES);
        return new ActivitySlotResponse(
                slot.getId(),
                slot.getActivity().getActivityId(),
                slot.getActivityDate(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getPrice(),
                slot.getCapacity(),
                slot.getCapacity() - bookedParticipants
        );
    }

    private ActivityBookingResponse toResponse(ActivityBooking booking) {
        ActivitySlot slot = booking.getActivitySlot();
        Activity activity = slot.getActivity();
        return new ActivityBookingResponse(
                booking.getId(),
                slot.getId(),
                activity.getActivityId(),
                activity.getActivityName(),
                slot.getActivityDate(),
                slot.getStartTime(),
                slot.getEndTime(),
                booking.getParticipantCount(),
                booking.getPricePerParticipant(),
                booking.getTotalAmount(),
                booking.getStatus(),
                booking.getBookedAt(),
                booking.getBookedBy().getId()
        );
    }
}
