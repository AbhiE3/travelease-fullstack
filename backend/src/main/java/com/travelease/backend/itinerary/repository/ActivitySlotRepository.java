package com.travelease.backend.itinerary.repository;

import com.travelease.backend.itinerary.entity.ActivitySlot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActivitySlotRepository extends JpaRepository<ActivitySlot, UUID> {

    List<ActivitySlot> findByActivity_ActivityId(String activityId);

    /**
     * Acquires a PESSIMISTIC_WRITE (SELECT ... FOR UPDATE) lock on the slot row.
     * A plain read-then-insert-child-row pattern is NOT protected by
     * ActivitySlot having no mutable capacity field to optimistically version -
     * two concurrent transactions could both read "capacity available" and both
     * insert a booking. Acquiring this lock at the start of booking creation
     * forces a second concurrent transaction for the SAME slot to block until
     * the first commits, so its own capacity-consumed sum correctly reflects
     * the first transaction's newly-inserted booking - the two are serialized
     * for this slot specifically, not globally.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ActivitySlot s WHERE s.id = :id")
    Optional<ActivitySlot> findByIdForUpdate(@Param("id") UUID id);
}
