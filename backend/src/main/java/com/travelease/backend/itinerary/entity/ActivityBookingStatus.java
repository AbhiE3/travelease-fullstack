package com.travelease.backend.itinerary.entity;

/**
 * CONFIRMED -> CANCELLED (traveler, before slot start)
 * CONFIRMED -> ATTENDED  (activity provider, after slot start)
 * CONFIRMED -> NO_SHOW   (activity provider, after slot start)
 * CANCELLED / ATTENDED / NO_SHOW are all terminal - no exit transitions.
 * No PENDING: there is no payment gateway/simulation for Activity Booking in
 * this phase, and booking creation is a single atomic operation (capacity
 * check + insert under a pessimistic slot lock), so there is no intermediate
 * reservation-hold state to represent.
 */
public enum ActivityBookingStatus {
    CONFIRMED,
    CANCELLED,
    ATTENDED,
    NO_SHOW
}
