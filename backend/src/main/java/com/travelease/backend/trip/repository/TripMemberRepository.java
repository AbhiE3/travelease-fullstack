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

    boolean existsByTripIdAndUserIdAndMemberStatus(UUID tripId, UUID userId, TripMemberStatus memberStatus);

    boolean existsByTripIdAndUserEmailAndMemberStatus(UUID tripId, String email, TripMemberStatus memberStatus);

    Optional<TripMember> findByTripIdAndUserEmail(UUID tripId, String email);

    Optional<TripMember> findByTripIdAndUserEmailAndMemberStatus(UUID tripId, String email, TripMemberStatus memberStatus);

    Optional<TripMember> findByTripIdAndUserId(UUID tripId, UUID userId);

    List<TripMember> findByTripIdAndUserIdIn(UUID tripId, Collection<UUID> userIds);

    List<TripMember> findByTripIdAndUserIdInAndMemberStatus(UUID tripId, Collection<UUID> userIds, TripMemberStatus memberStatus);

    List<TripMember> findByTripId(UUID tripId);

    List<TripMember> findByTripIdAndMemberStatus(UUID tripId, TripMemberStatus memberStatus);

    List<TripMember> findByUserId(UUID userId);

    List<TripMember> findByUserIdAndMemberStatus(UUID userId, TripMemberStatus memberStatus);

    Optional<TripMember> findByIdAndTripId(UUID id, UUID tripId);
}
