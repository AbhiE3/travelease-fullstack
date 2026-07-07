package com.travelease.backend.busbooking.service.impl;

import com.travelease.backend.busbooking.dto.request.CouponRequest;
import com.travelease.backend.busbooking.dto.request.DiscountRequest;
import com.travelease.backend.busbooking.dto.response.CouponResponse;
import com.travelease.backend.busbooking.dto.response.DiscountResponse;
import com.travelease.backend.busbooking.entity.Coupon;
import com.travelease.backend.busbooking.entity.Discount;
import com.travelease.backend.busbooking.entity.enums.DiscountType;
import com.travelease.backend.busbooking.exception.CouponException;
import com.travelease.backend.busbooking.exception.ResourceNotFoundException;
import com.travelease.backend.busbooking.mapper.CouponMapper;
import com.travelease.backend.busbooking.mapper.DiscountMapper;
import com.travelease.backend.busbooking.repository.CouponRepository;
import com.travelease.backend.busbooking.repository.DiscountRepository;
import com.travelease.backend.busbooking.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final DiscountRepository discountRepository;
    private final CouponMapper couponMapper;
    private final DiscountMapper discountMapper;

    @Override
    @Transactional
    public CouponResponse createCoupon(CouponRequest request) {
        if (couponRepository.existsByCode(request.getCode().trim().toUpperCase())) {
            throw new CouponException("Coupon code '" + request.getCode() + "' already exists");
        }
        validateCouponDates(request.getValidFrom(), request.getValidTo());
        validateDiscountValue(request.getDiscountType(), request.getDiscountValue());
        Coupon coupon = couponMapper.toEntity(request);
        return couponMapper.toResponse(couponRepository.save(coupon));
    }

    @Override
    @Transactional
    public CouponResponse updateCoupon(Long id, CouponRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));
        String newCode = request.getCode().trim().toUpperCase();
        couponRepository.findByCode(newCode).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new CouponException("Coupon code '" + newCode + "' already exists");
            }
        });
        validateCouponDates(request.getValidFrom(), request.getValidTo());
        validateDiscountValue(request.getDiscountType(), request.getDiscountValue());
        couponMapper.updateEntity(coupon, request);
        return couponMapper.toResponse(couponRepository.save(coupon));
    }

    @Override
    @Transactional
    public void deactivateCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));
        coupon.setActive(false);
        couponRepository.save(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponse getCouponById(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));
        return couponMapper.toResponse(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponse> getCoupons(Boolean active, LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        return couponRepository.findAll().stream()
                .filter(coupon -> active == null || Boolean.TRUE.equals(coupon.getActive()) == active)
                .filter(coupon -> from == null || (coupon.getValidFrom() != null && !coupon.getValidFrom().isBefore(from)))
                .filter(coupon -> to == null || (coupon.getValidTo() != null && !coupon.getValidTo().isAfter(to)))
                .filter(coupon -> coupon.getValidFrom() == null || !today.isBefore(coupon.getValidFrom()))
                .filter(coupon -> coupon.getValidTo() == null || !today.isAfter(coupon.getValidTo()))
                .map(couponMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponse validateCoupon(String code, Double fareAmount, Long routeId, String busType) {
        Coupon coupon = couponRepository.findByCode(code.trim().toUpperCase())
                .orElseThrow(() -> new CouponException("Invalid coupon code: " + code));

        if (!Boolean.TRUE.equals(coupon.getActive())) {
            throw new CouponException("Coupon '" + code + "' is inactive");
        }

        LocalDate today = LocalDate.now();
        if (today.isBefore(coupon.getValidFrom())) {
            throw new CouponException("Coupon '" + code + "' is not yet valid. Valid from: " + coupon.getValidFrom());
        }
        if (today.isAfter(coupon.getValidTo())) {
            throw new CouponException("Coupon '" + code + "' has expired. Valid till: " + coupon.getValidTo());
        }

        if (coupon.getMaxUsage() != null && coupon.getUsedCount() >= coupon.getMaxUsage()) {
            throw new CouponException("Coupon '" + code + "' has reached its usage limit");
        }

        if (fareAmount != null && coupon.getMinFare() != null && fareAmount < coupon.getMinFare()) {
            throw new CouponException("Coupon '" + code + "' requires a minimum fare of " + coupon.getMinFare());
        }

        if (coupon.getApplicableBusTypes() != null && !coupon.getApplicableBusTypes().isBlank() && busType != null) {
            List<String> allowed = Arrays.asList(coupon.getApplicableBusTypes().split(","));
            if (!allowed.stream().anyMatch(b -> b.trim().equalsIgnoreCase(busType))) {
                throw new CouponException("Coupon '" + code + "' is not applicable for bus type: " + busType);
            }
        }

        if (coupon.getApplicableRouteIds() != null && !coupon.getApplicableRouteIds().isBlank() && routeId != null) {
            List<String> allowed = Arrays.asList(coupon.getApplicableRouteIds().split(","));
            if (!allowed.stream().anyMatch(r -> r.trim().equals(routeId.toString()))) {
                throw new CouponException("Coupon '" + code + "' is not applicable for this route");
            }
        }

        return couponMapper.toResponse(coupon);
    }

    @Override
    @Transactional
    public void incrementCouponUsage(String code) {
        couponRepository.findByCode(code.trim().toUpperCase()).ifPresent(coupon -> {
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            couponRepository.save(coupon);
        });
    }

    @Override
    @Transactional
    public DiscountResponse createDiscount(DiscountRequest request) {
        validateDiscountValue(request.getDiscountType(), request.getDiscountValue());
        if (request.getValidFrom() != null && request.getValidTo() != null) {
            validateCouponDates(request.getValidFrom(), request.getValidTo());
        }
        Discount discount = discountMapper.toEntity(request);
        return discountMapper.toResponse(discountRepository.save(discount));
    }

    @Override
    @Transactional
    public DiscountResponse updateDiscount(Long id, DiscountRequest request) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount", "id", id));
        validateDiscountValue(request.getDiscountType(), request.getDiscountValue());
        if (request.getValidFrom() != null && request.getValidTo() != null) {
            validateCouponDates(request.getValidFrom(), request.getValidTo());
        }
        discountMapper.updateEntity(discount, request);
        return discountMapper.toResponse(discountRepository.save(discount));
    }

    @Override
    @Transactional
    public void deactivateDiscount(Long id) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount", "id", id));
        discount.setActive(false);
        discountRepository.save(discount);
    }

    @Override
    @Transactional(readOnly = true)
    public DiscountResponse getDiscountById(Long id) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount", "id", id));
        return discountMapper.toResponse(discount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiscountResponse> getDiscounts(Boolean active, LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        return discountRepository.findAll().stream()
                .filter(discount -> active == null || Boolean.TRUE.equals(discount.getActive()) == active)
                .filter(discount -> from == null || (discount.getValidFrom() != null && !discount.getValidFrom().isBefore(from)))
                .filter(discount -> to == null || (discount.getValidTo() != null && !discount.getValidTo().isAfter(to)))
                .filter(discount -> discount.getValidFrom() == null || !today.isBefore(discount.getValidFrom()))
                .filter(discount -> discount.getValidTo() == null || !today.isAfter(discount.getValidTo()))
                .map(discountMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DiscountResponse findBestDiscount(Long routeId, String busType, Double fareAmount) {
        List<Discount> activeDiscounts = discountRepository.findAll().stream()
                .filter(d -> Boolean.TRUE.equals(d.getActive()))
                .filter(d -> isApplicable(d, routeId, busType))
                .toList();

        Discount best = activeDiscounts.stream()
                .max((d1, d2) -> Double.compare(calculateDiscountValue(d1, fareAmount), calculateDiscountValue(d2, fareAmount)))
                .orElse(null);

        return best != null ? discountMapper.toResponse(best) : null;
    }

    private boolean isApplicable(Discount d, Long routeId, String busType) {
        if (d.getApplicableRouteIds() != null && !d.getApplicableRouteIds().isBlank() && routeId != null) {
            List<String> allowed = Arrays.asList(d.getApplicableRouteIds().split(","));
            if (allowed.stream().noneMatch(r -> r.trim().equals(routeId.toString()))) {
                return false;
            }
        }
        if (d.getApplicableBusTypes() != null && !d.getApplicableBusTypes().isBlank() && busType != null) {
            List<String> allowed = Arrays.asList(d.getApplicableBusTypes().split(","));
            if (allowed.stream().noneMatch(b -> b.trim().equalsIgnoreCase(busType))) {
                return false;
            }
        }
        return true;
    }

    private double calculateDiscountValue(Discount d, Double fareAmount) {
        if (d.getDiscountType() == DiscountType.PERCENTAGE) {
            return fareAmount * d.getDiscountValue() / 100.0;
        }
        return d.getDiscountValue();
    }

    private void validateCouponDates(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new CouponException("Valid-from date cannot be after valid-to date");
        }
    }

    private void validateDiscountValue(DiscountType type, Double value) {
        if (value == null || value <= 0) {
            throw new CouponException("Discount value must be positive");
        }
        if (type == DiscountType.PERCENTAGE && value > 100.0) {
            throw new CouponException("Percentage discount cannot exceed 100%");
        }
    }
}
