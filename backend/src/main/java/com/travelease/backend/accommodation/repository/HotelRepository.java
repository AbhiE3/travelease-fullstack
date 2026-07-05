package com.travelease.backend.accommodation.repository;

import com.travelease.backend.accommodation.entity.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HotelRepository extends JpaRepository<Hotel, UUID> {

    List<Hotel> findByDestinationId(Integer destinationId);

    List<Hotel> findByStatusIgnoreCase(String status);

    List<Hotel> findByHotelNameContainingIgnoreCase(String hotelName);
}
