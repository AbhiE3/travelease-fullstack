package com.travelease.backend.accommodation.repository;

import com.travelease.backend.accommodation.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID> {
    List<Room> findByHotelId(UUID hotelId);

    Optional<Room> findFirstByHotelIdAndRoomTypeIgnoreCaseAndAvailabilityStatusIgnoreCase(
            UUID hotelId,
            String roomType,
            String availabilityStatus
    );
}
