package com.travelease.backend.itinerary.service;

import com.travelease.backend.itinerary.dto.ItineraryRequest;
import com.travelease.backend.itinerary.dto.ItineraryResponse;
import com.travelease.backend.itinerary.entity.Itinerary;
import com.travelease.backend.itinerary.mapper.ItineraryMapper;
import com.travelease.backend.itinerary.repository.ItineraryRepository;
import com.travelease.backend.shared.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ItineraryService {

    @Autowired
    private ItineraryRepository itineraryRepository;

    @Autowired
    private ItineraryMapper itineraryMapper;

    // US-ITI-01 — Create itinerary item
    public ItineraryResponse addItem(ItineraryRequest request) {
        Itinerary itinerary = itineraryMapper.toEntity(request);
        itinerary.setItineraryId(UUID.randomUUID().toString());
        itinerary.setStatus("Pending");
        Itinerary saved = itineraryRepository.save(itinerary);
        return itineraryMapper.toResponse(saved);
    }

    // US-ITI-01 — Get all itinerary items for a trip
    public List<ItineraryResponse> getByTripId(String tripId) {
        return itineraryRepository.findByTripId(tripId)
                .stream()
                .map(itineraryMapper::toResponse)
                .collect(Collectors.toList());
    }

    // US-ITI-01 — Get itinerary filtered by date
    public List<ItineraryResponse> getByTripIdAndDate(
            String tripId, java.time.LocalDate activityDate) {
        return itineraryRepository
                .findByTripIdAndActivityDate(tripId, activityDate)
                .stream()
                .map(itineraryMapper::toResponse)
                .collect(Collectors.toList());
    }

    // US-ITI-02 — Update itinerary item (modify details OR complete/uncomplete)
    public ItineraryResponse updateItem(String itineraryId,
                                        ItineraryRequest request) {
        Optional<Itinerary> existing =
                itineraryRepository.findById(itineraryId);
        if (existing.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Itinerary item not found: " + itineraryId);
        }
        Itinerary itinerary = existing.get();

        // Update only fields that are sent
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

    // US-ITI-02 — Delete itinerary item
    public void deleteItem(String itineraryId) {
        if (!itineraryRepository.existsById(itineraryId)) {
            throw new ResourceNotFoundException(
                    "Itinerary item not found: " + itineraryId);
        }
        itineraryRepository.deleteById(itineraryId);
    }

    // US-ITI-03 — Get progress summary for a trip
    public java.util.Map<String, Object> getProgress(String tripId) {
        List<Itinerary> all = itineraryRepository.findByTripId(tripId);
        long total = all.size();
        long completed = all.stream()
                .filter(i -> "Completed".equals(i.getStatus()))
                .count();
        long pending = total - completed;
        double percentage = total > 0
                ? (completed * 100.0) / total : 0.0;

        return java.util.Map.of(
                "tripId", tripId,
                "totalActivities", total,
                "completedActivities", completed,
                "pendingActivities", pending,
                "completionPercentage", percentage
        );
    }
}