package com.travelease.backend.busbooking.service.impl;

import com.travelease.backend.busbooking.dto.response.MaintenanceResponse;
import com.travelease.backend.busbooking.entity.Bus;
import com.travelease.backend.busbooking.entity.Maintenance;
import com.travelease.backend.busbooking.entity.enums.BusStatus;
import com.travelease.backend.busbooking.entity.enums.BusType;
import com.travelease.backend.busbooking.mapper.MaintenanceMapper;
import com.travelease.backend.busbooking.repository.BusRepository;
import com.travelease.backend.busbooking.repository.MaintenanceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Disables the production spring.sql.init.* seed-data loading for this slice test only
// (test-local override, application.properties is unchanged) - otherwise seed_data.sql's
// demo buses/maintenance records leak into this test's isolated in-memory dataset and
// break its provider-scoping assertions.
@DataJpaTest
@TestPropertySource(properties = "spring.sql.init.mode=never")
class MaintenanceServiceImplProviderScopeTest {

    @Autowired
    private MaintenanceRepository maintenanceRepository;

    @Autowired
    private BusRepository busRepository;

    private MaintenanceServiceImpl maintenanceService;

    private Bus busForProvider(Long providerId, String busNumber) {
        return Bus.builder()
                .busNumber(busNumber)
                .busName("Bus " + busNumber)
                .totalSeats(40)
                .busType(BusType.AC_SEATER)
                .providerId(providerId)
                .status(BusStatus.ACTIVE)
                .build();
    }

    @Test
    void getMaintenanceRecordsScopesToProviderWhenProviderIdSupplied() {
        maintenanceService = new MaintenanceServiceImpl(maintenanceRepository, busRepository, new MaintenanceMapper());

        Bus busProvider1 = busRepository.save(busForProvider(1L, "BUS-P1"));
        Bus busProvider2 = busRepository.save(busForProvider(2L, "BUS-P2"));

        Maintenance maintenanceProvider1 = Maintenance.builder()
                .bus(busProvider1)
                .maintenanceType("OIL_CHANGE")
                .scheduledDate(LocalDate.now())
                .build();
        Maintenance maintenanceProvider2 = Maintenance.builder()
                .bus(busProvider2)
                .maintenanceType("TIRE_ROTATION")
                .scheduledDate(LocalDate.now())
                .build();

        maintenanceRepository.save(maintenanceProvider1);
        maintenanceRepository.save(maintenanceProvider2);

        List<MaintenanceResponse> scopedToProvider1 =
                maintenanceService.getMaintenanceRecords(1L, null, null, Pageable.unpaged());

        assertThat(scopedToProvider1).hasSize(1);
        assertThat(scopedToProvider1.get(0).getBusId()).isEqualTo(busProvider1.getId());

        List<MaintenanceResponse> unfiltered =
                maintenanceService.getMaintenanceRecords(null, null, null, Pageable.unpaged());

        assertThat(unfiltered).hasSize(2);
        assertThat(unfiltered.stream().map(MaintenanceResponse::getBusId))
                .containsExactlyInAnyOrder(busProvider1.getId(), busProvider2.getId());
    }
}
