package com.travelease.backend.itinerary.repository;

import com.travelease.backend.itinerary.entity.Delay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DelayRepository
        extends JpaRepository<Delay, String> {

    List<Delay> findByTripId(String tripId);
}