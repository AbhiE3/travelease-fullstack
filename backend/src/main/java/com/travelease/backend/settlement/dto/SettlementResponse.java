package com.travelease.backend.settlement.dto;

import com.travelease.backend.settlement.entity.SettlementStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record SettlementResponse(
        UUID id,
        UUID tripId,
        UUID payerId,
        String payerName,
        UUID receiverId,
        String receiverName,
        BigDecimal amount,
        SettlementStatus status
) {
}
