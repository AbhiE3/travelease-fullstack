package com.travelease.backend.accommodation.controller;

import com.travelease.backend.accommodation.dto.HotelDetailsResponse;
import com.travelease.backend.accommodation.dto.HotelBookingResponse;
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
import org.springframework.security.core.Authentication;
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
    public ResponseEntity<ApiResponse<HotelResponse>> createHotel(
            @Valid @RequestBody HotelRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                accommodationService.createHotel(request, authentication.getName()),
                "Hotel created"
        ));
    }

    @GetMapping("/hotels")
    public ResponseEntity<ApiResponse<List<HotelResponse>>> getProviderHotels(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                accommodationService.getProviderHotels(authentication.getName()),
                "Provider hotels retrieved"
        ));
    }

    @GetMapping("/hotels/{hotelId}")
    public ResponseEntity<ApiResponse<HotelDetailsResponse>> getProviderHotel(
            @PathVariable UUID hotelId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                accommodationService.getProviderHotel(hotelId, authentication.getName()),
                "Provider hotel retrieved"
        ));
    }

    @PutMapping("/hotels/{hotelId}")
    public ResponseEntity<ApiResponse<HotelResponse>> updateHotel(
            @PathVariable UUID hotelId,
            @Valid @RequestBody HotelRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                accommodationService.updateHotel(hotelId, request, authentication.getName()),
                "Hotel updated"
        ));
    }

    @PostMapping("/hotels/{hotelId}/rooms")
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(
            @PathVariable UUID hotelId,
            @Valid @RequestBody RoomRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                accommodationService.createRoom(hotelId, request, authentication.getName()),
                "Room created"
        ));
    }

    @GetMapping("/hotels/{hotelId}/rooms")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getRooms(
            @PathVariable UUID hotelId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                accommodationService.getRooms(hotelId, authentication.getName()),
                "Rooms retrieved"
        ));
    }

    @PutMapping("/hotels/{hotelId}/rooms/{roomId}")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(
            @PathVariable UUID hotelId,
            @PathVariable UUID roomId,
            @Valid @RequestBody RoomRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                accommodationService.updateRoom(hotelId, roomId, request, authentication.getName()),
                "Room updated"
        ));
    }

    @PutMapping("/rooms/{roomId}/availability")
    public ResponseEntity<ApiResponse<RoomResponse>> updateAvailability(
            @PathVariable UUID roomId,
            @Valid @RequestBody RoomAvailabilityRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                accommodationService.updateRoomAvailability(roomId, request, authentication.getName()),
                "Room availability updated"
        ));
    }

    @PutMapping("/rooms/{roomId}/maintenance")
    public ResponseEntity<ApiResponse<RoomResponse>> blockForMaintenance(
            @PathVariable UUID roomId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                accommodationService.blockRoomForMaintenance(roomId, authentication.getName()),
                "Room blocked for maintenance"
        ));
    }

    @GetMapping("/inventory")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getInventory(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                accommodationService.getProviderInventory(authentication.getName()),
                "Inventory retrieved"
        ));
    }

    @PutMapping("/hotels/{hotelId}/policies")
    public ResponseEntity<ApiResponse<HotelResponse>> updatePolicies(
            @PathVariable UUID hotelId,
            @Valid @RequestBody HotelPolicyRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                accommodationService.updatePolicies(hotelId, request, authentication.getName()),
                "Hotel policies updated"
        ));
    }

    @PutMapping("/hotel-bookings/{bookingId}/check-in")
    public ResponseEntity<ApiResponse<HotelBookingResponse>> checkIn(
            @PathVariable UUID bookingId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                accommodationService.checkIn(bookingId, authentication.getName()),
                "Guest checked in"
        ));
    }

    @PutMapping("/hotel-bookings/{bookingId}/check-out")
    public ResponseEntity<ApiResponse<HotelBookingResponse>> checkOut(
            @PathVariable UUID bookingId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                accommodationService.checkOut(bookingId, authentication.getName()),
                "Guest checked out"
        ));
    }
}
