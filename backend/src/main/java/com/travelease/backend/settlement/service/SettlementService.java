package com.travelease.backend.settlement.service;

import com.travelease.backend.settlement.dto.SettlementResponse;
import com.travelease.backend.settlement.dto.SettlementSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface SettlementService {

    List<SettlementResponse> getMySettlements(UUID tripId, String currentUserEmail);

    SettlementSummaryResponse getTripSummary(UUID tripId, String currentUserEmail);

    SettlementResponse markPaid(UUID settlementId, String currentUserEmail);
}
