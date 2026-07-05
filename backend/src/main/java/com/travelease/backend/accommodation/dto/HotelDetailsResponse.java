package com.travelease.backend.accommodation.dto;

import java.util.List;

public record HotelDetailsResponse(
        HotelResponse hotel,
        List<RoomResponse> rooms,
        String smartSuggestion
) {
}
