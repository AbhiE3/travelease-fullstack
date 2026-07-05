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
public class CouponResponse {

    private Long id;
    private String code;
    private String description;
    private DiscountType discountType;
    private Double discountValue;
    private Double minFare;
    private Double maxDiscount;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Integer maxUsage;
    private Integer usedCount;
    private String applicableBusTypes;
    private String applicableRouteIds;
    private Boolean active;
    private LocalDateTime createdAt;
}
