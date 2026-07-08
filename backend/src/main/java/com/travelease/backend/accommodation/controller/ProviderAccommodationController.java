package com.travelease.backend.accommodation.controller;

import com.travelease.backend.accommodation.dto.HotelBookingResponse;
import com.travelease.backend.accommodation.dto.HotelDetailsResponse;
import com.travelease.backend.accommodation.dto.HotelPolicyRequest;
import com.travelease.backend.accommodation.dto.HotelRequest;
import com.travelease.backend.accommodation.dto.HotelResponse;
import com.travelease.backend.accommodation.dto.RoomAvailabilityRequest;
import com.travelease.backend.accommodation.dto.RoomRequest;
import com.travelease.backend.accommodation.dto.RoomResponse;
import com.travelease.backend.accommodation.service.AccommodationService;
import com.travelease.backend.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/provider")
@RequiredArgsConstructor
public class ProviderAccommodationController {

    private final AccommodationService accommodationService;

    @PostMapping("/hotels")
    public ResponseEntity<ApiResponse<HotelResponse>> createHotel(@Valid @RequestBody HotelRequest request) {
        HotelResponse response = accommodationService.createHotel(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Hotel created"));
    }

    @GetMapping("/hotels")
    public ResponseEntity<ApiResponse<List<HotelResponse>>> hotels() {
        List<HotelResponse> response = accommodationService.getProviderHotels();
        return ResponseEntity.ok(ApiResponse.success(response, "Provider hotels retrieved"));
    }

    @GetMapping("/hotel-bookings")
    public ResponseEntity<ApiResponse<List<HotelBookingResponse>>> bookings() {
        List<HotelBookingResponse> response = accommodationService.getProviderBookings();
        return ResponseEntity.ok(ApiResponse.success(response, "Provider hotel bookings retrieved"));
    }

    @GetMapping("/hotels/{hotelId}")
    public ResponseEntity<ApiResponse<HotelDetailsResponse>> hotelDetails(@PathVariable UUID hotelId) {
        HotelDetailsResponse response = accommodationService.getHotelDetails(hotelId);
        return ResponseEntity.ok(ApiResponse.success(response, "Provider hotel details retrieved"));
    }

    @PutMapping("/hotels/{hotelId}")
    public ResponseEntity<ApiResponse<HotelResponse>> updateHotel(
            @PathVariable UUID hotelId,
            @Valid @RequestBody HotelRequest request
    ) {
        HotelResponse response = accommodationService.updateHotel(hotelId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Hotel updated"));
    }

    @PostMapping("/hotels/{hotelId}/rooms")
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(
            @PathVariable UUID hotelId,
            @Valid @RequestBody RoomRequest request
    ) {
        RoomResponse response = accommodationService.createRoom(hotelId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Room created"));
    }

    @GetMapping("/hotels/{hotelId}/rooms")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> rooms(@PathVariable UUID hotelId) {
        List<RoomResponse> response = accommodationService.getRooms(hotelId);
        return ResponseEntity.ok(ApiResponse.success(response, "Rooms retrieved"));
    }

    @PutMapping("/hotels/{hotelId}/rooms/{roomId}")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(
            @PathVariable UUID hotelId,
            @PathVariable UUID roomId,
            @Valid @RequestBody RoomRequest request
    ) {
        RoomResponse response = accommodationService.updateRoom(hotelId, roomId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Room updated"));
    }

    @PutMapping("/rooms/{roomId}/availability")
    public ResponseEntity<ApiResponse<RoomResponse>> updateAvailability(
            @PathVariable UUID roomId,
            @Valid @RequestBody RoomAvailabilityRequest request
    ) {
        RoomResponse response = accommodationService.updateAvailability(roomId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Room availability updated"));
    }

    @PutMapping("/rooms/{roomId}/maintenance")
    public ResponseEntity<ApiResponse<RoomResponse>> maintenance(@PathVariable UUID roomId) {
        RoomResponse response = accommodationService.blockMaintenance(roomId);
        return ResponseEntity.ok(ApiResponse.success(response, "Room blocked for maintenance"));
    }

    @GetMapping("/inventory")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> inventory() {
        List<RoomResponse> response = accommodationService.getInventory();
        return ResponseEntity.ok(ApiResponse.success(response, "Inventory calendar retrieved"));
    }

    @PutMapping("/hotels/{hotelId}/policies")
    public ResponseEntity<ApiResponse<HotelResponse>> updatePolicies(
            @PathVariable UUID hotelId,
            @Valid @RequestBody HotelPolicyRequest request
    ) {
        HotelResponse response = accommodationService.updatePolicies(hotelId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Hotel policies updated"));
    }

    @PutMapping("/hotel-bookings/{bookingId}/check-in")
    public ResponseEntity<ApiResponse<HotelBookingResponse>> checkIn(@PathVariable UUID bookingId) {
        HotelBookingResponse response = accommodationService.checkIn(bookingId);
        return ResponseEntity.ok(ApiResponse.success(response, "Guest checked in"));
    }

    @PutMapping("/hotel-bookings/{bookingId}/check-out")
    public ResponseEntity<ApiResponse<HotelBookingResponse>> checkOut(@PathVariable UUID bookingId) {
        HotelBookingResponse response = accommodationService.checkOut(bookingId);
        return ResponseEntity.ok(ApiResponse.success(response, "Guest checked out"));
    }
}
