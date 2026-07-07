package com.travelease.backend.busbooking.repository;

import com.travelease.backend.busbooking.entity.BookingTimeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingTimelineRepository extends JpaRepository<BookingTimeline, Long> {

    List<BookingTimeline> findByBookingIdOrderByOccurredAtAsc(Long bookingId);
}
