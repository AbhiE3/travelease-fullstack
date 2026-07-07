package com.travelease.backend.busbooking.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response for seat lock operation")
public class SeatLockResponse {

    @Schema(description = "Schedule ID for which seats were locked")
    private Long scheduleId;

    @Schema(description = "List of seat IDs that were successfully locked")
    private List<Long> lockedSeatIds;

    @Schema(description = "Timestamp when the locks were created")
    private LocalDateTime lockedAt;

    @Schema(description = "Timestamp when the locks will automatically expire")
    private LocalDateTime expiresAt;

    @Schema(description = "Status message")
    private String message;
}
