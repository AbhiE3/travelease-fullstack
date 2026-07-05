package com.travelease.backend.settlement.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SettlementSummaryResponse(
        UUID tripId,
        BigDecimal totalPayable,
        BigDecimal totalReceivable,
        List<SettlementResponse> settlements
) {
}
