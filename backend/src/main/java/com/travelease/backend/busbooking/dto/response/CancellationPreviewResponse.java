package com.travelease.backend.busbooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Combined cancellation preview for a schedule: shows the original fare alongside
 * the cancellation charge and the resulting refundable amount in a single call.
 * Replaces the separate cancellation-charge and refund calculation endpoints,
 * which were always consumed together for the same cancellation-preview screen.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancellationPreviewResponse {

    private Long scheduleId;
    private Double originalFare;
    private Double cancellationChargePercent;
    private Double cancellationCharge;
    private Double refundPercent;
    private Double refundableAmount;
}
