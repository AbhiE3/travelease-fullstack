package com.travelease.backend.busbooking.dto.response;

import com.travelease.backend.busbooking.entity.enums.BusType;
import com.travelease.backend.busbooking.entity.enums.BusStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusResponse {

    private Long id;
    private String busNumber;
    private String busName;
    private Integer totalSeats;
    private Long providerId;
    private BusType busType;
    private List<String> amenities;
    private BusStatus status;
    private LocalDateTime createdAt;
}
