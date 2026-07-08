package com.travelease.backend.trips_and_invitations.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.travelease.backend.trips_and_invitations.entity.Invitation;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, UUID>{

    List<Invitation> findByInviteeEmail(String inviteeEmail);

    List<Invitation> findByTripId(UUID tripId);

    boolean existsByTripTripIdAndInviteeEmail(UUID tripId, String inviteeEmail);
}
