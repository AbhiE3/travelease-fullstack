package com.travelease.backend.accommodation.service;

import com.travelease.backend.accommodation.dto.HotelRequest;
import com.travelease.backend.accommodation.dto.RoomAvailabilityRequest;
import com.travelease.backend.accommodation.dto.RoomRequest;
import com.travelease.backend.accommodation.entity.Hotel;
import com.travelease.backend.accommodation.entity.HotelBooking;
import com.travelease.backend.accommodation.entity.Room;
import com.travelease.backend.accommodation.repository.HotelBookingRepository;
import com.travelease.backend.accommodation.repository.HotelRepository;
import com.travelease.backend.accommodation.repository.HotelReviewRepository;
import com.travelease.backend.accommodation.repository.RoomRepository;
import com.travelease.backend.auth.entity.Role;
import com.travelease.backend.auth.entity.User;
import com.travelease.backend.auth.repository.UserRepository;
import com.travelease.backend.busbooking.security.SecurityUtil;
import com.travelease.backend.shared.exception.InvalidRequestException;
import com.travelease.backend.trip.repository.TripMemberRepository;
import com.travelease.backend.trip.repository.TripRepository;
import com.travelease.backend.trip.security.TripAuthorizationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Hotel Provider tenant-isolation tests. Uses a real SecurityUtil (backed by a
 * mocked UserRepository) rather than mocking SecurityUtil itself, matching the
 * established convention of AccommodationServiceImplTripAuthorizationTest using a
 * real TripAuthorizationService - the point is to verify the actual role-gate +
 * ownership-assertion wiring, not merely that some method was called.
 */
@ExtendWith(MockitoExtension.class)
class AccommodationServiceImplHotelProviderOwnershipTest {

    private static final Long PROVIDER_101 = 101L;
    private static final Long PROVIDER_102 = 102L;

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

    private AccommodationServiceImpl accommodationService;

