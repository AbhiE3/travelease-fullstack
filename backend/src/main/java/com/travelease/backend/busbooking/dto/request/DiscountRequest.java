package com.travelease.backend.busbooking.dto.request;

import com.travelease.backend.busbooking.entity.enums.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountRequest {

    @NotBlank(message = "Discount name is required")
    private String name;

    private String description;

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @Positive(message = "Discount value must be positive")
    private Double discountValue;

    private String applicableRouteIds; // comma-separated
    private String applicableBusTypes; // comma-separated

    private LocalDate validFrom;
    private LocalDate validTo;

    private Boolean active;
}
