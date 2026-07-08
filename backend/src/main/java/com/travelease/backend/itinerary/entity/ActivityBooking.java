package com.travelease.backend.itinerary.entity;

import com.travelease.backend.auth.entity.User;
import com.travelease.backend.shared.entity.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A Traveler-owned reservation against a specific ActivitySlot. Provider
 * ownership is deliberately NOT duplicated here - it is always derived through
 * activitySlot -> activity -> providerId, exactly as the approved design
 * requires, avoiding a redundant ownership column.
 */
@Getter
@Setter
@Entity
@AttributeOverride(name = "id", column = @Column(name = "activity_booking_id", nullable = false, updatable = false))
@Table(name = "activity_bookings")
public class ActivityBooking extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_slot_id", nullable = false)
    private ActivitySlot activitySlot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booked_by_user_id", nullable = false)
    private User bookedBy;

    @Column(name = "participant_count", nullable = false)
    private Integer participantCount;

    /**
     * Snapshotted from ActivitySlot.price at creation time - a later provider
     * price edit must never change the total of an already-placed booking.
     */
    @Column(name = "price_per_participant", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerParticipant;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivityBookingStatus status;

    @CreationTimestamp
    @Column(name = "booked_at", nullable = false, updatable = false)
    private LocalDateTime bookedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "attendance_marked_at")
    private LocalDateTime attendanceMarkedAt;

    /**
     * Optional association to the Traveler planning Trip (trip.entity.Trip)
     * this booking has been attached to for shared trip planning. Bare UUID,
     * no FK/relationship - mirrors Booking.travelerTripId (busbooking) and
     * HotelBooking.tripId, both of which use the same cross-module-decoupled
     * shape rather than importing the Trip entity into this domain.
     */
    @Column(name = "trip_id")
    private UUID tripId;
}
