package com.travelease.backend.trip.entity;

/**
 * Lifecycle of the Traveler planning Trip (this domain's own multi-day,
 * multi-service aggregation of Bus/Hotel/future Activity bookings) - distinct
 * from busbooking.entity.enums.TripStatus, which models a single physical bus
 * journey's operational lifecycle (SCHEDULED/BOARDING/DEPARTED/RUNNING/
 * DELAYED/ARRIVED/COMPLETED/CANCELLED). That vocabulary doesn't fit a planning
 * Trip that may span days and aggregate several independent service bookings,
 * so a dedicated enum is used here rather than reusing or colliding with it.
 */
public enum TravelerTripStatus {
    PLANNING,
    CONFIRMED,
    ONGOING,
    COMPLETED,
    CANCELLED
}
