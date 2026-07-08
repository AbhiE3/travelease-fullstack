package com.travelease.backend.accommodation.controller;

import com.travelease.backend.accommodation.dto.HotelDetailsResponse;
import com.travelease.backend.accommodation.dto.HotelResponse;
import com.travelease.backend.accommodation.dto.HotelReviewResponse;
import com.travelease.backend.accommodation.dto.ReviewRequest;
import com.travelease.backend.accommodation.service.AccommodationService;
import com.travelease.backend.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final AccommodationService accommodationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<HotelResponse>>> searchHotels(
            @RequestParam(required = false) Integer destinationId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String query
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                accommodationService.searchHotels(destinationId, status, query),
                "Hotels retrieved"
        ));
    }

    @GetMapping("/{hotelId}")
    public ResponseEntity<ApiResponse<HotelDetailsResponse>> getHotelDetails(@PathVariable UUID hotelId) {
        return ResponseEntity.ok(ApiResponse.success(accommodationService.getHotelDetails(hotelId), "Hotel retrieved"));
    }

    @GetMapping("/{hotelId}/reviews")
    public ResponseEntity<ApiResponse<List<HotelReviewResponse>>> getReviews(@PathVariable UUID hotelId) {
        return ResponseEntity.ok(ApiResponse.success(accommodationService.getReviews(hotelId), "Reviews retrieved"));
    }

    @PostMapping("/{hotelId}/reviews")
    public ResponseEntity<ApiResponse<HotelReviewResponse>> addReview(
            @PathVariable UUID hotelId,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                accommodationService.addReview(hotelId, request, authentication.getName()),
                "Review added"
        ));
    }

    @PutMapping("/{hotelId}/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<HotelReviewResponse>> updateReview(
            @PathVariable UUID hotelId,
            @PathVariable UUID reviewId,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                accommodationService.updateReview(hotelId, reviewId, request, authentication.getName()),
                "Review updated"
        ));
    }

    @DeleteMapping("/{hotelId}/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable UUID hotelId,
            @PathVariable UUID reviewId,
            Authentication authentication
    ) {
        accommodationService.deleteReview(hotelId, reviewId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "Review deleted"));
    }
}
