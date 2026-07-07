package com.travelease.backend.busbooking.mapper;

import com.travelease.backend.busbooking.dto.request.FareRequest;
import com.travelease.backend.busbooking.dto.response.FareResponse;
import com.travelease.backend.busbooking.entity.FareRule;
import com.travelease.backend.busbooking.entity.Route;
import com.travelease.backend.busbooking.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FareMapper {

    private final RouteRepository routeRepository;

    public FareRule toEntity(FareRequest request) {
        return FareRule.builder()
                .routeId(request.getRouteId())
                .busType(request.getBusType())
                .baseFare(request.getBaseFare())
                .dynamicFareEnabled(request.getDynamicFareEnabled() != null ? request.getDynamicFareEnabled() : false)
                .occupancyThreshold1(request.getOccupancyThreshold1() != null ? request.getOccupancyThreshold1() : 50)
                .fareMultiplier1(request.getFareMultiplier1() != null ? request.getFareMultiplier1() : 1.0)
                .occupancyThreshold2(request.getOccupancyThreshold2() != null ? request.getOccupancyThreshold2() : 75)
                .fareMultiplier2(request.getFareMultiplier2() != null ? request.getFareMultiplier2() : 1.2)
                .occupancyThreshold3(request.getOccupancyThreshold3() != null ? request.getOccupancyThreshold3() : 90)
                .fareMultiplier3(request.getFareMultiplier3() != null ? request.getFareMultiplier3() : 1.5)
                .weekendSurchargePercent(request.getWeekendSurchargePercent() != null ? request.getWeekendSurchargePercent() : 0.0)
                .festivalSurchargePercent(request.getFestivalSurchargePercent() != null ? request.getFestivalSurchargePercent() : 0.0)
                .festivalStartDate(request.getFestivalStartDate())
                .festivalEndDate(request.getFestivalEndDate())
                .seasonalSurchargePercent(request.getSeasonalSurchargePercent() != null ? request.getSeasonalSurchargePercent() : 0.0)
                .seasonalStartDate(request.getSeasonalStartDate())
                .seasonalEndDate(request.getSeasonalEndDate())
                .sleeperSurchargePercent(request.getSleeperSurchargePercent() != null ? request.getSleeperSurchargePercent() : 0.0)
                .semiSleeperSurchargePercent(request.getSemiSleeperSurchargePercent() != null ? request.getSemiSleeperSurchargePercent() : 0.0)
                .luxurySurchargePercent(request.getLuxurySurchargePercent() != null ? request.getLuxurySurchargePercent() : 0.0)
                .gstPercent(request.getGstPercent() != null ? request.getGstPercent() : 5.0)
                .taxPercent(request.getTaxPercent() != null ? request.getTaxPercent() : 0.0)
                .cancellationChargePercent(request.getCancellationChargePercent() != null ? request.getCancellationChargePercent() : 10.0)
                .refundPercent(request.getRefundPercent() != null ? request.getRefundPercent() : 90.0)
                .active(request.getActive() != null ? request.getActive() : true)
                .build();
    }

    public void updateEntity(FareRule entity, FareRequest request) {
        entity.setRouteId(request.getRouteId());
        entity.setBusType(request.getBusType());
        entity.setBaseFare(request.getBaseFare());
        if (request.getDynamicFareEnabled() != null) entity.setDynamicFareEnabled(request.getDynamicFareEnabled());
        if (request.getOccupancyThreshold1() != null) entity.setOccupancyThreshold1(request.getOccupancyThreshold1());
        if (request.getFareMultiplier1() != null) entity.setFareMultiplier1(request.getFareMultiplier1());
        if (request.getOccupancyThreshold2() != null) entity.setOccupancyThreshold2(request.getOccupancyThreshold2());
        if (request.getFareMultiplier2() != null) entity.setFareMultiplier2(request.getFareMultiplier2());
        if (request.getOccupancyThreshold3() != null) entity.setOccupancyThreshold3(request.getOccupancyThreshold3());
        if (request.getFareMultiplier3() != null) entity.setFareMultiplier3(request.getFareMultiplier3());
        if (request.getWeekendSurchargePercent() != null) entity.setWeekendSurchargePercent(request.getWeekendSurchargePercent());
        if (request.getFestivalSurchargePercent() != null) entity.setFestivalSurchargePercent(request.getFestivalSurchargePercent());
        entity.setFestivalStartDate(request.getFestivalStartDate());
        entity.setFestivalEndDate(request.getFestivalEndDate());
        if (request.getSeasonalSurchargePercent() != null) entity.setSeasonalSurchargePercent(request.getSeasonalSurchargePercent());
        entity.setSeasonalStartDate(request.getSeasonalStartDate());
        entity.setSeasonalEndDate(request.getSeasonalEndDate());
        if (request.getSleeperSurchargePercent() != null) entity.setSleeperSurchargePercent(request.getSleeperSurchargePercent());
        if (request.getSemiSleeperSurchargePercent() != null) entity.setSemiSleeperSurchargePercent(request.getSemiSleeperSurchargePercent());
        if (request.getLuxurySurchargePercent() != null) entity.setLuxurySurchargePercent(request.getLuxurySurchargePercent());
        if (request.getGstPercent() != null) entity.setGstPercent(request.getGstPercent());
        if (request.getTaxPercent() != null) entity.setTaxPercent(request.getTaxPercent());
        if (request.getCancellationChargePercent() != null) entity.setCancellationChargePercent(request.getCancellationChargePercent());
        if (request.getRefundPercent() != null) entity.setRefundPercent(request.getRefundPercent());
        if (request.getActive() != null) entity.setActive(request.getActive());
    }

    public FareResponse toResponse(FareRule rule) {
        String source = null;
        String destination = null;
        Route route = routeRepository.findById(rule.getRouteId()).orElse(null);
        if (route != null) {
            source = route.getSource();
            destination = route.getDestination();
        }
        return FareResponse.builder()
                .id(rule.getId())
                .routeId(rule.getRouteId())
                .source(source)
                .destination(destination)
                .busType(rule.getBusType())
                .baseFare(rule.getBaseFare())
                .dynamicFareEnabled(rule.getDynamicFareEnabled())
                .occupancyThreshold1(rule.getOccupancyThreshold1())
                .fareMultiplier1(rule.getFareMultiplier1())
                .occupancyThreshold2(rule.getOccupancyThreshold2())
                .fareMultiplier2(rule.getFareMultiplier2())
                .occupancyThreshold3(rule.getOccupancyThreshold3())
                .fareMultiplier3(rule.getFareMultiplier3())
                .weekendSurchargePercent(rule.getWeekendSurchargePercent())
                .festivalSurchargePercent(rule.getFestivalSurchargePercent())
                .festivalStartDate(rule.getFestivalStartDate())
                .festivalEndDate(rule.getFestivalEndDate())
                .seasonalSurchargePercent(rule.getSeasonalSurchargePercent())
                .seasonalStartDate(rule.getSeasonalStartDate())
                .seasonalEndDate(rule.getSeasonalEndDate())
                .sleeperSurchargePercent(rule.getSleeperSurchargePercent())
                .semiSleeperSurchargePercent(rule.getSemiSleeperSurchargePercent())
                .luxurySurchargePercent(rule.getLuxurySurchargePercent())
                .gstPercent(rule.getGstPercent())
                .taxPercent(rule.getTaxPercent())
                .cancellationChargePercent(rule.getCancellationChargePercent())
                .refundPercent(rule.getRefundPercent())
                .active(rule.getActive())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }
}
