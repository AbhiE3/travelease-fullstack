package com.travelease.backend.busbooking.service.impl;

import com.travelease.backend.busbooking.dto.request.BusRequest;
import com.travelease.backend.busbooking.dto.response.BusResponse;
import com.travelease.backend.busbooking.entity.Bus;
import com.travelease.backend.busbooking.entity.Seat;
import com.travelease.backend.busbooking.entity.enums.BusStatus;
import com.travelease.backend.busbooking.entity.enums.BusType;
import com.travelease.backend.busbooking.entity.enums.SeatStatus;
import com.travelease.backend.busbooking.entity.enums.SeatType;
import com.travelease.backend.busbooking.exception.ResourceNotFoundException;
import com.travelease.backend.busbooking.mapper.BusMapper;
import com.travelease.backend.busbooking.repository.BusRepository;
import com.travelease.backend.busbooking.repository.SeatRepository;
import com.travelease.backend.busbooking.service.BusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusServiceImpl implements BusService {

    private final BusRepository busRepository;
    private final SeatRepository seatRepository;
    private final BusMapper busMapper;

    @Override
    @Transactional
    public BusResponse createBus(BusRequest request) {
        if (busRepository.existsByBusNumber(request.getBusNumber())) {
            throw new IllegalArgumentException("Bus number already exists: " + request.getBusNumber());
        }

        Bus bus = Bus.builder()
                .busNumber(request.getBusNumber())
                .busName(request.getBusName())
                .totalSeats(request.getTotalSeats())
                .providerId(request.getProviderId())
                .busType(request.getBusType())
                .amenities(normalizeAmenities(request.getAmenities()))
                .status(BusStatus.ACTIVE)
                .build();

        Bus savedBus = busRepository.save(bus);
        seatRepository.saveAll(generateSeats(savedBus, request.getTotalSeats()));
        return busMapper.toResponse(savedBus);
    }

    @Override
    @Transactional
    public BusResponse updateBus(Long id, BusRequest request) {
        Bus bus = busRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bus", "id", id));

        if (!bus.getBusNumber().equals(request.getBusNumber())
                && busRepository.existsByBusNumber(request.getBusNumber())) {
            throw new IllegalArgumentException("Bus number already exists: " + request.getBusNumber());
        }

        if (!Objects.equals(bus.getTotalSeats(), request.getTotalSeats())
                && !seatRepository.findByBusId(bus.getId()).isEmpty()) {
            throw new IllegalArgumentException("Bus capacity cannot be changed after seats are generated");
        }

        bus.setBusNumber(request.getBusNumber());
        bus.setBusName(request.getBusName());
        bus.setTotalSeats(request.getTotalSeats());
        bus.setProviderId(request.getProviderId());
        bus.setBusType(request.getBusType());
        bus.setAmenities(normalizeAmenities(request.getAmenities()));

        return busMapper.toResponse(busRepository.save(bus));
    }

    @Override
    @Transactional
    public void deleteBus(Long id) {
        Bus bus = busRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bus", "id", id));
        bus.setStatus(BusStatus.INACTIVE);
        busRepository.save(bus);
    }

    @Override
    @Transactional(readOnly = true)
    public BusResponse getBusById(Long id) {
        Bus bus = busRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bus", "id", id));
        return busMapper.toResponse(bus);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusResponse> getBuses(Long providerId, BusStatus status) {
        return busRepository.findAll().stream()
                .filter(bus -> providerId == null || Objects.equals(bus.getProviderId(), providerId))
                .filter(bus -> status == null || bus.getStatus() == status)
                .map(busMapper::toResponse)
                .toList();
    }

    private List<Seat> generateSeats(Bus bus, int totalSeats) {
        List<Seat> seats = new ArrayList<>();
        int totalDecks = isSleeper(bus) ? 2 : 1;
        int seatsPerDeck = totalSeats / totalDecks;

        for (int deck = 1; deck <= totalDecks; deck++) {
            int seatsInThisDeck = (deck == 1) ? seatsPerDeck : (totalSeats - seatsPerDeck);
            for (int i = 1; i <= seatsInThisDeck; i++) {
                String seatPrefix = (deck == 1) ? "L" : "U";
                String seatNumber = seatPrefix + i;
                SeatType seatType = (i % 4 == 1 || i % 4 == 0) ? SeatType.WINDOW : SeatType.AISLE;

                if (deck == 1 && i <= 2) {
                    seatType = SeatType.LADIES;
                }

                seats.add(Seat.builder()
                        .bus(bus)
                        .seatNumber(seatNumber)
                        .seatType(seatType)
                        .deck(deck)
                        .status(SeatStatus.AVAILABLE)
                        .build());
            }
        }

        return seats;
    }

    private boolean isSleeper(Bus bus) {
        BusType type = bus.getBusType();
        return type == BusType.AC_SLEEPER || type == BusType.NON_AC_SLEEPER;
    }

    private List<String> normalizeAmenities(List<String> amenities) {
        if (amenities == null) {
            return new ArrayList<>();
        }

        return amenities.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
