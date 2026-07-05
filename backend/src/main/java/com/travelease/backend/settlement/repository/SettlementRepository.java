package com.travelease.backend.settlement.repository;

import com.travelease.backend.settlement.entity.Settlement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SettlementRepository extends JpaRepository<Settlement, UUID> {

    @Override
    @EntityGraph(attributePaths = {"trip", "payer", "receiver"})
    Optional<Settlement> findById(UUID id);

    @EntityGraph(attributePaths = {"payer", "receiver"})
    List<Settlement> findByTripId(UUID tripId);

    @EntityGraph(attributePaths = {"payer", "receiver"})
    @Query("""
            select settlement
            from Settlement settlement
            where settlement.trip.id = :tripId
              and (settlement.payer.email = :email or settlement.receiver.email = :email)
            """)
    List<Settlement> findParticipantSettlements(@Param("tripId") UUID tripId, @Param("email") String email);

    Optional<Settlement> findByTripIdAndPayerIdAndReceiverId(UUID tripId, UUID payerId, UUID receiverId);
}
