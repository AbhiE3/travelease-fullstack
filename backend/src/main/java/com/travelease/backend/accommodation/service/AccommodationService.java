package com.travelease.backend.accommodation.service;

import com.travelease.backend.accommodation.dto.*;

import java.util.List;
import java.util.UUID;

public interface AccommodationService {
    List<HotelResponse> searchHotels(Integer destinationId, String status, String query);

    HotelDetailsResponse getHotelDetails(UUID hotelId);

    BookingValidationResponse validateBooking(HotelBookingRequest request);

    BookingQuoteResponse quoteBooking(HotelBookingRequest request);

    HotelBookingResponse createBooking(HotelBookingRequest request, String userEmail);

    HotelBookingResponse getBooking(UUID bookingId, String userEmail);

    HotelBookingResponse modifyBooking(UUID bookingId, HotelBookingRequest request, String userEmail);

    HotelBookingResponse cancelBooking(UUID bookingId, String userEmail);

    List<HotelBookingResponse> getMyBookings(String userEmail);

    List<HotelReviewResponse> getReviews(UUID hotelId);

    HotelReviewResponse addReview(UUID hotelId, ReviewRequest request, String userEmail);

    HotelReviewResponse updateReview(UUID hotelId, UUID reviewId, ReviewRequest request, String userEmail);

    void deleteReview(UUID hotelId, UUID reviewId, String userEmail);

    HotelBillResponse getBill(UUID bookingId, String userEmail);

    HotelBookingResponse attachBookingToTrip(UUID tripId, AttachHotelBookingRequest request, String userEmail);

    void removeBookingFromTrip(UUID tripId, UUID bookingId, String userEmail);

    AccommodationSummaryResponse getTripAccommodationSummary(UUID tripId, String userEmail);

    HotelResponse createHotel(HotelRequest request, String providerEmail);

    List<HotelResponse> getProviderHotels(String providerEmail);

    HotelDetailsResponse getProviderHotel(UUID hotelId, String providerEmail);

    HotelResponse updateHotel(UUID hotelId, HotelRequest request, String providerEmail);

    RoomResponse createRoom(UUID hotelId, RoomRequest request, String providerEmail);

    List<RoomResponse> getRooms(UUID hotelId, String providerEmail);

    RoomResponse updateRoom(UUID hotelId, UUID roomId, RoomRequest request, String providerEmail);

    RoomResponse updateRoomAvailability(UUID roomId, RoomAvailabilityRequest request, String providerEmail);

    RoomResponse blockRoomForMaintenance(UUID roomId, String providerEmail);

    List<RoomResponse> getProviderInventory(String providerEmail);

    HotelResponse updatePolicies(UUID hotelId, HotelPolicyRequest request, String providerEmail);

    HotelBookingResponse checkIn(UUID bookingId, String providerEmail);

    HotelBookingResponse checkOut(UUID bookingId, String providerEmail);
}
