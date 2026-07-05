package com.travelease.backend.accommodation.repository;

import com.travelease.backend.accommodation.entity.HotelBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HotelBookingRepository extends JpaRepository<HotelBooking, UUID> {

    List<HotelBooking> findByTripId(UUID tripId);

    List<HotelBooking> findByHotelId(UUID hotelId);

    List<HotelBooking> findByBookedByEmail(String email);
}
