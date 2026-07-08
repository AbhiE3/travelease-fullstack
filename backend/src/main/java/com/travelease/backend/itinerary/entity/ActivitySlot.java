package com.travelease.backend.itinerary.entity;

import com.travelease.backend.shared.entity.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * The date/time-specific bookable inventory configuration for an Activity -
 * "Activity X, on date Y, from time A to time B, at price P, with capacity C".
 * Deliberately does NOT store a separate availableCapacity: with no
 * ActivityBooking yet to decrement it, a second capacity-like field would only
 * ever equal capacity, i.e. dead mutable state with no path to ever diverge.
 * That field belongs to the phase that introduces booking-capacity decrement
 * logic, not this one. No status/availability flag either - nothing in this
 * phase's management operations needs to represent "blocked" separately from
 * simply not listing/updating a slot, and Phase 1 explicitly avoids inventing
 * a lifecycle beyond what current management requires.
 */
@Getter
@Setter
@Entity
@AttributeOverride(name = "id", column = @Column(name = "activity_slot_id", nullable = false, updatable = false))
@Table(name = "activity_slots")
public class ActivitySlot extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", referencedColumnName = "ActivityID", nullable = false)
    private Activity activity;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer capacity;
}