    @BeforeEach
    void setUp() {
        SecurityUtil securityUtil = new SecurityUtil(userRepository);
        accommodationService = new AccommodationServiceImpl(
                hotelRepository, roomRepository, bookingRepository, reviewRepository, userRepository, tripRepository,
                new TripAuthorizationService(tripMemberRepository), securityUtil);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String email, String role) {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, authorities));
    }

    private User hotelProviderUser(String email, Long providerId) {
        User user = new User();
        user.setEmail(email);
        user.setRole(Role.ROLE_HOTEL_PROVIDER);
        user.setProviderId(providerId);
        return user;
    }

    private Hotel hotelOwnedBy(Long providerId) {
        Hotel hotel = new Hotel();
        hotel.setProviderId(providerId);
        hotel.setDestinationId(1);
        hotel.setHotelName("Test Hotel " + providerId);
        hotel.setAddress("Somewhere");
        hotel.setPricePerNight(new BigDecimal("100.00"));
        hotel.setStatus("ACTIVE");
        return hotel;
    }

    private Room roomOf(Hotel hotel) {
        Room room = new Room();
        room.setHotel(hotel);
        room.setRoomType("STANDARD");
        room.setCapacity(2);
        room.setBedType("QUEEN");
        room.setPricePerNight(new BigDecimal("100.00"));
        room.setAvailabilityStatus("AVAILABLE");
        return room;
    }

    private HotelBooking bookingAt(Hotel hotel) {
        HotelBooking booking = new HotelBooking();
        booking.setHotel(hotel);
        booking.setCheckInDate(LocalDate.now().plusDays(1));
        booking.setCheckOutDate(LocalDate.now().plusDays(3));
        booking.setRoomType("STANDARD");
        booking.setTotalAmount(new BigDecimal("200.00"));
        booking.setBookingStatus("CONFIRMED");
        return booking;
    }

    // --- createHotel ---

    @Test
    void hotelProviderCreatesHotelWithOwnProviderIdAssignedServerSide() {
        User provider = hotelProviderUser("hotelprovider1@travelease.com", PROVIDER_101);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_HOTEL_PROVIDER");
        when(hotelRepository.save(any(Hotel.class))).thenAnswer(inv -> inv.getArgument(0));

        HotelRequest request = new HotelRequest(1, "New Hotel", "Addr", null, new BigDecimal("100.00"), null, "ACTIVE", null);
        accommodationService.createHotel(request);

        var captor = org.mockito.ArgumentCaptor.forClass(Hotel.class);
        org.mockito.Mockito.verify(hotelRepository).save(captor.capture());
        assertThat(captor.getValue().getProviderId()).isEqualTo(PROVIDER_101);
    }

    @Test
    void hotelProviderCannotCreateHotelForAnotherProviderId() {
        User provider = hotelProviderUser("hotelprovider1@travelease.com", PROVIDER_101);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_HOTEL_PROVIDER");

        HotelRequest request = new HotelRequest(1, "New Hotel", "Addr", null, new BigDecimal("100.00"), null, "ACTIVE", PROVIDER_102);

        assertThatThrownBy(() -> accommodationService.createHotel(request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void adminCreatingHotelWithoutExplicitProviderIdIsRejectedRatherThanDefaulted() {
        authenticateAs("admin@travelease.com", "ROLE_ADMIN");

        HotelRequest request = new HotelRequest(1, "New Hotel", "Addr", null, new BigDecimal("100.00"), null, "ACTIVE", null);

        assertThatThrownBy(() -> accommodationService.createHotel(request))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void adminCreatingHotelWithExplicitProviderIdWorks() {
        authenticateAs("admin@travelease.com", "ROLE_ADMIN");
        when(hotelRepository.save(any(Hotel.class))).thenAnswer(inv -> inv.getArgument(0));

        HotelRequest request = new HotelRequest(1, "New Hotel", "Addr", null, new BigDecimal("100.00"), null, "ACTIVE", PROVIDER_101);
        accommodationService.createHotel(request);

        var captor = org.mockito.ArgumentCaptor.forClass(Hotel.class);
        org.mockito.Mockito.verify(hotelRepository).save(captor.capture());
        assertThat(captor.getValue().getProviderId()).isEqualTo(PROVIDER_101);
    }

    // --- hotel ownership: get/update/policies ---

    @Test
    void hotelProviderCanAccessOwnHotel() {
        User provider = hotelProviderUser("hotelprovider1@travelease.com", PROVIDER_101);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_HOTEL_PROVIDER");
        Hotel hotel = hotelOwnedBy(PROVIDER_101);
        UUID hotelId = hotel.getId();
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(hotel));

        var response = accommodationService.getProviderHotelDetails(hotelId);

        assertThat(response.hotel().hotelId()).isEqualTo(hotelId);
    }

    @Test
    void hotelProviderCannotAccessAnotherProvidersHotel() {
        User provider = hotelProviderUser("hotelprovider1@travelease.com", PROVIDER_101);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_HOTEL_PROVIDER");
        Hotel othersHotel = hotelOwnedBy(PROVIDER_102);
        UUID hotelId = othersHotel.getId();
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(othersHotel));

        assertThatThrownBy(() -> accommodationService.getProviderHotelDetails(hotelId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void hotelProviderCannotUpdateAnotherProvidersHotel() {
        User provider = hotelProviderUser("hotelprovider1@travelease.com", PROVIDER_101);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_HOTEL_PROVIDER");
        Hotel othersHotel = hotelOwnedBy(PROVIDER_102);
        UUID hotelId = othersHotel.getId();
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(othersHotel));

        HotelRequest request = new HotelRequest(1, "Hacked Name", "Addr", null, new BigDecimal("100.00"), null, "ACTIVE", null);

        assertThatThrownBy(() -> accommodationService.updateHotel(hotelId, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void hotelProviderCannotUpdatePoliciesOfAnotherProvidersHotel() {
        User provider = hotelProviderUser("hotelprovider1@travelease.com", PROVIDER_101);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_HOTEL_PROVIDER");
        Hotel othersHotel = hotelOwnedBy(PROVIDER_102);
        UUID hotelId = othersHotel.getId();
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(othersHotel));

        assertThatThrownBy(() -> accommodationService.updatePolicies(hotelId, new com.travelease.backend.accommodation.dto.HotelPolicyRequest("No pets")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void adminBypassesHotelOwnershipForGetAndUpdate() {
        authenticateAs("admin@travelease.com", "ROLE_ADMIN");
        Hotel hotel = hotelOwnedBy(PROVIDER_101);
        UUID hotelId = hotel.getId();
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(hotel));
        when(hotelRepository.save(any(Hotel.class))).thenAnswer(inv -> inv.getArgument(0));

        var details = accommodationService.getProviderHotelDetails(hotelId);
        assertThat(details.hotel().hotelId()).isEqualTo(hotelId);

        HotelRequest request = new HotelRequest(1, "Admin Renamed", "Addr", null, new BigDecimal("100.00"), null, "ACTIVE", null);
        var updated = accommodationService.updateHotel(hotelId, request);
        assertThat(updated.hotelName()).isEqualTo("Admin Renamed");
    }

    // --- room ownership through hotel ---

    @Test
    void hotelProviderCanCreateRoomUnderOwnHotel() {
        User provider = hotelProviderUser("hotelprovider1@travelease.com", PROVIDER_101);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_HOTEL_PROVIDER");
        Hotel hotel = hotelOwnedBy(PROVIDER_101);
        UUID hotelId = hotel.getId();
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(hotel));
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

        RoomRequest request = new RoomRequest("STANDARD", 2, "QUEEN", new BigDecimal("100.00"), "AVAILABLE");
        var response = accommodationService.createRoom(hotelId, request);

        assertThat(response.roomType()).isEqualTo("STANDARD");
    }

    @Test
    void hotelProviderCannotCreateRoomUnderAnotherProvidersHotel() {
        User provider = hotelProviderUser("hotelprovider1@travelease.com", PROVIDER_101);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_HOTEL_PROVIDER");
        Hotel othersHotel = hotelOwnedBy(PROVIDER_102);
        UUID hotelId = othersHotel.getId();
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(othersHotel));

        RoomRequest request = new RoomRequest("STANDARD", 2, "QUEEN", new BigDecimal("100.00"), "AVAILABLE");

        assertThatThrownBy(() -> accommodationService.createRoom(hotelId, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void hotelProviderCannotUpdateAnotherProvidersRoom() {
        User provider = hotelProviderUser("hotelprovider1@travelease.com", PROVIDER_101);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_HOTEL_PROVIDER");
        Hotel othersHotel = hotelOwnedBy(PROVIDER_102);
        UUID hotelId = othersHotel.getId();
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(othersHotel));

        RoomRequest request = new RoomRequest("STANDARD", 2, "QUEEN", new BigDecimal("100.00"), "AVAILABLE");

        assertThatThrownBy(() -> accommodationService.updateRoom(hotelId, UUID.randomUUID(), request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void hotelProviderCannotUpdateAvailabilityOfAnotherProvidersRoom() {
        User provider = hotelProviderUser("hotelprovider1@travelease.com", PROVIDER_101);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_HOTEL_PROVIDER");
        Hotel othersHotel = hotelOwnedBy(PROVIDER_102);
        Room othersRoom = roomOf(othersHotel);
        UUID roomId = othersRoom.getId();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(othersRoom));

        assertThatThrownBy(() -> accommodationService.updateAvailability(roomId, new RoomAvailabilityRequest("MAINTENANCE")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void hotelProviderCanUpdateAvailabilityOfOwnRoom() {
        User provider = hotelProviderUser("hotelprovider1@travelease.com", PROVIDER_101);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_HOTEL_PROVIDER");
        Hotel hotel = hotelOwnedBy(PROVIDER_101);
        Room room = roomOf(hotel);
        UUID roomId = room.getId();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = accommodationService.updateAvailability(roomId, new RoomAvailabilityRequest("MAINTENANCE"));

        assertThat(response.availabilityStatus()).isEqualTo("MAINTENANCE");
    }

    @Test
    void hotelProviderCannotBlockMaintenanceOnAnotherProvidersRoom() {
        User provider = hotelProviderUser("hotelprovider1@travelease.com", PROVIDER_101);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_HOTEL_PROVIDER");
        Hotel othersHotel = hotelOwnedBy(PROVIDER_102);
        Room othersRoom = roomOf(othersHotel);
        UUID roomId = othersRoom.getId();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(othersRoom));

        assertThatThrownBy(() -> accommodationService.blockMaintenance(roomId))
                .isInstanceOf(AccessDeniedException.class);
    }

    // --- inventory scoping ---

    @Test
    void inventoryIsScopedToEffectiveProviderIdViaRepositoryQuery() {
        User provider = hotelProviderUser("hotelprovider1@travelease.com", PROVIDER_101);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_HOTEL_PROVIDER");
        Hotel hotel = hotelOwnedBy(PROVIDER_101);
        Room room = roomOf(hotel);
        when(roomRepository.findByHotel_ProviderId(PROVIDER_101)).thenReturn(List.of(room));

        var inventory = accommodationService.getInventory(null);

        assertThat(inventory).hasSize(1);
        org.mockito.Mockito.verify(roomRepository, org.mockito.Mockito.never()).findAll();
    }

    @Test
    void adminInventoryWithNoFilterReturnsAllViaFindAll() {
        authenticateAs("admin@travelease.com", "ROLE_ADMIN");
        when(roomRepository.findAll()).thenReturn(List.of());

        accommodationService.getInventory(null);

        org.mockito.Mockito.verify(roomRepository).findAll();
        org.mockito.Mockito.verify(roomRepository, org.mockito.Mockito.never()).findByHotel_ProviderId(any());
    }

    // --- check-in / check-out ownership ---

    @Test
    void hotelProviderCanCheckInBookingAtOwnHotel() {
        User provider = hotelProviderUser("hotelprovider1@travelease.com", PROVIDER_101);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_HOTEL_PROVIDER");
        Hotel hotel = hotelOwnedBy(PROVIDER_101);
        HotelBooking booking = bookingAt(hotel);
        UUID bookingId = booking.getId();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(HotelBooking.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = accommodationService.checkIn(bookingId);

        assertThat(response.bookingStatus()).isEqualTo("CHECKED_IN");
    }

    @Test
    void hotelProviderCannotCheckInBookingAtAnotherProvidersHotel() {
        User provider = hotelProviderUser("hotelprovider1@travelease.com", PROVIDER_101);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_HOTEL_PROVIDER");
        Hotel othersHotel = hotelOwnedBy(PROVIDER_102);
        HotelBooking booking = bookingAt(othersHotel);
        UUID bookingId = booking.getId();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> accommodationService.checkIn(bookingId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void hotelProviderCannotCheckOutBookingAtAnotherProvidersHotel() {
        User provider = hotelProviderUser("hotelprovider1@travelease.com", PROVIDER_101);
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        authenticateAs(provider.getEmail(), "ROLE_HOTEL_PROVIDER");
        Hotel othersHotel = hotelOwnedBy(PROVIDER_102);
        HotelBooking booking = bookingAt(othersHotel);
        UUID bookingId = booking.getId();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> accommodationService.checkOut(bookingId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void adminCanCheckInAnyProvidersBooking() {
        authenticateAs("admin@travelease.com", "ROLE_ADMIN");
        Hotel hotel = hotelOwnedBy(PROVIDER_101);
        HotelBooking booking = bookingAt(hotel);
        UUID bookingId = booking.getId();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(HotelBooking.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = accommodationService.checkIn(bookingId);

        assertThat(response.bookingStatus()).isEqualTo("CHECKED_IN");
    }

    // --- transport provider (ROLE_PROVIDER) must not gain Hotel Provider access ---

    @Test
    void transportProviderCannotAccessHotelProviderManagement() {
        authenticateAs("provider1@travelease.com", "ROLE_PROVIDER");
        Hotel hotel = hotelOwnedBy(PROVIDER_101);
        UUID hotelId = hotel.getId();
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(hotel));

        assertThatThrownBy(() -> accommodationService.getProviderHotelDetails(hotelId))
                .isInstanceOf(AccessDeniedException.class);
    }
}
