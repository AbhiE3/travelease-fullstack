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

    List<HotelResponse> getProviderHotels();

    HotelResponse updateHotel(UUID hotelId, HotelRequest request);

    HotelResponse updatePolicies(UUID hotelId, HotelPolicyRequest request);

    RoomResponse createRoom(UUID hotelId, RoomRequest request);

    List<RoomResponse> getRooms(UUID hotelId);

    RoomResponse updateRoom(UUID hotelId, UUID roomId, RoomRequest request);

    RoomResponse updateAvailability(UUID roomId, RoomAvailabilityRequest request);

    RoomResponse blockMaintenance(UUID roomId);

    List<RoomResponse> getInventory();

    BookingValidationResponse validateBooking(HotelBookingRequest request);

    BookingQuoteResponse quoteBooking(HotelBookingRequest request);

    HotelBookingResponse createBooking(HotelBookingRequest request, String currentUserEmail);

    HotelBookingResponse getBooking(UUID bookingId);

    HotelBookingResponse updateBooking(UUID bookingId, HotelBookingRequest request);

    HotelBookingResponse cancelBooking(UUID bookingId);

    List<HotelBookingResponse> getMyBookings(String currentUserEmail);

    List<HotelBookingResponse> getProviderBookings();

    HotelBillResponse getBill(UUID bookingId);

    HotelBookingResponse attachBookingToTrip(UUID tripId, AttachHotelBookingRequest request);

    void removeBookingFromTrip(UUID tripId, UUID bookingId);

    AccommodationSummaryResponse getAccommodationSummary(UUID tripId);

    List<HotelReviewResponse> getReviews(UUID hotelId);

    HotelReviewResponse addReview(UUID hotelId, ReviewRequest request, String currentUserEmail);

    HotelReviewResponse updateReview(UUID hotelId, UUID reviewId, ReviewRequest request, String currentUserEmail);

    void deleteReview(UUID hotelId, UUID reviewId, String currentUserEmail);

    HotelBookingResponse checkIn(UUID bookingId);

    HotelBookingResponse checkOut(UUID bookingId);
}
