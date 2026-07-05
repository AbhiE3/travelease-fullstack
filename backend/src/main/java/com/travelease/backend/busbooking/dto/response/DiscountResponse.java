package com.travelease.backend.busbooking.dto.response;

import com.travelease.backend.busbooking.entity.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountResponse {

    private Long id;
    private String name;
    private String description;
    private DiscountType discountType;
    private Double discountValue;
    private String applicableRouteIds;
    private String applicableBusTypes;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Boolean active;
    private LocalDateTime createdAt;
}
