package com.travelease.backend.accommodation.service;

import com.travelease.backend.accommodation.dto.*;
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
import com.travelease.backend.trip.entity.Trip;
import com.travelease.backend.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AccommodationServiceImpl implements AccommodationService {

    private static final String ACTIVE = "ACTIVE";
    private static final String AVAILABLE = "AVAILABLE";
    private static final String BOOKED = "BOOKED";
    private static final String MAINTENANCE = "MAINTENANCE";
    private static final String CONFIRMED = "CONFIRMED";
    private static final String CANCELLED = "CANCELLED";
    private static final String CHECKED_IN = "CHECKED_IN";
    private static final String CHECKED_OUT = "CHECKED_OUT";

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final HotelBookingRepository bookingRepository;
    private final HotelReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;

    @Override
    @Transactional(readOnly = true)
    public List<HotelResponse> searchHotels(Integer destinationId, String status, String query) {
        return hotelRepository.findAll().stream()
                .filter(hotel -> destinationId == null || destinationId.equals(hotel.getDestinationId()))
                .filter(hotel -> isBlank(status) || status.equalsIgnoreCase(hotel.getStatus()))
                .filter(hotel -> isBlank(query) || hotel.getName().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))
                        || hotel.getAddress().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))
                .map(this::toHotelResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public HotelDetailsResponse getHotelDetails(UUID hotelId) {
        Hotel hotel = getHotel(hotelId);
        List<RoomResponse> rooms = roomRepository.findByHotelId(hotelId).stream().map(this::toRoomResponse).toList();
        String suggestion = rooms.stream().anyMatch(room -> AVAILABLE.equalsIgnoreCase(room.availabilityStatus()))
                ? "Rooms are available for booking"
                : "No rooms are currently available";
        return new HotelDetailsResponse(toHotelResponse(hotel), rooms, suggestion);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingValidationResponse validateBooking(HotelBookingRequest request) {
        List<String> errors = new ArrayList<>();
        if (request.checkOutDate() == null || request.checkInDate() == null
                || !request.checkOutDate().isAfter(request.checkInDate())) {
            errors.add("Check-out date must be after check-in date");
        }
        Hotel hotel = hotelRepository.findById(request.hotelId()).orElse(null);
        if (hotel == null) {
            errors.add("Hotel not found");
        } else if (!ACTIVE.equalsIgnoreCase(hotel.getStatus())) {
            errors.add("Hotel is not active");
        } else {
            Room room = findAvailableRoomOrNull(request.hotelId(), request.roomType());
            if (room == null) {
                errors.add("No available room found for requested room type");
            } else if (request.guests() != null && request.guests() > room.getCapacity()) {
                errors.add("Guest count exceeds room capacity");
            }
        }
        return new BookingValidationResponse(errors.isEmpty(), errors);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingQuoteResponse quoteBooking(HotelBookingRequest request) {
        ensureValidBookingRequest(request);
        Room room = findAvailableRoom(request.hotelId(), request.roomType());
        long nights = nights(request);
        BigDecimal total = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));
        return new BookingQuoteResponse(request.hotelId(), room.getId(), room.getRoomType(),
                request.checkInDate(), request.checkOutDate(), nights, room.getPricePerNight(), total);
    }

    @Override
    public HotelBookingResponse createBooking(HotelBookingRequest request, String userEmail) {
        ensureValidBookingRequest(request);
        User user = getUser(userEmail);
        Hotel hotel = getHotel(request.hotelId());
        Room room = findAvailableRoom(request.hotelId(), request.roomType());
        ensureCapacity(room, request.guests());

        HotelBooking booking = new HotelBooking();
        booking.setBookedBy(user);
        booking.setHotel(hotel);
        booking.setRoom(room);
        booking.setCheckInDate(request.checkInDate());
        booking.setCheckOutDate(request.checkOutDate());
        booking.setGuests(request.guests());
        booking.setBookingStatus(CONFIRMED);
        booking.setTotalAmount(room.getPricePerNight().multiply(BigDecimal.valueOf(nights(request))));

        room.setAvailabilityStatus(BOOKED);
        roomRepository.save(room);
        return toBookingResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional(readOnly = true)
    public HotelBookingResponse getBooking(UUID bookingId, String userEmail) {
        HotelBooking booking = getBookingEntity(bookingId);
        ensureBookingOwner(booking, userEmail);
        return toBookingResponse(booking);
    }

    @Override
    public HotelBookingResponse modifyBooking(UUID bookingId, HotelBookingRequest request, String userEmail) {
        HotelBooking booking = getBookingEntity(bookingId);
        ensureBookingOwner(booking, userEmail);
        ensureMutableBooking(booking);
        ensureValidBookingRequest(request);

        Room oldRoom = booking.getRoom();
        oldRoom.setAvailabilityStatus(AVAILABLE);
        Room newRoom = findAvailableRoom(request.hotelId(), request.roomType());
        ensureCapacity(newRoom, request.guests());
        newRoom.setAvailabilityStatus(BOOKED);

        booking.setHotel(getHotel(request.hotelId()));
        booking.setRoom(newRoom);
        booking.setCheckInDate(request.checkInDate());
        booking.setCheckOutDate(request.checkOutDate());
        booking.setGuests(request.guests());
        booking.setTotalAmount(newRoom.getPricePerNight().multiply(BigDecimal.valueOf(nights(request))));
        roomRepository.save(oldRoom);
        roomRepository.save(newRoom);
        return toBookingResponse(bookingRepository.save(booking));
    }

    @Override
    public HotelBookingResponse cancelBooking(UUID bookingId, String userEmail) {
        HotelBooking booking = getBookingEntity(bookingId);
        ensureBookingOwner(booking, userEmail);
        if (CANCELLED.equalsIgnoreCase(booking.getBookingStatus())) {
            throw new InvalidRequestException("Booking is already cancelled");
        }
        if (CHECKED_OUT.equalsIgnoreCase(booking.getBookingStatus())) {
            throw new InvalidRequestException("Checked-out booking cannot be cancelled");
        }
        booking.setBookingStatus(CANCELLED);
        booking.getRoom().setAvailabilityStatus(AVAILABLE);
        roomRepository.save(booking.getRoom());
        return toBookingResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelBookingResponse> getMyBookings(String userEmail) {
        return bookingRepository.findByBookedByEmailOrderByCreatedAtDesc(userEmail).stream()
                .map(this::toBookingResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelReviewResponse> getReviews(UUID hotelId) {
        getHotel(hotelId);
        return reviewRepository.findByHotelIdOrderByCreatedAtDesc(hotelId).stream()
                .map(this::toReviewResponse)
                .toList();
    }

    @Override
    public HotelReviewResponse addReview(UUID hotelId, ReviewRequest request, String userEmail) {
        HotelReview review = new HotelReview();
        review.setHotel(getHotel(hotelId));
        review.setUser(getUser(userEmail));
        review.setRating(request.rating());
        review.setComment(request.comment());
        return toReviewResponse(reviewRepository.save(review));
    }

    @Override
    public HotelReviewResponse updateReview(UUID hotelId, UUID reviewId, ReviewRequest request, String userEmail) {
        HotelReview review = getReview(hotelId, reviewId);
        ensureReviewOwner(review, userEmail);
        review.setRating(request.rating());
        review.setComment(request.comment());
        return toReviewResponse(reviewRepository.save(review));
    }

    @Override
    public void deleteReview(UUID hotelId, UUID reviewId, String userEmail) {
        HotelReview review = getReview(hotelId, reviewId);
        ensureReviewOwner(review, userEmail);
        reviewRepository.delete(review);
    }

    @Override
    @Transactional(readOnly = true)
    public HotelBillResponse getBill(UUID bookingId, String userEmail) {
        HotelBooking booking = getBookingEntity(bookingId);
        ensureBookingOwner(booking, userEmail);
        return new HotelBillResponse(booking.getId(), booking.getHotel().getName(), booking.getBookedBy().getName(),
                booking.getRoom().getRoomType(), ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate()),
                booking.getTotalAmount(), booking.getBookingStatus());
    }

    @Override
    public HotelBookingResponse attachBookingToTrip(UUID tripId, AttachHotelBookingRequest request, String userEmail) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        HotelBooking booking = getBookingEntity(request.bookingId());
        ensureBookingOwner(booking, userEmail);
        booking.setTrip(trip);
        return toBookingResponse(bookingRepository.save(booking));
    }

    @Override
    public void removeBookingFromTrip(UUID tripId, UUID bookingId, String userEmail) {
        HotelBooking booking = getBookingEntity(bookingId);
        ensureBookingOwner(booking, userEmail);
        if (booking.getTrip() == null || !tripId.equals(booking.getTrip().getId())) {
            throw new InvalidRequestException("Booking is not attached to this trip");
        }
        booking.setTrip(null);
        bookingRepository.save(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public AccommodationSummaryResponse getTripAccommodationSummary(UUID tripId, String userEmail) {
        tripRepository.findById(tripId).orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        List<HotelBookingResponse> bookings = bookingRepository.findByTripId(tripId).stream()
                .map(this::toBookingResponse)
                .toList();
        BigDecimal total = bookings.stream()
                .map(HotelBookingResponse::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AccommodationSummaryResponse(tripId, bookings.size(), total, bookings);
    }

    @Override
    public HotelResponse createHotel(HotelRequest request, String providerEmail) {
        Hotel hotel = new Hotel();
        hotel.setProvider(getUser(providerEmail));
        applyHotelRequest(hotel, request);
        return toHotelResponse(hotelRepository.save(hotel));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelResponse> getProviderHotels(String providerEmail) {
        return hotelRepository.findByProviderEmail(providerEmail).stream()
                .map(this::toHotelResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public HotelDetailsResponse getProviderHotel(UUID hotelId, String providerEmail) {
        Hotel hotel = getProviderOwnedHotel(hotelId, providerEmail);
        List<RoomResponse> rooms = roomRepository.findByHotelId(hotel.getId()).stream().map(this::toRoomResponse).toList();
        return new HotelDetailsResponse(toHotelResponse(hotel), rooms, "Provider hotel details");
    }

    @Override
    public HotelResponse updateHotel(UUID hotelId, HotelRequest request, String providerEmail) {
        Hotel hotel = getProviderOwnedHotel(hotelId, providerEmail);
        applyHotelRequest(hotel, request);
        return toHotelResponse(hotelRepository.save(hotel));
    }

    @Override
    public RoomResponse createRoom(UUID hotelId, RoomRequest request, String providerEmail) {
        Hotel hotel = getProviderOwnedHotel(hotelId, providerEmail);
        Room room = new Room();
        room.setHotel(hotel);
        applyRoomRequest(room, request);
        return toRoomResponse(roomRepository.save(room));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getRooms(UUID hotelId, String providerEmail) {
        getProviderOwnedHotel(hotelId, providerEmail);
        return roomRepository.findByHotelId(hotelId).stream().map(this::toRoomResponse).toList();
    }

    @Override
    public RoomResponse updateRoom(UUID hotelId, UUID roomId, RoomRequest request, String providerEmail) {
        getProviderOwnedHotel(hotelId, providerEmail);
        Room room = getRoom(roomId);
        if (!hotelId.equals(room.getHotel().getId())) {
            throw new InvalidRequestException("Room does not belong to this hotel");
        }
        applyRoomRequest(room, request);
        return toRoomResponse(roomRepository.save(room));
    }

    @Override
    public RoomResponse updateRoomAvailability(UUID roomId, RoomAvailabilityRequest request, String providerEmail) {
        Room room = getProviderOwnedRoom(roomId, providerEmail);
        room.setAvailabilityStatus(normalizeStatus(request.availabilityStatus(), AVAILABLE));
        return toRoomResponse(roomRepository.save(room));
    }

    @Override
    public RoomResponse blockRoomForMaintenance(UUID roomId, String providerEmail) {
        Room room = getProviderOwnedRoom(roomId, providerEmail);
        if (BOOKED.equalsIgnoreCase(room.getAvailabilityStatus())) {
            throw new InvalidRequestException("Booked room cannot be blocked for maintenance");
        }
        room.setAvailabilityStatus(MAINTENANCE);
        return toRoomResponse(roomRepository.save(room));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getProviderInventory(String providerEmail) {
        return hotelRepository.findByProviderEmail(providerEmail).stream()
                .flatMap(hotel -> roomRepository.findByHotelId(hotel.getId()).stream())
                .map(this::toRoomResponse)
                .toList();
    }

    @Override
    public HotelResponse updatePolicies(UUID hotelId, HotelPolicyRequest request, String providerEmail) {
        Hotel hotel = getProviderOwnedHotel(hotelId, providerEmail);
        hotel.setPolicies(request.policies());
        return toHotelResponse(hotelRepository.save(hotel));
    }

    @Override
    public HotelBookingResponse checkIn(UUID bookingId, String providerEmail) {
        HotelBooking booking = getProviderOwnedBooking(bookingId, providerEmail);
        if (!CONFIRMED.equalsIgnoreCase(booking.getBookingStatus())) {
            throw new InvalidRequestException("Only confirmed bookings can be checked in");
        }
        booking.setBookingStatus(CHECKED_IN);
        return toBookingResponse(bookingRepository.save(booking));
    }

    @Override
    public HotelBookingResponse checkOut(UUID bookingId, String providerEmail) {
        HotelBooking booking = getProviderOwnedBooking(bookingId, providerEmail);
        if (!CHECKED_IN.equalsIgnoreCase(booking.getBookingStatus())) {
            throw new InvalidRequestException("Only checked-in bookings can be checked out");
        }
        booking.setBookingStatus(CHECKED_OUT);
        booking.getRoom().setAvailabilityStatus(AVAILABLE);
        roomRepository.save(booking.getRoom());
        return toBookingResponse(bookingRepository.save(booking));
    }

    private void applyHotelRequest(Hotel hotel, HotelRequest request) {
        hotel.setName(request.name());
        hotel.setDestinationId(request.destinationId());
        hotel.setAddress(request.address());
        hotel.setDescription(request.description());
        hotel.setStatus(normalizeStatus(request.status(), ACTIVE));
    }

    private void applyRoomRequest(Room room, RoomRequest request) {
        room.setRoomType(request.roomType());
        room.setCapacity(request.capacity());
        room.setBedType(request.bedType());
        room.setPricePerNight(request.pricePerNight());
        room.setAvailabilityStatus(normalizeStatus(request.availabilityStatus(), AVAILABLE));
    }

    private Hotel getHotel(UUID hotelId) {
        return hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found"));
    }

    private Room getRoom(UUID roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
    }

    private HotelBooking getBookingEntity(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel booking not found"));
    }

    private HotelReview getReview(UUID hotelId, UUID reviewId) {
        HotelReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        if (!hotelId.equals(review.getHotel().getId())) {
            throw new InvalidRequestException("Review does not belong to this hotel");
        }
        return review;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Hotel getProviderOwnedHotel(UUID hotelId, String providerEmail) {
        Hotel hotel = getHotel(hotelId);
        if (!providerEmail.equalsIgnoreCase(hotel.getProvider().getEmail())) {
            throw new AccessDeniedException("Hotel does not belong to current provider");
        }
        return hotel;
    }

    private Room getProviderOwnedRoom(UUID roomId, String providerEmail) {
        Room room = getRoom(roomId);
        if (!providerEmail.equalsIgnoreCase(room.getHotel().getProvider().getEmail())) {
            throw new AccessDeniedException("Room does not belong to current provider");
        }
        return room;
    }

    private HotelBooking getProviderOwnedBooking(UUID bookingId, String providerEmail) {
        HotelBooking booking = getBookingEntity(bookingId);
        if (!providerEmail.equalsIgnoreCase(booking.getHotel().getProvider().getEmail())) {
            throw new AccessDeniedException("Booking does not belong to current provider");
        }
        return booking;
    }

    private Room findAvailableRoom(UUID hotelId, String roomType) {
        Room room = findAvailableRoomOrNull(hotelId, roomType);
        if (room == null) {
            throw new InvalidRequestException("No available room found for requested room type");
        }
        return room;
    }

    private Room findAvailableRoomOrNull(UUID hotelId, String roomType) {
        return roomRepository.findFirstByHotelIdAndRoomTypeIgnoreCaseAndAvailabilityStatusIgnoreCase(
                hotelId,
                roomType,
                AVAILABLE
        ).orElse(null);
    }

    private void ensureValidBookingRequest(HotelBookingRequest request) {
        BookingValidationResponse validation = validateBooking(request);
        if (!validation.valid()) {
            throw new InvalidRequestException(String.join(", ", validation.errors()));
        }
    }

    private void ensureCapacity(Room room, Integer guests) {
        if (guests != null && guests > room.getCapacity()) {
            throw new InvalidRequestException("Guest count exceeds room capacity");
        }
    }

    private void ensureBookingOwner(HotelBooking booking, String userEmail) {
        if (!userEmail.equalsIgnoreCase(booking.getBookedBy().getEmail())) {
            throw new AccessDeniedException("Booking does not belong to current user");
        }
    }

    private void ensureReviewOwner(HotelReview review, String userEmail) {
        if (!userEmail.equalsIgnoreCase(review.getUser().getEmail())) {
            throw new AccessDeniedException("Review does not belong to current user");
        }
    }

    private void ensureMutableBooking(HotelBooking booking) {
        if (CANCELLED.equalsIgnoreCase(booking.getBookingStatus())
                || CHECKED_IN.equalsIgnoreCase(booking.getBookingStatus())
                || CHECKED_OUT.equalsIgnoreCase(booking.getBookingStatus())) {
            throw new InvalidRequestException("Booking cannot be modified in status " + booking.getBookingStatus());
        }
    }

    private long nights(HotelBookingRequest request) {
        return ChronoUnit.DAYS.between(request.checkInDate(), request.checkOutDate());
    }

    private String normalizeStatus(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private HotelResponse toHotelResponse(Hotel hotel) {
        return new HotelResponse(hotel.getId(), hotel.getName(), hotel.getDestinationId(), hotel.getAddress(),
                hotel.getDescription(), hotel.getStatus(), hotel.getPolicies());
    }

    private RoomResponse toRoomResponse(Room room) {
        return new RoomResponse(room.getId(), room.getHotel().getId(), room.getRoomType(), room.getCapacity(),
                room.getBedType(), room.getPricePerNight(), room.getAvailabilityStatus());
    }

    private HotelBookingResponse toBookingResponse(HotelBooking booking) {
        UUID tripId = booking.getTrip() == null ? null : booking.getTrip().getId();
        return new HotelBookingResponse(booking.getId(), booking.getHotel().getId(), booking.getHotel().getName(),
                booking.getRoom().getId(), booking.getRoom().getRoomType(), tripId, booking.getCheckInDate(),
                booking.getCheckOutDate(), booking.getGuests(), booking.getBookingStatus(), booking.getTotalAmount());
    }

    private HotelReviewResponse toReviewResponse(HotelReview review) {
        return new HotelReviewResponse(review.getId(), review.getHotel().getId(), review.getUser().getId(),
                review.getUser().getName(), review.getRating(), review.getComment());
    }
}
