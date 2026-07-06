package com.travelease.backend.accommodation.repository;

import com.travelease.backend.accommodation.entity.HotelBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HotelBookingRepository extends JpaRepository<HotelBooking, UUID> {
    List<HotelBooking> findByBookedByEmailOrderByCreatedAtDesc(String email);

    List<HotelBooking> findByTripId(UUID tripId);
}
