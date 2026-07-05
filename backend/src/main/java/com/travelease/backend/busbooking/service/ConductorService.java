package com.travelease.backend.busbooking.service;

import com.travelease.backend.busbooking.dto.request.ConductorRequest;
import com.travelease.backend.busbooking.dto.response.ConductorResponse;
import com.travelease.backend.busbooking.entity.enums.ConductorStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ConductorService {

    ConductorResponse createConductor(ConductorRequest request);

    ConductorResponse updateConductor(Long id, ConductorRequest request);

    ConductorResponse getConductorById(Long id);

    List<ConductorResponse> getConductors(Long providerId, ConductorStatus status, Pageable pageable);

    ConductorResponse updateConductorStatus(Long id, ConductorStatus status);

    void deactivateConductor(Long id);
}
