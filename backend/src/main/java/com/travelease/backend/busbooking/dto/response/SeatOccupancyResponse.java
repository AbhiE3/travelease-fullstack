package com.travelease.backend.busbooking.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Seat occupancy statistics for a schedule")
public class SeatOccupancyResponse {

    @Schema(description = "Schedule ID")
    private Long scheduleId;

    @Schema(description = "Total number of seats in the bus")
    private int totalSeats;

    @Schema(description = "Number of seats already booked")
    private int bookedSeats;

    @Schema(description = "Number of seats currently available for booking")
    private int availableSeats;

    @Schema(description = "Number of seats currently locked by other users")
    private int lockedSeats;

    @Schema(description = "Occupancy percentage (booked + locked) / total * 100")
    private double occupancyPercentage;
}
