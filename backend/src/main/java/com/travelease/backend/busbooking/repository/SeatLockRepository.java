package com.travelease.backend.busbooking.repository;

import com.travelease.backend.busbooking.entity.SeatLock;
import com.travelease.backend.busbooking.entity.enums.SeatLockStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatLockRepository extends JpaRepository<SeatLock, Long> {

    Optional<SeatLock> findBySeatIdAndScheduleIdAndStatus(Long seatId, Long scheduleId, SeatLockStatus status);

    List<SeatLock> findByScheduleIdAndStatus(Long scheduleId, SeatLockStatus status);

    List<SeatLock> findByUserIdAndScheduleIdAndStatus(Long userId, Long scheduleId, SeatLockStatus status);

    List<SeatLock> findByStatusAndExpiresAtBefore(SeatLockStatus status, LocalDateTime expiresAt);

    @Query("SELECT sl.seat.id FROM SeatLock sl " +
           "WHERE sl.scheduleId = :scheduleId " +
           "AND sl.status = :status " +
           "AND sl.expiresAt > :now")
    List<Long> findLockedSeatIdsByScheduleId(@Param("scheduleId") Long scheduleId,
                                             @Param("status") SeatLockStatus status,
                                             @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE SeatLock sl SET sl.status = :expiredStatus " +
           "WHERE sl.status = :lockedStatus AND sl.expiresAt < :now")
    int expireLocks(@Param("lockedStatus") SeatLockStatus lockedStatus,
                    @Param("expiredStatus") SeatLockStatus expiredStatus,
                    @Param("now") LocalDateTime now);
}
