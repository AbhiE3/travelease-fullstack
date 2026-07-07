package com.travelease.backend.busbooking.service.impl;

import com.travelease.backend.busbooking.dto.request.FareCalculationRequest;
import com.travelease.backend.busbooking.dto.request.FareRequest;
import com.travelease.backend.busbooking.dto.response.*;
import com.travelease.backend.busbooking.entity.*;
import com.travelease.backend.busbooking.entity.enums.BusType;
import com.travelease.backend.busbooking.entity.enums.DiscountType;
import com.travelease.backend.busbooking.exception.ResourceNotFoundException;
import com.travelease.backend.busbooking.mapper.FareMapper;
import com.travelease.backend.busbooking.repository.*;
import com.travelease.backend.busbooking.service.CouponService;
import com.travelease.backend.busbooking.service.FareService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FareServiceImpl implements FareService {

    private final FareRuleRepository fareRuleRepository;
    private final BusScheduleRepository scheduleRepository;
    private final SeatRepository seatRepository;
    private final FareMapper fareMapper;
    private final CouponService couponService;

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // CRUD
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    @Transactional
    public FareResponse createFareRule(FareRequest request) {
        validateFareRequest(request);
        FareRule rule = fareMapper.toEntity(request);
        return fareMapper.toResponse(fareRuleRepository.save(rule));
    }

    @Override
    @Transactional
    public FareResponse updateFareRule(Long id, FareRequest request) {
        FareRule rule = fareRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FareRule", "id", id));
        validateFareRequest(request);
        fareMapper.updateEntity(rule, request);
        return fareMapper.toResponse(fareRuleRepository.save(rule));
    }

    @Override
    @Transactional
    public void deleteFareRule(Long id) {
        FareRule rule = fareRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FareRule", "id", id));
        rule.setActive(false);
        fareRuleRepository.save(rule);
    }

    @Override
    @Transactional(readOnly = true)
    public FareResponse getFareRuleById(Long id) {
        FareRule rule = fareRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FareRule", "id", id));
        return fareMapper.toResponse(rule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FareResponse> getFareRules(Long routeId, BusType busType, Boolean active) {
        return fareRuleRepository.findAll().stream()
                .filter(rule -> routeId == null || routeId.equals(rule.getRouteId()))
                .filter(rule -> busType == null || rule.getBusType() == null || rule.getBusType() == busType)
                .filter(rule -> active == null || Boolean.TRUE.equals(rule.getActive()) == active)
                .map(fareMapper::toResponse)
                .toList();
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // Pricing Engine
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    @Transactional(readOnly = true)
    public FareBreakdownResponse calculateFareBreakdown(FareCalculationRequest request) {
        BusSchedule schedule = scheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", request.getScheduleId()));

        List<Seat> seats = seatRepository.findAllById(request.getSeatIds());
        if (seats.isEmpty()) {
            throw new ResourceNotFoundException("Seats", "ids", request.getSeatIds().toString());
        }

        Route route = schedule.getRoute();
        Bus bus = schedule.getBus();
        int numSeats = seats.size();

        // Find applicable fare rule
        Optional<FareRule> fareRuleOpt = findFareRule(route.getId(), bus.getBusType());

        // Base fare from schedule (admin-set), fallback to fare rule
        double baseFarePerSeat = schedule.getFare();
        if (fareRuleOpt.isPresent()) {
            baseFarePerSeat = fareRuleOpt.get().getBaseFare();
        }

        double totalBaseFare = baseFarePerSeat * numSeats;

        // Occupancy percentage
        int totalSeats = bus.getTotalSeats();
        int bookedSeats = totalSeats - schedule.getAvailableSeats();
        double occupancyPct = totalSeats > 0 ? (bookedSeats * 100.0) / totalSeats : 0.0;

        // â”€â”€ Dynamic fare â”€â”€
        double dynamicMultiplier = 1.0;
        if (fareRuleOpt.isPresent() && Boolean.TRUE.equals(fareRuleOpt.get().getDynamicFareEnabled())) {
            dynamicMultiplier = getDynamicMultiplier(fareRuleOpt.get(), occupancyPct);
        }
        double dynamicAdjustment = totalBaseFare * (dynamicMultiplier - 1.0);

        // â”€â”€ Weekend surcharge â”€â”€
        double weekendSurcharge = 0.0;
        if (fareRuleOpt.isPresent() && isWeekend(schedule.getTravelDate())) {
            weekendSurcharge = totalBaseFare * fareRuleOpt.get().getWeekendSurchargePercent() / 100.0;
        }

        // â”€â”€ Festival surcharge â”€â”€
        double festivalSurcharge = 0.0;
        if (fareRuleOpt.isPresent() && isInDateRange(schedule.getTravelDate(),
                fareRuleOpt.get().getFestivalStartDate(), fareRuleOpt.get().getFestivalEndDate())) {
            festivalSurcharge = totalBaseFare * fareRuleOpt.get().getFestivalSurchargePercent() / 100.0;
        }

        // â”€â”€ Seasonal surcharge â”€â”€
        double seasonalSurcharge = 0.0;
        if (fareRuleOpt.isPresent() && isInDateRange(schedule.getTravelDate(),
                fareRuleOpt.get().getSeasonalStartDate(), fareRuleOpt.get().getSeasonalEndDate())) {
            seasonalSurcharge = totalBaseFare * fareRuleOpt.get().getSeasonalSurchargePercent() / 100.0;
        }

        // â”€â”€ Seat-type surcharge (per seat) â”€â”€
        double totalSeatTypeSurcharge = 0.0;
        List<FareBreakdownResponse.SeatFareBreakdown> seatBreakdowns = new ArrayList<>();
        for (Seat seat : seats) {
            double seatSurcharge = calculateSeatTypeSurcharge(seat, baseFarePerSeat, fareRuleOpt.orElse(null), bus.getBusType());
            totalSeatTypeSurcharge += seatSurcharge;

            seatBreakdowns.add(FareBreakdownResponse.SeatFareBreakdown.builder()
                    .seatId(seat.getId())
                    .seatNumber(seat.getSeatNumber())
                    .seatType(seat.getSeatType() != null ? seat.getSeatType().name() : "STANDARD")
                    .baseFare(baseFarePerSeat)
                    .seatTypeSurcharge(round2(seatSurcharge))
                    .dynamicAdjustment(round2(baseFarePerSeat * (dynamicMultiplier - 1.0)))
                    .subtotal(round2(baseFarePerSeat + seatSurcharge + baseFarePerSeat * (dynamicMultiplier - 1.0)))
                    .build());
        }

        // â”€â”€ Bus-type surcharge â”€â”€
        double busTypeSurchargePct = getBusTypeSurchargePercent(bus.getBusType());
        double busTypeSurcharge = totalBaseFare * busTypeSurchargePct / 100.0;

        // â”€â”€ Subtotal â”€â”€
        double subtotal = totalBaseFare + dynamicAdjustment + weekendSurcharge
                + festivalSurcharge + seasonalSurcharge + totalSeatTypeSurcharge + busTypeSurcharge;

        // â”€â”€ Automatic discount â”€â”€
        double discountAmount = 0.0;
        String appliedDiscount = null;
        DiscountResponse bestDiscount = couponService.findBestDiscount(route.getId(),
                bus.getBusType() != null ? bus.getBusType().name() : null, subtotal);
        if (bestDiscount != null) {
            if (bestDiscount.getDiscountType() == DiscountType.PERCENTAGE) {
                discountAmount = subtotal * bestDiscount.getDiscountValue() / 100.0;
            } else {
                discountAmount = bestDiscount.getDiscountValue();
            }
            appliedDiscount = bestDiscount.getName();
        }

        double afterDiscount = subtotal - discountAmount;

        // â”€â”€ Coupon discount â”€â”€
        double couponDiscount = 0.0;
        String appliedCoupon = null;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            try {
                CouponResponse validated = couponService.validateCoupon(
                        request.getCouponCode(), afterDiscount, route.getId(),
                        bus.getBusType() != null ? bus.getBusType().name() : null);
                if (validated.getDiscountType() == DiscountType.PERCENTAGE) {
                    couponDiscount = afterDiscount * validated.getDiscountValue() / 100.0;
                } else {
                    couponDiscount = validated.getDiscountValue();
                }
                if (validated.getMaxDiscount() != null && couponDiscount > validated.getMaxDiscount()) {
                    couponDiscount = validated.getMaxDiscount();
                }
                appliedCoupon = validated.getCode();
            } catch (Exception e) {
                log.warn("Coupon '{}' could not be applied: {}", request.getCouponCode(), e.getMessage());
            }
        }

        double afterCoupon = afterDiscount - couponDiscount;

        // â”€â”€ GST â”€â”€
        double gstPct = fareRuleOpt.map(FareRule::getGstPercent).orElse(5.0);
        double gstAmount = afterCoupon * gstPct / 100.0;

        // â”€â”€ Tax â”€â”€
        double taxPct = fareRuleOpt.map(FareRule::getTaxPercent).orElse(0.0);
        double taxAmount = afterCoupon * taxPct / 100.0;

        // â”€â”€ Final amount â”€â”€
        double finalAmount = afterCoupon + gstAmount + taxAmount;

        // â”€â”€ Cancellation / refund â”€â”€
        double cancelPct = fareRuleOpt.map(FareRule::getCancellationChargePercent).orElse(10.0);
        double refundPct = fareRuleOpt.map(FareRule::getRefundPercent).orElse(90.0);
        double cancellationCharge = finalAmount * cancelPct / 100.0;
        double refundAmount = finalAmount * refundPct / 100.0;

        // Distribute discount proportionally to seat breakdowns
        double totalDiscount = discountAmount + couponDiscount;
        for (FareBreakdownResponse.SeatFareBreakdown sb : seatBreakdowns) {
            double seatShare = subtotal > 0 ? sb.getSubtotal() / subtotal : 1.0 / numSeats;
            double seatDiscount = totalDiscount * seatShare;
            sb.setDiscount(round2(seatDiscount));
            sb.setFinalFare(round2(sb.getSubtotal() - seatDiscount
                    + (gstAmount + taxAmount) / numSeats));
        }

        return FareBreakdownResponse.builder()
                .scheduleId(schedule.getId())
                .routeId(route.getId())
                .busId(bus.getId())
                .busNumber(bus.getBusNumber())
                .busType(bus.getBusType())
                .source(route.getSource())
                .destination(route.getDestination())
                .travelDate(schedule.getTravelDate())
                .numberOfSeats(numSeats)
                .occupancyPercentage(round2(occupancyPct))
                .baseFare(round2(totalBaseFare))
                .dynamicFareAdjustment(round2(dynamicAdjustment))
                .weekendSurcharge(round2(weekendSurcharge))
                .festivalSurcharge(round2(festivalSurcharge))
                .seasonalSurcharge(round2(seasonalSurcharge))
                .seatTypeSurcharge(round2(totalSeatTypeSurcharge))
                .busTypeSurcharge(round2(busTypeSurcharge))
                .subtotal(round2(subtotal))
                .discountAmount(round2(discountAmount))
                .appliedDiscount(appliedDiscount)
                .couponDiscount(round2(couponDiscount))
                .appliedCoupon(appliedCoupon)
                .gstAmount(round2(gstAmount))
                .gstPercent(gstPct)
                .taxAmount(round2(taxAmount))
                .taxPercent(taxPct)
                .finalAmount(round2(finalAmount))
                .cancellationChargePercent(cancelPct)
                .refundPercent(refundPct)
                .cancellationCharge(round2(cancellationCharge))
                .refundAmount(round2(refundAmount))
                .seatBreakdowns(seatBreakdowns)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PriceCalculatorResponse calculatePrice(FareCalculationRequest request) {
        FareBreakdownResponse breakdown = calculateFareBreakdown(request);
        double savings = (breakdown.getDiscountAmount() != null ? breakdown.getDiscountAmount() : 0.0)
                + (breakdown.getCouponDiscount() != null ? breakdown.getCouponDiscount() : 0.0);
        return PriceCalculatorResponse.builder()
                .breakdown(breakdown)
                .totalPayable(breakdown.getFinalAmount())
                .totalSavings(round2(savings))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CancellationPreviewResponse getCancellationPreview(Long scheduleId, Double totalFare) {
        BusSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", scheduleId));
        Optional<FareRule> fareRuleOpt = findFareRule(schedule.getRoute().getId(),
                schedule.getBus().getBusType());
        double cancelPct = fareRuleOpt.map(FareRule::getCancellationChargePercent).orElse(10.0);
        double refundPct = fareRuleOpt.map(FareRule::getRefundPercent).orElse(90.0);
        double cancellationCharge = round2(totalFare * cancelPct / 100.0);
        double refundableAmount = round2(totalFare * refundPct / 100.0);

        return CancellationPreviewResponse.builder()
                .scheduleId(scheduleId)
                .originalFare(totalFare)
                .cancellationChargePercent(cancelPct)
                .cancellationCharge(cancellationCharge)
                .refundPercent(refundPct)
                .refundableAmount(refundableAmount)
                .build();
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // Private helpers
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private Optional<FareRule> findFareRule(Long routeId, BusType busType) {
        List<FareRule> rules = fareRuleRepository.findApplicableFareRules(routeId, busType);
        if (rules.isEmpty()) {
            return Optional.empty();
        }
        // Prefer bus-type-specific rule over generic
        return rules.stream()
                .filter(r -> r.getBusType() != null && r.getBusType() == busType)
                .findFirst()
                .or(() -> Optional.of(rules.get(0)));
    }

    private double getDynamicMultiplier(FareRule rule, double occupancyPct) {
        if (occupancyPct >= rule.getOccupancyThreshold3()) {
            return rule.getFareMultiplier3();
        } else if (occupancyPct >= rule.getOccupancyThreshold2()) {
            return rule.getFareMultiplier2();
        } else if (occupancyPct >= rule.getOccupancyThreshold1()) {
            return rule.getFareMultiplier1();
        }
        return 1.0;
    }

    private double calculateSeatTypeSurcharge(Seat seat, double baseFare, FareRule rule, BusType busType) {
        if (rule == null || busType == null) return 0.0;
        // Seat-type surcharge is derived from the bus type since all seats on a bus share the same comfort level
        return switch (busType) {
            case AC_SLEEPER, NON_AC_SLEEPER -> baseFare * rule.getSleeperSurchargePercent() / 100.0;
            case AC_SEMI_SLEEPER, NON_AC_SEMI_SLEEPER -> baseFare * rule.getSemiSleeperSurchargePercent() / 100.0;
            case AC_LUXURY, NON_AC_LUXURY -> baseFare * rule.getLuxurySurchargePercent() / 100.0;
            default -> 0.0;
        };
    }

    private double getBusTypeSurchargePercent(BusType busType) {
        if (busType == null) return 0.0;
        return switch (busType) {
            case AC_SLEEPER -> 20.0;
            case NON_AC_SLEEPER -> 10.0;
            case AC_SEMI_SLEEPER -> 15.0;
            case NON_AC_SEMI_SLEEPER -> 10.0;
            case AC_LUXURY -> 30.0;
            case NON_AC_LUXURY -> 25.0;
            case AC_SEATER -> 15.0;
            case NON_AC_SEATER -> 5.0;
        };
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    private boolean isInDateRange(LocalDate date, LocalDate from, LocalDate to) {
        if (from == null || to == null) return false;
        return !date.isBefore(from) && !date.isAfter(to);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private void validateFareRequest(FareRequest request) {
        if (request.getBaseFare() != null && request.getBaseFare() < 0) {
            throw new IllegalArgumentException("Base fare cannot be negative");
        }
        if (request.getGstPercent() != null && request.getGstPercent() < 0) {
            throw new IllegalArgumentException("GST percentage cannot be negative");
        }
        if (request.getRefundPercent() != null && (request.getRefundPercent() < 0 || request.getRefundPercent() > 100)) {
            throw new IllegalArgumentException("Refund percentage must be between 0 and 100");
        }
        if (request.getCancellationChargePercent() != null && (request.getCancellationChargePercent() < 0 || request.getCancellationChargePercent() > 100)) {
            throw new IllegalArgumentException("Cancellation charge percentage must be between 0 and 100");
        }
        if (request.getFestivalStartDate() != null && request.getFestivalEndDate() != null
                && request.getFestivalStartDate().isAfter(request.getFestivalEndDate())) {
            throw new IllegalArgumentException("Festival start date cannot be after end date");
        }
        if (request.getSeasonalStartDate() != null && request.getSeasonalEndDate() != null
                && request.getSeasonalStartDate().isAfter(request.getSeasonalEndDate())) {
            throw new IllegalArgumentException("Seasonal start date cannot be after end date");
        }
    }
}
