package com.travelease.backend.busbooking.mapper;

import com.travelease.backend.busbooking.dto.request.DiscountRequest;
import com.travelease.backend.busbooking.dto.response.DiscountResponse;
import com.travelease.backend.busbooking.entity.Discount;
import org.springframework.stereotype.Component;

@Component
public class DiscountMapper {

    public Discount toEntity(DiscountRequest request) {
        return Discount.builder()
                .name(request.getName())
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .applicableRouteIds(request.getApplicableRouteIds())
                .applicableBusTypes(request.getApplicableBusTypes())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();
    }

    public void updateEntity(Discount entity, DiscountRequest request) {
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setDiscountType(request.getDiscountType());
        entity.setDiscountValue(request.getDiscountValue());
        entity.setApplicableRouteIds(request.getApplicableRouteIds());
        entity.setApplicableBusTypes(request.getApplicableBusTypes());
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
        if (request.getActive() != null) entity.setActive(request.getActive());
    }

    public DiscountResponse toResponse(Discount discount) {
        return DiscountResponse.builder()
                .id(discount.getId())
                .name(discount.getName())
                .description(discount.getDescription())
                .discountType(discount.getDiscountType())
                .discountValue(discount.getDiscountValue())
                .applicableRouteIds(discount.getApplicableRouteIds())
                .applicableBusTypes(discount.getApplicableBusTypes())
                .validFrom(discount.getValidFrom())
                .validTo(discount.getValidTo())
                .active(discount.getActive())
                .createdAt(discount.getCreatedAt())
                .build();
    }
}
