package com.travelease.backend.busbooking.mapper;

import com.travelease.backend.busbooking.dto.request.CouponRequest;
import com.travelease.backend.busbooking.dto.response.CouponResponse;
import com.travelease.backend.busbooking.entity.Coupon;
import org.springframework.stereotype.Component;

@Component
public class CouponMapper {

    public Coupon toEntity(CouponRequest request) {
        return Coupon.builder()
                .code(request.getCode().trim().toUpperCase())
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minFare(request.getMinFare() != null ? request.getMinFare() : 0.0)
                .maxDiscount(request.getMaxDiscount())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .maxUsage(request.getMaxUsage())
                .applicableBusTypes(request.getApplicableBusTypes())
                .applicableRouteIds(request.getApplicableRouteIds())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();
    }

    public void updateEntity(Coupon entity, CouponRequest request) {
        entity.setCode(request.getCode().trim().toUpperCase());
        entity.setDescription(request.getDescription());
        entity.setDiscountType(request.getDiscountType());
        entity.setDiscountValue(request.getDiscountValue());
        if (request.getMinFare() != null) entity.setMinFare(request.getMinFare());
        entity.setMaxDiscount(request.getMaxDiscount());
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
        entity.setMaxUsage(request.getMaxUsage());
        entity.setApplicableBusTypes(request.getApplicableBusTypes());
        entity.setApplicableRouteIds(request.getApplicableRouteIds());
        if (request.getActive() != null) entity.setActive(request.getActive());
    }

    public CouponResponse toResponse(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minFare(coupon.getMinFare())
                .maxDiscount(coupon.getMaxDiscount())
                .validFrom(coupon.getValidFrom())
                .validTo(coupon.getValidTo())
                .maxUsage(coupon.getMaxUsage())
                .usedCount(coupon.getUsedCount())
                .applicableBusTypes(coupon.getApplicableBusTypes())
                .applicableRouteIds(coupon.getApplicableRouteIds())
                .active(coupon.getActive())
                .createdAt(coupon.getCreatedAt())
                .build();
    }
}
