package com.travelease.backend.trip.service;

import com.travelease.backend.auth.entity.Role;
import com.travelease.backend.auth.entity.User;
import com.travelease.backend.auth.repository.UserRepository;
import com.travelease.backend.shared.exception.InvalidRequestException;
import com.travelease.backend.shared.exception.ResourceNotFoundException;
import com.travelease.backend.trip.dto.AddTripMemberRequest;
import com.travelease.backend.trip.dto.CreateTripRequest;
import com.travelease.backend.trip.dto.PendingInvitationResponse;
import com.travelease.backend.trip.dto.TripMemberResponse;
import com.travelease.backend.trip.dto.TripResponse;
import com.travelease.backend.trip.dto.TripStatusTransitionRequest;
import com.travelease.backend.trip.dto.UpdateTripRequest;
import com.travelease.backend.trip.entity.Trip;
import com.travelease.backend.trip.entity.TravelerTripStatus;
import com.travelease.backend.trip.entity.TripMember;
import com.travelease.backend.trip.entity.TripMemberStatus;
import com.travelease.backend.trip.mapper.TravelerTripMapper;
import com.travelease.backend.trip.repository.TripMemberRepository;
import com.travelease.backend.trip.repository.TripRepository;
import com.travelease.backend.trip.security.TripAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Named distinctly from busbooking.service.impl.TripServiceImpl (which serves
 * the unrelated BusBooking operational Trip) to avoid a Spring bean-name
 * collision and to keep the two Trip domains unambiguous.
 */
@Service
@RequiredArgsConstructor
public class TravelerTripServiceImpl implements TripService {

    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final UserRepository userRepository;
    private final TripAuthorizationService tripAuthorizationService;
    private final TravelerTripMapper tripMapper;

