package com.travelease.backend.trip.security;

import com.travelease.backend.shared.exception.InvalidRequestException;
import com.travelease.backend.trip.entity.Trip;
import com.travelease.backend.trip.entity.TravelerTripStatus;
import com.travelease.backend.trip.entity.TripMemberStatus;
import com.travelease.backend.trip.repository.TripMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Central authority for trip-level ownership decisions (Layer 2 business
 * authorization). Organizer/member status is derived from Trip.organizer and
 * TripMember rows, never from a JWT role, since the same ROLE_TRAVELER user can
 * be organizer of one trip and a member of another.
 */
@Component
@RequiredArgsConstructor
public class TripAuthorizationService {

    private final TripMemberRepository tripMemberRepository;

    public boolean isOrganizer(Trip trip, UUID userId) {
        return trip.getOrganizer().getId().equals(userId);
    }

    public boolean isMember(Trip trip, UUID userId) {
        return isOrganizer(trip, userId)
                || tripMemberRepository.existsByTripIdAndUserIdAndMemberStatus(trip.getId(), userId, TripMemberStatus.ACCEPTED);
    }

    public void requireOrganizer(Trip trip, UUID userId, boolean isAdmin) {
        if (isAdmin || isOrganizer(trip, userId)) {
            return;
        }
        throw new AccessDeniedException("Only the trip organizer can perform this action");
    }

    public void requireMember(Trip trip, UUID userId, boolean isAdmin) {
        if (isAdmin || isMember(trip, userId)) {
            return;
        }
        throw new AccessDeniedException("Current user is not authorized to access this trip");
    }

    /**
     * Lifecycle-state gate for mutations across Budget/Expense/Itinerary/
     * membership/Bus-Booking-attachment/Hotel-Booking-attachment: once a Trip is
     * COMPLETED or CANCELLED it is historical and closed to further planning
     * mutation. Unlike requireOrganizer/requireMember (which answer "is this
     * caller allowed"), this answers "is this Trip's data still open to
     * mutation at all" - a fact about the Trip's own state, not about who is
     * asking - so, deliberately, there is no Admin bypass here: nobody adds a
     * new expense or re-attaches a booking to a trip that has already closed.
     * Settlement's markPaid/read-recalculation is intentionally NOT gated by
     * this - paying off an already-incurred debt is expected to happen after a
     * trip ends, and recalculation never creates new financial obligations.
     */
    public void requireMutableTrip(Trip trip) {
        if (trip.getStatus() == TravelerTripStatus.COMPLETED || trip.getStatus() == TravelerTripStatus.CANCELLED) {
            throw new InvalidRequestException(
                    "Trip is " + trip.getStatus() + " and no longer accepts this action");
        }
    }
}
