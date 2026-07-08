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
import com.travelease.backend.trip.repository.TripRepository;
import com.travelease.backend.trip.security.TripAuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ItineraryService {

    @Autowired
    private ItineraryRepository itineraryRepository;

    @Autowired
    private ItineraryMapper itineraryMapper;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TripAuthorizationService tripAuthorizationService;

    @Autowired
    private UserRepository userRepository;

    // US-ITI-01 — Create itinerary item. Collaborative: any active trip
    // participant (organizer or ACCEPTED member) may add an item - itinerary
    // items record no per-item creator/owner field, so there is nothing to scope
    // create access more narrowly than trip membership.
    public ItineraryResponse addItem(ItineraryRequest request, String currentUserEmail) {
        Trip trip = resolveTrip(request.getTripId());
        requireActiveMember(trip, currentUserEmail);
        tripAuthorizationService.requireMutableTrip(trip);

        Itinerary itinerary = itineraryMapper.toEntity(request);
        itinerary.setItineraryId(UUID.randomUUID().toString());
        itinerary.setTripId(trip.getId().toString());
        itinerary.setStatus("Pending");
        Itinerary saved = itineraryRepository.save(itinerary);
        return itineraryMapper.toResponse(saved);
    }

    // US-ITI-01 — Get all itinerary items for a trip. Active trip participants only.
    public List<ItineraryResponse> getByTripId(String tripId, String currentUserEmail) {
        Trip trip = resolveTrip(tripId);
        requireActiveMember(trip, currentUserEmail);
        return itineraryRepository.findByTripId(trip.getId().toString())
                .stream()
                .map(itineraryMapper::toResponse)
                .collect(Collectors.toList());
    }

    // US-ITI-01 — Get itinerary filtered by date. Active trip participants only.
    public List<ItineraryResponse> getByTripIdAndDate(
            String tripId, java.time.LocalDate activityDate, String currentUserEmail) {
        Trip trip = resolveTrip(tripId);
        requireActiveMember(trip, currentUserEmail);
        return itineraryRepository
                .findByTripIdAndActivityDate(trip.getId().toString(), activityDate)
                .stream()
                .map(itineraryMapper::toResponse)
                .collect(Collectors.toList());
    }

    // US-ITI-02 — Update itinerary item (modify details OR complete/uncomplete).
    // Collaborative: same reasoning as create - any active participant may
    // update/check off shared itinerary items. The item's trip is resolved from
    // its own persisted tripId (never from client input), so authorization
    // cannot be fooled by a mismatched tripId in the request body, and an item
    // can never be reassigned to a different trip via update.
    public ItineraryResponse updateItem(String itineraryId, ItineraryRequest request, String currentUserEmail) {
        Itinerary itinerary = findItemById(itineraryId);
        Trip trip = resolveTrip(itinerary.getTripId());
        requireActiveMember(trip, currentUserEmail);
        tripAuthorizationService.requireMutableTrip(trip);

        if (request.getActivityDate() != null)
            itinerary.setActivityDate(request.getActivityDate());
        if (request.getStartTime() != null)
            itinerary.setStartTime(request.getStartTime());
        if (request.getEndTime() != null)
            itinerary.setEndTime(request.getEndTime());

        // US-ITI-03 — Mark complete or incomplete
        if (request.getStatus() != null) {
            itinerary.setStatus(request.getStatus());
            if ("Completed".equals(request.getStatus())) {
                itinerary.setCompletionTime(LocalDateTime.now());
            } else if ("Pending".equals(request.getStatus())) {
                itinerary.setCompletionTime(null);
            }
        }

        Itinerary updated = itineraryRepository.save(itinerary);
        return itineraryMapper.toResponse(updated);
    }

    // US-ITI-02 — Delete itinerary item. Organizer-only: deletion is
    // irreversible (unlike toggling status), so the smallest safe model gives a
    // single point of accountability rather than letting any accepted member
    // remove another participant's shared itinerary contribution.
    public void deleteItem(String itineraryId, String currentUserEmail) {
        Itinerary itinerary = findItemById(itineraryId);
        Trip trip = resolveTrip(itinerary.getTripId());
        User currentUser = resolveCurrentUser(currentUserEmail);
        tripAuthorizationService.requireOrganizer(trip, currentUser.getId(), isAdmin(currentUser));
        tripAuthorizationService.requireMutableTrip(trip);

        itineraryRepository.delete(itinerary);
    }

    // US-ITI-03 — Get progress summary for a trip. Active trip participants only.
    public Map<String, Object> getProgress(String tripId, String currentUserEmail) {
        Trip trip = resolveTrip(tripId);
        requireActiveMember(trip, currentUserEmail);

        List<Itinerary> all = itineraryRepository.findByTripId(trip.getId().toString());
        long total = all.size();
        long completed = all.stream()
                .filter(i -> "Completed".equals(i.getStatus()))
                .count();
        long pending = total - completed;
        double percentage = total > 0
                ? (completed * 100.0) / total : 0.0;

        return Map.of(
                "tripId", tripId,
                "totalActivities", total,
                "completedActivities", completed,
                "pendingActivities", pending,
                "completionPercentage", percentage
        );
    }

    private Itinerary findItemById(String itineraryId) {
        return itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary item not found: " + itineraryId));
    }

    // The Trip domain's canonical identity is a UUID (Trip.id); Itinerary.tripId
    // remains a plain String column (no schema change made here - nothing in the
    // current dataset requires it), but every authorization decision resolves it
    // against the real Trip via this single conversion point, so a malformed
    // tripId reliably surfaces as 400 and a well-formed-but-nonexistent one as
    // 404, instead of ever reaching TripAuthorizationService with a bogus value.
    private Trip resolveTrip(String tripId) {
        UUID tripUuid;
        try {
            tripUuid = UUID.fromString(tripId);
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("tripId must be a valid UUID: " + tripId);
        }
        return tripRepository.findById(tripUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Trip with id " + tripId + " not found"));
    }

    private User resolveCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private void requireActiveMember(Trip trip, String currentUserEmail) {
        User currentUser = resolveCurrentUser(currentUserEmail);
        tripAuthorizationService.requireMember(trip, currentUser.getId(), isAdmin(currentUser));
    }

    private boolean isAdmin(User user) {
        return user.getRole() == Role.ROLE_ADMIN;
    }
}
