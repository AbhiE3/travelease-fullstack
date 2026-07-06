package com.travelease.backend.itinerary.repository;

import com.travelease.backend.itinerary.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ActivityRepository
        extends JpaRepository<Activity, String> {

    List<Activity> findByDestinationId(Integer destinationId);
}