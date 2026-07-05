package com.travelease.backend.trip.repository;

import com.travelease.backend.trip.entity.TripMember;
import com.travelease.backend.trip.entity.TripMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TripMemberRepository extends JpaRepository<TripMember, UUID> {

    boolean existsByTripIdAndUserEmail(UUID tripId, String email);

    boolean existsByTripIdAndUserId(UUID tripId, UUID userId);

    Optional<TripMember> findByTripIdAndUserEmail(UUID tripId, String email);

    List<TripMember> findByTripIdAndUserIdIn(UUID tripId, Collection<UUID> userIds);

    List<TripMember> findByTripId(UUID tripId);

    List<TripMember> findByTripIdAndMemberStatus(UUID tripId, TripMemberStatus memberStatus);
}
