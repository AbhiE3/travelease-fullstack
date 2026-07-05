package com.travelease.backend.settlement.mapper;

import com.travelease.backend.settlement.dto.SettlementResponse;
import com.travelease.backend.settlement.entity.Settlement;
import org.springframework.stereotype.Component;

@Component
public class SettlementMapper {

    public SettlementResponse toResponse(Settlement settlement) {
        return new SettlementResponse(
                settlement.getId(),
                settlement.getTrip().getId(),
                settlement.getPayer().getId(),
                settlement.getPayer().getName(),
                settlement.getReceiver().getId(),
                settlement.getReceiver().getName(),
                settlement.getAmount(),
                settlement.getStatus()
        );
    }
}
