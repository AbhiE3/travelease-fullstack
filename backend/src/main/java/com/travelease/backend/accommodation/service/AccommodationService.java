package com.travelease.backend.accommodation.service;

import com.travelease.backend.accommodation.dto.AccommodationSummaryResponse;
import com.travelease.backend.accommodation.dto.AttachHotelBookingRequest;
import com.travelease.backend.accommodation.dto.BookingQuoteResponse;
import com.travelease.backend.accommodation.dto.BookingValidationResponse;
import com.travelease.backend.accommodation.dto.HotelBillResponse;
import com.travelease.backend.accommodation.dto.HotelBookingRequest;
import com.travelease.backend.accommodation.dto.HotelBookingResponse;
import com.travelease.backend.accommodation.dto.HotelDetailsResponse;
import com.travelease.backend.accommodation.dto.HotelPolicyRequest;
import com.travelease.backend.accommodation.dto.HotelRequest;
import com.travelease.backend.accommodation.dto.HotelResponse;
import com.travelease.backend.accommodation.dto.HotelReviewResponse;
import com.travelease.backend.accommodation.dto.ReviewRequest;
import com.travelease.backend.accommodation.dto.RoomAvailabilityRequest;
import com.travelease.backend.accommodation.dto.RoomRequest;
import com.travelease.backend.accommodation.dto.RoomResponse;

import java.util.List;
import java.util.UUID;

public interface AccommodationService {

    List<HotelResponse> searchHotels(Integer destinationId, String status, String query);

    HotelDetailsResponse getHotelDetails(UUID hotelId);

    HotelResponse createHotel(HotelRequest request);

    List<HotelResponse> getProviderHotels(Long providerId);

    /**
     * Hotel Provider management variant of {@link #getHotelDetails(UUID)}: same
     * response shape, but additionally asserts that the caller's effective Hotel
     * Provider tenant owns the hotel. getHotelDetails itself stays unscoped because
     * it also backs the public/traveler-facing GET /api/hotels/{hotelId} endpoint.
     */
    HotelDetailsResponse getProviderHotelDetails(UUID hotelId);

    HotelResponse updateHotel(UUID hotelId, HotelRequest request);

    HotelResponse updatePolicies(UUID hotelId, HotelPolicyRequest request);

    RoomResponse createRoom(UUID hotelId, RoomRequest request);

    List<RoomResponse> getRooms(UUID hotelId);

    RoomResponse updateRoom(UUID hotelId, UUID roomId, RoomRequest request);

    RoomResponse updateAvailability(UUID roomId, RoomAvailabilityRequest request);

    RoomResponse blockMaintenance(UUID roomId);

    List<RoomResponse> getInventory(Long providerId);

    BookingValidationResponse validateBooking(HotelBookingRequest request);

    BookingQuoteResponse quoteBooking(HotelBookingRequest request);

    HotelBookingResponse createBooking(HotelBookingRequest request, String currentUserEmail);

    HotelBookingResponse getBooking(UUID bookingId, String currentUserEmail);

    HotelBookingResponse updateBooking(UUID bookingId, HotelBookingRequest request, String currentUserEmail);

    HotelBookingResponse cancelBooking(UUID bookingId, String currentUserEmail);

    List<HotelBookingResponse> getMyBookings(String currentUserEmail);

    HotelBillResponse getBill(UUID bookingId, String currentUserEmail);

    HotelBookingResponse attachBookingToTrip(UUID tripId, AttachHotelBookingRequest request, String currentUserEmail);

    void removeBookingFromTrip(UUID tripId, UUID bookingId, String currentUserEmail);

    AccommodationSummaryResponse getAccommodationSummary(UUID tripId, String currentUserEmail);

    List<HotelReviewResponse> getReviews(UUID hotelId);

    HotelReviewResponse addReview(UUID hotelId, ReviewRequest request, String currentUserEmail);

    HotelReviewResponse updateReview(UUID hotelId, UUID reviewId, ReviewRequest request, String currentUserEmail);

    void deleteReview(UUID hotelId, UUID reviewId, String currentUserEmail);

    HotelBookingResponse checkIn(UUID bookingId);

    HotelBookingResponse checkOut(UUID bookingId);
}
