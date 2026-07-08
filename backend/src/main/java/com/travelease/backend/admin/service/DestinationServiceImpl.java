package com.travelease.backend.admin.service;

import com.travelease.backend.admin.dto.DestinationResponse;
import com.travelease.backend.admin.entity.Destination;
import com.travelease.backend.admin.repository.DestinationRepository;
import com.travelease.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DestinationServiceImpl implements DestinationService {

    private final DestinationRepository destinationRepository;

    @Override
    public List<DestinationResponse> getAllDestinations() {
        return destinationRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public DestinationResponse getDestinationById(Integer destinationId) {
        Destination destination = destinationRepository.findById(destinationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Destination with id " + destinationId + " not found"));
        return toResponse(destination);
    }

    private DestinationResponse toResponse(Destination destination) {
        return new DestinationResponse(
                destination.getDestinationId(),
                destination.getDestinationName(),
                destination.getState(),
                destination.getCountry(),
                destination.getDescription()
        );
    }
}
