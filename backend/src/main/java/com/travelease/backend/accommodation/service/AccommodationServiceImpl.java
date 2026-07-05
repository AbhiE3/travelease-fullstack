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
import com.travelease.backend.accommodation.entity.Hotel;
import com.travelease.backend.accommodation.entity.HotelBooking;
import com.travelease.backend.accommodation.entity.HotelReview;
import com.travelease.backend.accommodation.entity.Room;
import com.travelease.backend.accommodation.repository.HotelBookingRepository;
import com.travelease.backend.accommodation.repository.HotelRepository;
import com.travelease.backend.accommodation.repository.HotelReviewRepository;
import com.travelease.backend.accommodation.repository.RoomRepository;
import com.travelease.backend.auth.entity.User;
import com.travelease.backend.auth.repository.UserRepository;
import com.travelease.backend.shared.exception.InvalidRequestException;
import com.travelease.backend.shared.exception.ResourceNotFoundException;
import com.travelease.backend.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccommodationServiceImpl implements AccommodationService {

    private static final String AVAILABLE = "AVAILABLE";
    private static final String CONFIRMED = "CONFIRMED";
    private static final String CANCELLED = "CANCELLED";
    private static final String CHECKED_IN = "CHECKED_IN";
    private static final String CHECKED_OUT = "CHECKED_OUT";
    private static final String MAINTENANCE = "MAINTENANCE";

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final HotelBookingRepository bookingRepository;
    private final HotelReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;

    @Override
    @Transactional(readOnly = true)
    public List<HotelResponse> searchHotels(Integer destinationId, String status, String query) {
        List<Hotel> hotels;
        if (destinationId != null) {
            hotels = hotelRepository.findByDestinationId(destinationId);
        } else if (status != null && !status.isBlank()) {
            hotels = hotelRepository.findByStatusIgnoreCase(status);
        } else if (query != null && !query.isBlank()) {
            hotels = hotelRepository.findByHotelNameContainingIgnoreCase(query);
        } else {
            hotels = hotelRepository.findAll();
        }
        return hotels.stream().map(this::toHotelResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public HotelDetailsResponse getHotelDetails(UUID hotelId) {
        Hotel hotel = getHotel(hotelId);
        List<RoomResponse> rooms = roomRepository.findByHotelId(hotelId).stream().map(this::toRoomResponse).toList();
        String suggestion = rooms.stream().anyMatch(room -> AVAILABLE.equalsIgnoreCase(room.availabilityStatus()))
                ? "Rooms are available for this hotel"
                : "No available rooms right now";
        return new HotelDetailsResponse(toHotelResponse(hotel), rooms, suggestion);
    }

    @Override
    @Transactional
    public HotelResponse createHotel(HotelRequest request) {
        Hotel hotel = new Hotel();
        applyHotelRequest(hotel, request);
        return toHotelResponse(hotelRepository.save(hotel));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelResponse> getProviderHotels() {
        return hotelRepository.findAll().stream().map(this::toHotelResponse).toList();
    }

    @Override
    @Transactional
    public HotelResponse updateHotel(UUID hotelId, HotelRequest request) {
        Hotel hotel = getHotel(hotelId);
        applyHotelRequest(hotel, request);
        return toHotelResponse(hotelRepository.save(hotel));
    }

    @Override
    @Transactional
    public HotelResponse updatePolicies(UUID hotelId, HotelPolicyRequest request) {
        Hotel hotel = getHotel(hotelId);
        hotel.setPolicies(request.policies());
        return toHotelResponse(hotelRepository.save(hotel));
    }

    @Override
    @Transactional
    public RoomResponse createRoom(UUID hotelId, RoomRequest request) {
        Room room = new Room();
        room.setHotel(getHotel(hotelId));
        applyRoomRequest(room, request);
        return toRoomResponse(roomRepository.save(room));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getRooms(UUID hotelId) {
        ensureHotelExists(hotelId);
        return roomRepository.findByHotelId(hotelId).stream().map(this::toRoomResponse).toList();
    }

    @Override
    @Transactional
    public RoomResponse updateRoom(UUID hotelId, UUID roomId, RoomRequest request) {
        ensureHotelExists(hotelId);
        Room room = getRoom(roomId);
        if (!room.getHotel().getId().equals(hotelId)) {
            throw new InvalidRequestException("Room does not belong to hotel " + hotelId);
        }
        applyRoomRequest(room, request);
        return toRoomResponse(roomRepository.save(room));
    }

    @Override
    @Transactional
    public RoomResponse updateAvailability(UUID roomId, RoomAvailabilityRequest request) {
        Room room = getRoom(roomId);
        room.setAvailabilityStatus(request.availabilityStatus());
        return toRoomResponse(roomRepository.save(room));
    }

    @Override
    @Transactional
    public RoomResponse blockMaintenance(UUID roomId) {
        Room room = getRoom(roomId);
        room.setAvailabilityStatus(MAINTENANCE);
        return toRoomResponse(roomRepository.save(room));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getInventory() {
        return roomRepository.findAll().stream().map(this::toRoomResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingValidationResponse validateBooking(HotelBookingRequest request) {
        List<String> errors = validate(request);
        return new BookingValidationResponse(errors.isEmpty(), errors);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingQuoteResponse quoteBooking(HotelBookingRequest request) {
        List<String> errors = validate(request);
        if (!errors.isEmpty()) {
            throw new InvalidRequestException(String.join("; ", errors));
        }
        Room room = findAvailableRoom(request);
        long nights = ChronoUnit.DAYS.between(request.checkInDate(), request.checkOutDate());
        BigDecimal total = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));
        return new BookingQuoteResponse(
                request.hotelId(),
                request.roomType(),
                request.checkInDate(),
                request.checkOutDate(),
                nights,
                room.getPricePerNight(),
                total
        );
    }

    @Override
    @Transactional
    public HotelBookingResponse createBooking(HotelBookingRequest request, String currentUserEmail) {
        BookingQuoteResponse quote = quoteBooking(request);
        HotelBooking booking = new HotelBooking();
        booking.setTripId(request.tripId());
        booking.setHotel(getHotel(request.hotelId()));
        booking.setBookedBy(getCurrentUser(currentUserEmail));
        booking.setCheckInDate(request.checkInDate());
        booking.setCheckOutDate(request.checkOutDate());
        booking.setRoomType(request.roomType());
        booking.setRoomNumber(request.roomNumber());
        booking.setTotalAmount(quote.totalAmount());
        booking.setBookingStatus(CONFIRMED);
        return toBookingResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional(readOnly = true)
    public HotelBookingResponse getBooking(UUID bookingId) {
        return toBookingResponse(getBookingEntity(bookingId));
    }

    @Override
    @Transactional
    public HotelBookingResponse updateBooking(UUID bookingId, HotelBookingRequest request) {
        BookingQuoteResponse quote = quoteBooking(request);
        HotelBooking booking = getBookingEntity(bookingId);
        booking.setTripId(request.tripId());
        booking.setHotel(getHotel(request.hotelId()));
        booking.setCheckInDate(request.checkInDate());
        booking.setCheckOutDate(request.checkOutDate());
        booking.setRoomType(request.roomType());
        booking.setRoomNumber(request.roomNumber());
        booking.setTotalAmount(quote.totalAmount());
        return toBookingResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public HotelBookingResponse cancelBooking(UUID bookingId) {
        HotelBooking booking = getBookingEntity(bookingId);
        booking.setBookingStatus(CANCELLED);
        return toBookingResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelBookingResponse> getMyBookings(String currentUserEmail) {
        return bookingRepository.findByBookedByEmail(currentUserEmail).stream().map(this::toBookingResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public HotelBillResponse getBill(UUID bookingId) {
        HotelBooking booking = getBookingEntity(bookingId);
        return new HotelBillResponse(
                booking.getId(),
                booking.getHotel().getHotelName(),
                booking.getRoomType(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate()),
                booking.getTotalAmount(),
                booking.getBookingStatus()
        );
    }

    @Override
    @Transactional
    public HotelBookingResponse attachBookingToTrip(UUID tripId, AttachHotelBookingRequest request) {
        ensureTripExists(tripId);
        HotelBooking booking = getBookingEntity(request.bookingId());
        booking.setTripId(tripId);
        return toBookingResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public void removeBookingFromTrip(UUID tripId, UUID bookingId) {
        HotelBooking booking = getBookingEntity(bookingId);
        if (!tripId.equals(booking.getTripId())) {
            throw new InvalidRequestException("Booking is not attached to trip " + tripId);
        }
        booking.setTripId(null);
        bookingRepository.save(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public AccommodationSummaryResponse getAccommodationSummary(UUID tripId) {
        ensureTripExists(tripId);
        List<HotelBookingResponse> bookings = bookingRepository.findByTripId(tripId)
                .stream()
                .map(this::toBookingResponse)
                .toList();
        BigDecimal total = bookings.stream()
                .map(HotelBookingResponse::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AccommodationSummaryResponse(tripId, bookings.size(), total, bookings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelReviewResponse> getReviews(UUID hotelId) {
        ensureHotelExists(hotelId);
        return reviewRepository.findByHotelId(hotelId).stream().map(this::toReviewResponse).toList();
    }

    @Override
    @Transactional
    public HotelReviewResponse addReview(UUID hotelId, ReviewRequest request, String currentUserEmail) {
        HotelReview review = new HotelReview();
        review.setHotel(getHotel(hotelId));
        review.setUser(getCurrentUser(currentUserEmail));
        review.setRating(request.rating());
        review.setComment(request.comment());
        return toReviewResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional
    public HotelReviewResponse updateReview(UUID hotelId, UUID reviewId, ReviewRequest request, String currentUserEmail) {
        HotelReview review = getReviewForHotel(hotelId, reviewId);
        ensureReviewOwner(review, currentUserEmail);
        review.setRating(request.rating());
        review.setComment(request.comment());
        return toReviewResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional
    public void deleteReview(UUID hotelId, UUID reviewId, String currentUserEmail) {
        HotelReview review = getReviewForHotel(hotelId, reviewId);
        ensureReviewOwner(review, currentUserEmail);
        reviewRepository.delete(review);
    }

    @Override
    @Transactional
    public HotelBookingResponse checkIn(UUID bookingId) {
        HotelBooking booking = getBookingEntity(bookingId);
        booking.setBookingStatus(CHECKED_IN);
        return toBookingResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public HotelBookingResponse checkOut(UUID bookingId) {
        HotelBooking booking = getBookingEntity(bookingId);
        booking.setBookingStatus(CHECKED_OUT);
        return toBookingResponse(bookingRepository.save(booking));
    }

    private List<String> validate(HotelBookingRequest request) {
        List<String> errors = new ArrayList<>();
        if (!hotelRepository.existsById(request.hotelId())) {
            errors.add("Hotel not found");
        }
        if (request.checkInDate() != null && request.checkOutDate() != null
                && !request.checkOutDate().isAfter(request.checkInDate())) {
            errors.add("Check-out date must be after check-in date");
        }
        if (request.hotelId() != null && request.roomType() != null
                && roomRepository.findFirstByHotelIdAndRoomTypeIgnoreCaseAndAvailabilityStatusIgnoreCase(
                request.hotelId(),
                request.roomType(),
                AVAILABLE
        ).isEmpty()) {
            errors.add("No available room found for requested room type");
        }
        return errors;
    }

    private Room findAvailableRoom(HotelBookingRequest request) {
        return roomRepository.findFirstByHotelIdAndRoomTypeIgnoreCaseAndAvailabilityStatusIgnoreCase(
                        request.hotelId(),
                        request.roomType(),
                        AVAILABLE
                )
                .orElseThrow(() -> new InvalidRequestException("No available room found for requested room type"));
    }

    private void applyHotelRequest(Hotel hotel, HotelRequest request) {
        hotel.setDestinationId(request.destinationId());
        hotel.setHotelName(request.hotelName());
        hotel.setAddress(request.address());
        hotel.setRating(request.rating());
        hotel.setPricePerNight(request.pricePerNight());
        hotel.setAmenities(request.amenities());
        hotel.setStatus(request.status());
    }

    private void applyRoomRequest(Room room, RoomRequest request) {
        room.setRoomType(request.roomType());
        room.setCapacity(request.capacity());
        room.setBedType(request.bedType());
        room.setPricePerNight(request.pricePerNight());
        room.setAvailabilityStatus(request.availabilityStatus());
    }

    private Hotel getHotel(UUID hotelId) {
        return hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel with id " + hotelId + " not found"));
    }

    private void ensureHotelExists(UUID hotelId) {
        if (!hotelRepository.existsById(hotelId)) {
            throw new ResourceNotFoundException("Hotel with id " + hotelId + " not found");
        }
    }

    private Room getRoom(UUID roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room with id " + roomId + " not found"));
    }

    private HotelBooking getBookingEntity(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel booking with id " + bookingId + " not found"));
    }

    private HotelReview getReviewForHotel(UUID hotelId, UUID reviewId) {
        HotelReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review with id " + reviewId + " not found"));
        if (!review.getHotel().getId().equals(hotelId)) {
            throw new InvalidRequestException("Review does not belong to hotel " + hotelId);
        }
        return review;
    }

    private User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User with email " + email + " not found"));
    }

    private void ensureTripExists(UUID tripId) {
        if (!tripRepository.existsById(tripId)) {
            throw new ResourceNotFoundException("Trip with id " + tripId + " not found");
        }
    }

    private void ensureReviewOwner(HotelReview review, String email) {
        if (!Objects.equals(review.getUser().getEmail(), email)) {
            throw new AccessDeniedException("Current user does not own this review");
        }
    }

    private HotelResponse toHotelResponse(Hotel hotel) {
        return new HotelResponse(
                hotel.getId(),
                hotel.getDestinationId(),
                hotel.getHotelName(),
                hotel.getAddress(),
                hotel.getRating(),
                hotel.getPricePerNight(),
                hotel.getAmenities(),
                hotel.getStatus(),
                hotel.getPolicies()
        );
    }

    private RoomResponse toRoomResponse(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getHotel().getId(),
                room.getRoomType(),
                room.getCapacity(),
                room.getBedType(),
                room.getPricePerNight(),
                room.getAvailabilityStatus()
        );
    }

    private HotelBookingResponse toBookingResponse(HotelBooking booking) {
        return new HotelBookingResponse(
                booking.getId(),
                booking.getTripId(),
                booking.getHotel().getId(),
                booking.getHotel().getHotelName(),
                booking.getBookedBy() == null ? null : booking.getBookedBy().getId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getRoomType(),
                booking.getRoomNumber(),
                booking.getTotalAmount(),
                booking.getBookingStatus()
        );
    }

    private HotelReviewResponse toReviewResponse(HotelReview review) {
        return new HotelReviewResponse(
                review.getId(),
                review.getHotel().getId(),
                review.getUser().getId(),
                review.getUser().getName(),
                review.getRating(),
                review.getComment()
        );
    }
}
