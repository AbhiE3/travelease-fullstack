package com.travelease.backend.trip.repository;

import com.travelease.backend.trip.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TripRepository extends JpaRepository<Trip, UUID> {

    List<Trip> findByOrganizerId(UUID organizerId);
}