    @Override
    @Transactional
    public TripResponse createTrip(CreateTripRequest request, String currentUserEmail) {
        requireValidDateRange(request.startDate(), request.endDate());
        User organizer = resolveCurrentUser(currentUserEmail);

        Trip trip = new Trip();
        trip.setTripName(request.tripName());
        trip.setOrganizer(organizer);
        trip.setSourceLocation(request.sourceLocation());
        trip.setDestinationId(request.destinationId());
        trip.setBudgetAmount(request.budgetAmount());
        trip.setCategoryId(request.categoryId());
        trip.setStartDate(request.startDate());
        trip.setEndDate(request.endDate());
        trip.setStatus(TravelerTripStatus.PLANNING);
        Trip saved = tripRepository.save(trip);

        // The organizer is also recorded as a TripMember (ACCEPTED) so that the
        // pre-existing Budget/Expense/Settlement membership checks -
        // existsByTripIdAndUserEmail - already recognize the organizer, exactly
        // like DemoDataInitializer's seed trip does.
        TripMember organizerMembership = new TripMember();
        organizerMembership.setTrip(saved);
        organizerMembership.setUser(organizer);
        organizerMembership.setMemberStatus(TripMemberStatus.ACCEPTED);
        organizerMembership.setJoinedDate(LocalDateTime.now());
        tripMemberRepository.save(organizerMembership);

        return tripMapper.toResponse(saved, "ORGANIZER");
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripResponse> getMyTrips(String currentUserEmail) {
        User user = resolveCurrentUser(currentUserEmail);

        Map<UUID, Trip> tripsById = new LinkedHashMap<>();
        tripRepository.findByOrganizerId(user.getId()).forEach(trip -> tripsById.put(trip.getId(), trip));
        tripMemberRepository.findByUserIdAndMemberStatus(user.getId(), TripMemberStatus.ACCEPTED)
                .forEach(member -> tripsById.putIfAbsent(member.getTrip().getId(), member.getTrip()));

        return tripsById.values().stream()
                .map(trip -> tripMapper.toResponse(trip, viewerRole(trip, user.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TripResponse getTripById(UUID tripId, String currentUserEmail) {
        Trip trip = findTrip(tripId);
        User user = resolveCurrentUser(currentUserEmail);
        tripAuthorizationService.requireMember(trip, user.getId(), isAdmin(user));
        return tripMapper.toResponse(trip, viewerRole(trip, user.getId()));
    }

    @Override
    @Transactional
    public TripResponse updateTrip(UUID tripId, UpdateTripRequest request, String currentUserEmail) {
        Trip trip = findTrip(tripId);
        User user = resolveCurrentUser(currentUserEmail);
        tripAuthorizationService.requireOrganizer(trip, user.getId(), isAdmin(user));
        tripAuthorizationService.requireMutableTrip(trip);
        requireValidDateRange(request.startDate(), request.endDate());

        trip.setTripName(request.tripName());
        trip.setSourceLocation(request.sourceLocation());
        trip.setDestinationId(request.destinationId());
        trip.setBudgetAmount(request.budgetAmount());
        trip.setCategoryId(request.categoryId());
        trip.setStartDate(request.startDate());
        trip.setEndDate(request.endDate());
        Trip saved = tripRepository.save(trip);

        return tripMapper.toResponse(saved, viewerRole(saved, user.getId()));
    }

    private static final Map<TravelerTripStatus, Set<TravelerTripStatus>> ALLOWED_TRANSITIONS = Map.of(
            TravelerTripStatus.PLANNING, Set.of(TravelerTripStatus.CONFIRMED, TravelerTripStatus.CANCELLED),
            TravelerTripStatus.CONFIRMED, Set.of(TravelerTripStatus.ONGOING, TravelerTripStatus.CANCELLED),
            TravelerTripStatus.ONGOING, Set.of(TravelerTripStatus.COMPLETED),
            TravelerTripStatus.COMPLETED, Set.of(),
            TravelerTripStatus.CANCELLED, Set.of()
    );

    @Override
    @Transactional
    public TripResponse transitionStatus(UUID tripId, TripStatusTransitionRequest request, String currentUserEmail) {
        Trip trip = findTrip(tripId);
        User user = resolveCurrentUser(currentUserEmail);
        tripAuthorizationService.requireOrganizer(trip, user.getId(), isAdmin(user));

        TravelerTripStatus current = trip.getStatus();
        TravelerTripStatus target = request.status();

        // Self-transitions are simply absent from every status's allowed-target
        // set, so X -> X is rejected the same way as any other invalid edge -
        // no separate idempotency special-case, matching a strict state-machine
        // reading rather than the attach/detach-style idempotency used elsewhere.
        if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
            throw new InvalidRequestException("Cannot transition trip from " + current + " to " + target);
        }

        // The only date-aware guard in this model: a trip cannot be marked
        // ONGOING before its own start date arrives. No admin override - this is
        // a fact about the trip's dates, not a per-caller authorization decision.
        if (target == TravelerTripStatus.ONGOING && LocalDate.now().isBefore(trip.getStartDate())) {
            throw new InvalidRequestException("Trip cannot start before its start date " + trip.getStartDate());
        }

        trip.setStatus(target);
        Trip saved = tripRepository.save(trip);
        return tripMapper.toResponse(saved, viewerRole(saved, user.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripMemberResponse> getTripMembers(UUID tripId, String currentUserEmail) {
        Trip trip = findTrip(tripId);
        User user = resolveCurrentUser(currentUserEmail);
        tripAuthorizationService.requireMember(trip, user.getId(), isAdmin(user));
        return tripMemberRepository.findByTripId(tripId).stream()
                .map(tripMapper::toMemberResponse)
                .toList();
    }

    @Override
    @Transactional
    public TripMemberResponse addTripMember(UUID tripId, AddTripMemberRequest request, String currentUserEmail) {
        Trip trip = findTrip(tripId);
        User currentUser = resolveCurrentUser(currentUserEmail);
        tripAuthorizationService.requireOrganizer(trip, currentUser.getId(), isAdmin(currentUser));
        tripAuthorizationService.requireMutableTrip(trip);

        User targetUser = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User with email " + request.email() + " not found"));

        if (targetUser.getRole() != Role.ROLE_TRAVELER) {
            throw new InvalidRequestException("Only travelers can be added as trip participants");
        }
        if (targetUser.getId().equals(trip.getOrganizer().getId())) {
            throw new InvalidRequestException("The organizer cannot invite themselves");
        }

        Optional<TripMember> existing = tripMemberRepository.findByTripIdAndUserId(tripId, targetUser.getId());
        TripMember member;
        if (existing.isPresent()) {
            member = existing.get();
            if (member.getMemberStatus() == TripMemberStatus.ACCEPTED) {
                throw new InvalidRequestException("User is already an active member of this trip");
            }
            if (member.getMemberStatus() == TripMemberStatus.INVITED) {
                throw new InvalidRequestException("User already has a pending invitation to this trip");
            }
            // REJECTED: re-invite by reusing the existing row (unique constraint on
            // trip_id+user_id forbids a second row). budgetAmount/spentAmount are left
            // untouched - a REJECTED row can only ever have come from an INVITED row
            // that was declined, and INVITED members are never eligible participants
            // in Expense's ACCEPTED-only checks, so those fields are always still the
            // BigDecimal.ZERO entity default here; there is no financial history to lose.
            member.setMemberStatus(TripMemberStatus.INVITED);
            member.setJoinedDate(null);
        } else {
            member = new TripMember();
            member.setTrip(trip);
            member.setUser(targetUser);
            member.setMemberStatus(TripMemberStatus.INVITED);
            member.setJoinedDate(null);
        }

        TripMember saved = tripMemberRepository.save(member);
        return tripMapper.toMemberResponse(saved);
    }

    @Override
    @Transactional
    public TripMemberResponse acceptTripMemberInvitation(UUID tripId, UUID tripMemberId, String currentUserEmail) {
        Trip trip = findTrip(tripId);
        User currentUser = resolveCurrentUser(currentUserEmail);
        TripMember member = tripMemberRepository.findByIdAndTripId(tripMemberId, tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip member with id " + tripMemberId + " not found"));

        if (!member.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only the invited traveler can accept this invitation");
        }
        if (member.getMemberStatus() != TripMemberStatus.INVITED) {
            throw new InvalidRequestException("Only a pending invitation can be accepted");
        }
        // A pending invitation must not become an active member of a trip that
        // has already closed while the invite sat unanswered.
        tripAuthorizationService.requireMutableTrip(trip);

        member.setMemberStatus(TripMemberStatus.ACCEPTED);
        member.setJoinedDate(LocalDateTime.now());
        TripMember saved = tripMemberRepository.save(member);
        return tripMapper.toMemberResponse(saved);
    }

    @Override
    @Transactional
    public TripMemberResponse rejectTripMemberInvitation(UUID tripId, UUID tripMemberId, String currentUserEmail) {
        Trip trip = findTrip(tripId);
        User currentUser = resolveCurrentUser(currentUserEmail);
        TripMember member = tripMemberRepository.findByIdAndTripId(tripMemberId, tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip member with id " + tripMemberId + " not found"));

        if (!member.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only the invited traveler can reject this invitation");
        }
        if (member.getMemberStatus() != TripMemberStatus.INVITED) {
            throw new InvalidRequestException("Only a pending invitation can be rejected");
        }
        tripAuthorizationService.requireMutableTrip(trip);

        member.setMemberStatus(TripMemberStatus.REJECTED);
        TripMember saved = tripMemberRepository.save(member);
        return tripMapper.toMemberResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingInvitationResponse> getMyPendingInvitations(String currentUserEmail) {
        User user = resolveCurrentUser(currentUserEmail);
        return tripMemberRepository.findByUserIdAndMemberStatus(user.getId(), TripMemberStatus.INVITED).stream()
                .map(tripMapper::toPendingInvitationResponse)
                .toList();
    }

    @Override
    @Transactional
    public void removeTripMember(UUID tripId, UUID tripMemberId, String currentUserEmail) {
        Trip trip = findTrip(tripId);
        User currentUser = resolveCurrentUser(currentUserEmail);
        tripAuthorizationService.requireOrganizer(trip, currentUser.getId(), isAdmin(currentUser));
        tripAuthorizationService.requireMutableTrip(trip);

        TripMember member = tripMemberRepository.findByIdAndTripId(tripMemberId, tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip member with id " + tripMemberId + " not found"));

        if (member.getUser().getId().equals(trip.getOrganizer().getId())) {
            throw new InvalidRequestException("The trip organizer cannot be removed from trip membership");
        }

        // One removal path for all three statuses: withdraws a pending invitation
        // (INVITED), removes an active participant (ACCEPTED), or clears stale
        // declined-invitation history (REJECTED) - organizer-only in every case.
        tripMemberRepository.delete(member);
    }

    private Trip findTrip(UUID tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip with id " + tripId + " not found"));
    }

    private User resolveCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private boolean isAdmin(User user) {
        return user.getRole() == Role.ROLE_ADMIN;
    }

    private String viewerRole(Trip trip, UUID userId) {
        if (tripAuthorizationService.isOrganizer(trip, userId)) {
            return "ORGANIZER";
        }
        if (tripAuthorizationService.isMember(trip, userId)) {
            return "MEMBER";
        }
        return "ADMIN";
    }

    private void requireValidDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new InvalidRequestException("startDate must not be after endDate");
        }
    }
}
