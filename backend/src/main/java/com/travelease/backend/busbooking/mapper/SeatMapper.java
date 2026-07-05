package com.travelease.backend.busbooking.mapper;

import com.travelease.backend.busbooking.dto.response.SeatResponse;
import com.travelease.backend.busbooking.entity.Seat;
import org.springframework.stereotype.Component;

@Component
public class SeatMapper {

    public SeatResponse toResponse(Seat seat) {
        return SeatResponse.builder()
                .id(seat.getId())
                .seatNumber(seat.getSeatNumber())
                .seatType(seat.getSeatType())
                .deck(seat.getDeck())
                .status(seat.getStatus())
                .build();
    }
}
