package com.travelease.backend.trip.mapper;

import com.travelease.backend.auth.entity.User;
import com.travelease.backend.trip.dto.PendingInvitationResponse;
import com.travelease.backend.trip.dto.TripMemberResponse;
import com.travelease.backend.trip.dto.TripOrganizerSummary;
import com.travelease.backend.trip.dto.TripResponse;
import com.travelease.backend.trip.entity.Trip;
import com.travelease.backend.trip.entity.TripMember;
import org.springframework.stereotype.Component;

/**
 * Maps the traveler-planning Trip domain (trip.entity.Trip / trip_members).
 * Named distinctly from busbooking.mapper.TripMapper (which maps the unrelated
 * BusBooking operational Trip / bus_trips) to avoid a Spring bean-name collision
 * and to keep the two Trip domains unambiguous, per the project's domain split.
 */
@Component
public class TravelerTripMapper {

    public TripResponse toResponse(Trip trip, String viewerRole) {
        User organizer = trip.getOrganizer();
        TripOrganizerSummary organizerSummary = new TripOrganizerSummary(
                organizer.getId(), organizer.getName(), organizer.getEmail());

        return new TripResponse(
                trip.getId(),
                trip.getTripName(),
                organizerSummary,
                trip.getSourceLocation(),
                trip.getDestinationId(),
                trip.getBudgetAmount(),
                trip.getCategoryId(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getStatus(),
                viewerRole,
                trip.getCreatedAt(),
                trip.getUpdatedAt()
        );
    }

    public TripMemberResponse toMemberResponse(TripMember member) {
        User user = member.getUser();
        return new TripMemberResponse(
                member.getId(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                member.getMemberStatus(),
                member.getJoinedDate(),
                member.getBudgetAmount(),
                member.getSpentAmount()
        );
    }

    public PendingInvitationResponse toPendingInvitationResponse(TripMember member) {
        Trip trip = member.getTrip();
        User organizer = trip.getOrganizer();
        TripOrganizerSummary organizerSummary = new TripOrganizerSummary(
                organizer.getId(), organizer.getName(), organizer.getEmail());

        return new PendingInvitationResponse(
                member.getId(),
                trip.getId(),
                trip.getTripName(),
                organizerSummary,
                trip.getSourceLocation(),
                trip.getStartDate(),
                trip.getEndDate(),
                member.getMemberStatus()
        );
    }
}
