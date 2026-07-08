package com.travelease.backend.admin.service;

import com.travelease.backend.admin.dto.DestinationResponse;
import com.travelease.backend.admin.entity.Destination;
import com.travelease.backend.admin.repository.DestinationRepository;
import com.travelease.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DestinationServiceImplTest {

    @Mock
    private DestinationRepository destinationRepository;

    @InjectMocks
    private DestinationServiceImpl destinationService;

    private Destination sampleDestination() {
        Destination destination = new Destination();
        destination.setDestinationId(1);
        destination.setDestinationName("Mumbai");
        destination.setState("Maharashtra");
        destination.setCountry("India");
        destination.setDescription("Financial capital of India");
        return destination;
    }

    @Test
    void getAllDestinationsMapsEveryRow() {
        when(destinationRepository.findAll()).thenReturn(List.of(sampleDestination()));

        List<DestinationResponse> result = destinationService.getAllDestinations();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).destinationId()).isEqualTo(1);
        assertThat(result.get(0).destinationName()).isEqualTo("Mumbai");
        assertThat(result.get(0).state()).isEqualTo("Maharashtra");
    }

    @Test
    void getDestinationByIdReturnsMappedResponseWhenFound() {
        when(destinationRepository.findById(1)).thenReturn(Optional.of(sampleDestination()));

        DestinationResponse result = destinationService.getDestinationById(1);

        assertThat(result.destinationName()).isEqualTo("Mumbai");
    }

    @Test
    void getDestinationByIdThrowsWhenNotFound() {
        when(destinationRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> destinationService.getDestinationById(99))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
