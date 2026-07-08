package com.travelease.backend.itinerary.service;

import com.travelease.backend.itinerary.dto.ActivityBookingResponse;
import com.travelease.backend.itinerary.dto.ActivitySlotResponse;
import com.travelease.backend.itinerary.dto.AttachActivityBookingRequest;
import com.travelease.backend.itinerary.dto.CreateActivityBookingRequest;
import com.travelease.backend.itinerary.entity.ActivityBookingStatus;

import java.util.List;
import java.util.UUID;

public interface ActivityBookingService {

    // Traveler-facing
    List<ActivitySlotResponse> getBookableSlots(String activityId);
    ActivityBookingResponse createBooking(CreateActivityBookingRequest request);
    ActivityBookingResponse cancelBooking(UUID bookingId);
    ActivityBookingResponse getMyBooking(UUID bookingId);
    List<ActivityBookingResponse> getMyBookings();

    // Activity Provider-facing
    List<ActivityBookingResponse> getBookingsForActivity(String activityId);
    ActivityBookingResponse getProviderBooking(UUID bookingId);
    ActivityBookingResponse markAttendance(UUID bookingId, ActivityBookingStatus status);

    // Traveler Trip integration - attach/detach/list only; booking lifecycle
    // (create/cancel/attendance) is intentionally untouched by this feature.
    ActivityBookingResponse attachBookingToTrip(UUID tripId, AttachActivityBookingRequest request);
    void removeBookingFromTrip(UUID tripId, UUID bookingId);
    List<ActivityBookingResponse> getTripActivityBookings(UUID tripId);
}
