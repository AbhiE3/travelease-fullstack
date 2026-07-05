package com.travelease.backend.busbooking.repository;

import com.travelease.backend.busbooking.entity.Booking;
import com.travelease.backend.busbooking.entity.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    Optional<Booking> findByBookingReference(String ref);

    List<Booking> findByScheduleId(Long scheduleId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = :status")
    Long countByStatus(@Param("status") com.travelease.backend.busbooking.entity.enums.BookingStatus status);

    @Query("SELECT COALESCE(SUM(b.totalFare), 0.0) FROM Booking b WHERE b.status = 'CONFIRMED'")
    Double sumRevenueFromConfirmedBookings();

    @Query("SELECT COUNT(b) FROM Booking b WHERE CAST(b.bookedAt AS DATE) = :today")
    Long countTodayBookings(@Param("today") LocalDate today);

    @Query("SELECT DISTINCT b.schedule.route.source, b.schedule.route.destination, " +
           "b.schedule.route.id, COUNT(b) as bookingCount " +
           "FROM Booking b WHERE b.userId = :userId AND b.status = 'CONFIRMED' " +
           "GROUP BY b.schedule.route.source, b.schedule.route.destination, b.schedule.route.id " +
           "ORDER BY bookingCount DESC")
    List<Object[]> findSearchSuggestionsByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT b.schedule.route.source, b.schedule.route.destination, " +
           "b.schedule.route.id, COUNT(b) as bookingCount " +
           "FROM Booking b WHERE b.status = 'CONFIRMED' " +
           "GROUP BY b.schedule.route.source, b.schedule.route.destination, b.schedule.route.id " +
           "ORDER BY bookingCount DESC")
    List<Object[]> findFrequentlyBookedRoutes(Pageable pageable);

    // Phase 6 â€“ Booking lifecycle queries
    List<Booking> findByUserIdAndStatusOrderByBookedAtDesc(Long userId, BookingStatus status);

    // Ticket verification (fixes C-1: avoids loading all bookings)
    Optional<Booking> findByTicketNumber(String ticketNumber);

    @Query("SELECT b FROM Booking b WHERE b.status IN :statuses AND b.expiresAt < :now")
    List<Booking> findExpiredBookings(@Param("statuses") List<BookingStatus> statuses, @Param("now") LocalDateTime now);

    @Query("SELECT b FROM Booking b WHERE b.userId = :userId " +
           "AND b.status = 'CONFIRMED' " +
           "AND b.schedule.travelDate = :yesterday")
    List<Booking> findBookingsToComplete(@Param("userId") Long userId, @Param("yesterday") LocalDate yesterday);

    // Phase 9 â€“ Analytics queries
    @Query("SELECT COALESCE(SUM(b.totalFare), 0.0) FROM Booking b WHERE b.schedule.bus.providerId = :providerId AND b.status = 'CONFIRMED'")
    Double sumRevenueByProvider(@Param("providerId") Long providerId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.schedule.bus.providerId = :providerId AND CAST(b.bookedAt AS DATE) = :today")
    Long countTodayBookingsByProvider(@Param("providerId") Long providerId, @Param("today") LocalDate today);

    @Query("SELECT COALESCE(SUM(b.totalFare), 0.0) FROM Booking b WHERE b.schedule.bus.providerId = :providerId AND CAST(b.bookedAt AS DATE) = :today AND b.status = 'CONFIRMED'")
    Double sumTodayRevenueByProvider(@Param("providerId") Long providerId, @Param("today") LocalDate today);

    @Query("SELECT COALESCE(SUM(b.totalFare), 0.0) FROM Booking b WHERE b.schedule.bus.providerId = :providerId AND b.bookedAt >= :since AND b.status = 'CONFIRMED'")
    Double sumRevenueByProviderSince(@Param("providerId") Long providerId, @Param("since") LocalDateTime since);

    // Fix H-4: Single batch query for daily revenue trend (replaces 2N individual queries)
    @Query("SELECT CAST(b.bookedAt AS DATE), COALESCE(SUM(b.totalFare), 0.0) " +
            "FROM Booking b WHERE b.schedule.bus.providerId = :providerId AND b.status = 'CONFIRMED' " +
            "AND b.bookedAt >= :since AND b.bookedAt < :until " +
            "GROUP BY CAST(b.bookedAt AS DATE)")
    List<Object[]> sumDailyRevenueByProviderAndDateRange(@Param("providerId") Long providerId, @Param("since") LocalDateTime since, @Param("until") LocalDateTime until);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.schedule.bus.providerId = :providerId AND b.status = 'CONFIRMED'")
    Long countConfirmedBookingsByProvider(@Param("providerId") Long providerId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.schedule.bus.providerId = :providerId AND b.status = 'CANCELLED'")
    Long countCancelledBookingsByProvider(@Param("providerId") Long providerId);

    // Date-range variants for booking analytics (from/to filter support)
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.schedule.bus.providerId = :providerId AND b.status = 'CONFIRMED' " +
           "AND b.bookedAt >= :since AND b.bookedAt < :until")
    Long countConfirmedBookingsByProviderAndDateRange(@Param("providerId") Long providerId,
                                                       @Param("since") LocalDateTime since,
                                                       @Param("until") LocalDateTime until);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.schedule.bus.providerId = :providerId AND b.status = 'CANCELLED' " +
           "AND b.bookedAt >= :since AND b.bookedAt < :until")
    Long countCancelledBookingsByProviderAndDateRange(@Param("providerId") Long providerId,
                                                       @Param("since") LocalDateTime since,
                                                       @Param("until") LocalDateTime until);

    @Query("SELECT COALESCE(COUNT(bs), 0) FROM Booking b JOIN b.bookingSeats bs WHERE b.schedule.bus.id = :busId AND b.status = 'CONFIRMED'")
    Long countSeatsSoldByBus(@Param("busId") Long busId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.schedule.bus.id = :busId AND b.status = 'CONFIRMED'")
    Long countBookingsByBus(@Param("busId") Long busId);

    @Query("SELECT COALESCE(SUM(b.totalFare), 0.0) FROM Booking b WHERE b.schedule.bus.id = :busId AND b.status = 'CONFIRMED'")
    Double sumRevenueByBus(@Param("busId") Long busId);

    // Fix: route booking stats must be scoped to the requesting provider (was previously
    // aggregating confirmed bookings across ALL providers, leaking cross-provider data
    // into per-provider route analytics).
    @Query("SELECT b.schedule.route.id, b.schedule.route.source, b.schedule.route.destination, COUNT(b), COALESCE(SUM(b.totalFare), 0.0) " +
           "FROM Booking b WHERE b.schedule.bus.providerId = :providerId AND b.status = 'CONFIRMED' " +
           "GROUP BY b.schedule.route.id, b.schedule.route.source, b.schedule.route.destination ORDER BY COUNT(b) DESC")
    List<Object[]> getRouteBookingStats(@Param("providerId") Long providerId, Pageable pageable);

    @Query(value = "SELECT EXTRACT(HOUR FROM b.booked_at) as booking_hour, COUNT(*) as booking_count " +
            "FROM bookings b " +
            "JOIN bus_schedules bs ON b.schedule_id = bs.id " +
            "JOIN buses bus ON bs.bus_id = bus.id " +
            "WHERE bus.provider_id = :providerId " +
            "AND b.booked_at >= :since AND b.booked_at < :until " +
            "GROUP BY EXTRACT(HOUR FROM b.booked_at) " +
            "ORDER BY EXTRACT(HOUR FROM b.booked_at)", nativeQuery = true)
    List<Object[]> getPeakBookingHoursByProviderAndDateRange(@Param("providerId") Long providerId,
                                                              @Param("since") LocalDateTime since,
                                                              @Param("until") LocalDateTime until);

    @Query("SELECT COALESCE(AVG(b.totalFare), 0.0) FROM Booking b WHERE b.schedule.bus.providerId = :providerId AND b.status = 'CONFIRMED'")
    Double averageBookingValueByProvider(@Param("providerId") Long providerId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.schedule.bus.providerId = :providerId AND b.couponCode IS NOT NULL AND b.couponCode <> ''")
    Long countCouponUsageByProvider(@Param("providerId") Long providerId);

    @Query("SELECT COALESCE(SUM(b.couponDiscount), 0.0) FROM Booking b WHERE b.schedule.bus.providerId = :providerId")
    Double sumCouponDiscountByProvider(@Param("providerId") Long providerId);
}
