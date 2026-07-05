package com.travelease.backend.busbooking.service;

import com.travelease.backend.busbooking.dto.request.CouponRequest;
import com.travelease.backend.busbooking.dto.request.DiscountRequest;
import com.travelease.backend.busbooking.dto.response.CouponResponse;
import com.travelease.backend.busbooking.dto.response.DiscountResponse;

import java.time.LocalDate;
import java.util.List;

public interface CouponService {

    CouponResponse createCoupon(CouponRequest request);

    CouponResponse updateCoupon(Long id, CouponRequest request);

    void deactivateCoupon(Long id);

    CouponResponse getCouponById(Long id);

    List<CouponResponse> getCoupons(Boolean active, LocalDate from, LocalDate to);

    CouponResponse validateCoupon(String code, Double fareAmount, Long routeId, String busType);

    void incrementCouponUsage(String code);

    DiscountResponse createDiscount(DiscountRequest request);

    DiscountResponse updateDiscount(Long id, DiscountRequest request);

    void deactivateDiscount(Long id);

    DiscountResponse getDiscountById(Long id);

    List<DiscountResponse> getDiscounts(Boolean active, LocalDate from, LocalDate to);

    DiscountResponse findBestDiscount(Long routeId, String busType, Double fareAmount);
}
