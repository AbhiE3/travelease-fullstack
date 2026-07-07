package com.travelease.backend.busbooking.service;

import com.travelease.backend.busbooking.dto.request.DriverRequest;
import com.travelease.backend.busbooking.dto.response.DriverResponse;
import com.travelease.backend.busbooking.entity.enums.DriverStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DriverService {

    DriverResponse createDriver(DriverRequest request);

    DriverResponse updateDriver(Long id, DriverRequest request);

    DriverResponse getDriverById(Long id);

    List<DriverResponse> getDrivers(Long providerId, DriverStatus status, Pageable pageable);

    void deactivateDriver(Long id);
}
