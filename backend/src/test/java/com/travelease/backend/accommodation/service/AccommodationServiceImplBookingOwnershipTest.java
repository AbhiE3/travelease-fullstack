package com.travelease.backend.accommodation.service;

import com.travelease.backend.accommodation.dto.HotelBillResponse;
import com.travelease.backend.accommodation.dto.HotelBookingRequest;
import com.travelease.backend.accommodation.dto.HotelBookingResponse;
import com.travelease.backend.accommodation.entity.Hotel;
import com.travelease.backend.accommodation.entity.HotelBooking;
import com.travelease.backend.accommodation.entity.Room;
import com.travelease.backend.accommodation.repository.HotelBookingRepository;
import com.travelease.backend.accommodation.repository.HotelRepository;
import com.travelease.backend.accommodation.repository.HotelReviewRepository;
import com.travelease.backend.accommodation.repository.RoomRepository;
import com.travelease.backend.auth.entity.User;
import com.travelease.backend.auth.repository.UserRepository;
import com.travelease.backend.busbooking.security.SecurityUtil;
import com.travelease.backend.trip.repository.TripMemberRepository;
import com.travelease.backend.trip.repository.TripRepository;
import com.travelease.backend.trip.security.TripAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccommodationServiceImplBookingOwnershipTest {

    @Mock
    private HotelRepository hotelRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private HotelBookingRepository bookingRepository;
    @Mock
    private HotelReviewRepository reviewRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private TripMemberRepository tripMemberRepository;
    @Mock
    private SecurityUtil securityUtil;

    private AccommodationServiceImpl accommodationService;

    @BeforeEach
    void setUp() {
        accommodationService = new AccommodationServiceImpl(
                hotelRepository, roomRepository, bookingRepository, reviewRepository, userRepository, tripRepository,
                new TripAuthorizationService(tripMemberRepository), securityUtil);
    }

    private User user(String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setName(email);
        return user;
    }

    private Hotel hotel() {
        Hotel hotel = new Hotel();
        hotel.setDestinationId(1);
        hotel.setHotelName("Test Hotel");
        hotel.setAddress("Somewhere");
        hotel.setPricePerNight(new BigDecimal("100.00"));
        hotel.setStatus("ACTIVE");
        return hotel;
    }

    private HotelBooking bookingOwnedBy(User owner) {
        HotelBooking booking = new HotelBooking();
        booking.setHotel(hotel());
        booking.setBookedBy(owner);
        booking.setCheckInDate(LocalDate.now().plusDays(1));
        booking.setCheckOutDate(LocalDate.now().plusDays(3));
        booking.setRoomType("DELUXE");
        booking.setTotalAmount(new BigDecimal("200.00"));
        booking.setBookingStatus("CONFIRMED");
        return booking;
    }

    // --- getBooking ---

    @Test
    void ownerCanGetOwnBooking() {
        User alice = user("alice@travelease.test");
        HotelBooking booking = bookingOwnedBy(alice);
        UUID bookingId = booking.getId();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        HotelBookingResponse response = accommodationService.getBooking(bookingId, alice.getEmail());

        assertThat(response.hotelBookingId()).isEqualTo(bookingId);
    }

    @Test
    void anotherTravelerCannotGetBooking() {
        User alice = user("alice@travelease.test");
        HotelBooking booking = bookingOwnedBy(alice);
        UUID bookingId = booking.getId();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> accommodationService.getBooking(bookingId, "bob@travelease.test"))
                .isInstanceOf(AccessDeniedException.class);
    }

    // --- updateBooking ---

    @Test
    void ownerCanUpdateOwnBooking() {
        User alice = user("alice@travelease.test");
        HotelBooking booking = bookingOwnedBy(alice);
        UUID bookingId = booking.getId();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(HotelBooking.class))).thenAnswer(inv -> inv.getArgument(0));

        UUID hotelId = UUID.randomUUID();
        Hotel hotel = hotel();
        Room room = new Room();
        room.setHotel(hotel);
        room.setRoomType("DELUXE");
        room.setCapacity(2);
        room.setBedType("KING");
        room.setPricePerNight(new BigDecimal("120.00"));
        room.setAvailabilityStatus("AVAILABLE");

        when(hotelRepository.existsById(hotelId)).thenReturn(true);
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(hotel));
        when(roomRepository.findFirstByHotelIdAndRoomTypeIgnoreCaseAndAvailabilityStatusIgnoreCase(
                hotelId, "DELUXE", "AVAILABLE")).thenReturn(Optional.of(room));

        HotelBookingRequest request = new HotelBookingRequest(
                null, hotelId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), "DELUXE", null);

        HotelBookingResponse response = accommodationService.updateBooking(bookingId, request, alice.getEmail());

        assertThat(response.hotelBookingId()).isEqualTo(bookingId);
    }

    @Test
    void anotherTravelerCannotUpdateBooking() {
        User alice = user("alice@travelease.test");
        HotelBooking booking = bookingOwnedBy(alice);
        UUID bookingId = booking.getId();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        HotelBookingRequest request = new HotelBookingRequest(
                null, UUID.randomUUID(), LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), "DELUXE", null);

        assertThatThrownBy(() -> accommodationService.updateBooking(bookingId, request, "bob@travelease.test"))
                .isInstanceOf(AccessDeniedException.class);
    }

    // --- cancelBooking ---

    @Test
    void ownerCanCancelOwnBooking() {
        User alice = user("alice@travelease.test");
        HotelBooking booking = bookingOwnedBy(alice);
        UUID bookingId = booking.getId();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(HotelBooking.class))).thenAnswer(inv -> inv.getArgument(0));

        HotelBookingResponse response = accommodationService.cancelBooking(bookingId, alice.getEmail());

        assertThat(response.bookingStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void anotherTravelerCannotCancelBooking() {
        User alice = user("alice@travelease.test");
        HotelBooking booking = bookingOwnedBy(alice);
        UUID bookingId = booking.getId();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> accommodationService.cancelBooking(bookingId, "bob@travelease.test"))
                .isInstanceOf(AccessDeniedException.class);
    }

    // --- getBill ---

    @Test
    void ownerCanRetrieveOwnBill() {
        User alice = user("alice@travelease.test");
        HotelBooking booking = bookingOwnedBy(alice);
        UUID bookingId = booking.getId();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        HotelBillResponse bill = accommodationService.getBill(bookingId, alice.getEmail());

        assertThat(bill.bookingId()).isEqualTo(bookingId);
    }

    @Test
    void anotherTravelerCannotRetrieveBill() {
        User alice = user("alice@travelease.test");
        HotelBooking booking = bookingOwnedBy(alice);
        UUID bookingId = booking.getId();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> accommodationService.getBill(bookingId, "bob@travelease.test"))
                .isInstanceOf(AccessDeniedException.class);
    }

    // --- regression: creation and self-listing unaffected ---

    @Test
    void bookingCreationStillWorks() {
        User alice = user("alice@travelease.test");
        UUID hotelId = UUID.randomUUID();
        Hotel hotel = hotel();
        Room room = new Room();
        room.setHotel(hotel);
        room.setRoomType("DELUXE");
        room.setCapacity(2);
        room.setBedType("KING");
        room.setPricePerNight(new BigDecimal("120.00"));
        room.setAvailabilityStatus("AVAILABLE");

        when(hotelRepository.existsById(hotelId)).thenReturn(true);
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(hotel));
        when(roomRepository.findFirstByHotelIdAndRoomTypeIgnoreCaseAndAvailabilityStatusIgnoreCase(
                hotelId, "DELUXE", "AVAILABLE")).thenReturn(Optional.of(room));
        when(userRepository.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));
        when(bookingRepository.save(any(HotelBooking.class))).thenAnswer(inv -> inv.getArgument(0));

        HotelBookingRequest request = new HotelBookingRequest(
                null, hotelId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), "DELUXE", null);

        HotelBookingResponse response = accommodationService.createBooking(request, alice.getEmail());

        assertThat(response.bookingStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    void currentUserBookingListingStillWorks() {
        User alice = user("alice@travelease.test");
        HotelBooking booking = bookingOwnedBy(alice);
        when(bookingRepository.findByBookedByEmail(alice.getEmail())).thenReturn(java.util.List.of(booking));

        var responses = accommodationService.getMyBookings(alice.getEmail());

        assertThat(responses).hasSize(1);
    }
}
