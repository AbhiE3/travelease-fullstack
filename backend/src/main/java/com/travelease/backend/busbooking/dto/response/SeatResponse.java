package com.travelease.backend.busbooking.dto.response;

import com.travelease.backend.busbooking.entity.enums.SeatStatus;
import com.travelease.backend.busbooking.entity.enums.SeatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatResponse {

    private Long id;
    private String seatNumber;
    private SeatType seatType;
    private Integer deck;
    private SeatStatus status;
}
