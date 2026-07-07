package com.travelease.backend.itinerary.repository;

import com.travelease.backend.itinerary.entity.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ItineraryRepository
        extends JpaRepository<Itinerary, String> {

    List<Itinerary> findByTripId(String tripId);

    List<Itinerary> findByTripIdAndStatus(
            String tripId, String status);

    List<Itinerary> findByTripIdAndActivityDate(
            String tripId, LocalDate activityDate);
}