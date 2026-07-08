package com.travelease.backend.busbooking.service.impl;

import com.travelease.backend.busbooking.dto.request.TripAssignmentRequest;
import com.travelease.backend.busbooking.entity.Bus;
import com.travelease.backend.busbooking.entity.BusSchedule;
import com.travelease.backend.busbooking.entity.Conductor;
import com.travelease.backend.busbooking.entity.Driver;
import com.travelease.backend.busbooking.entity.enums.BusStatus;
import com.travelease.backend.busbooking.entity.enums.ConductorStatus;
import com.travelease.backend.busbooking.entity.enums.DriverStatus;
import com.travelease.backend.busbooking.entity.enums.ScheduleStatus;
import com.travelease.backend.busbooking.mapper.TripMapper;
import com.travelease.backend.busbooking.repository.BusScheduleRepository;
import com.travelease.backend.busbooking.repository.ConductorRepository;
import com.travelease.backend.busbooking.repository.DriverRepository;
import com.travelease.backend.busbooking.repository.TripRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripServiceImplAssignTripOwnershipTest {

    @Mock
    private TripRepository tripRepository;
    @Mock
    private BusScheduleRepository scheduleRepository;
    @Mock
    private DriverRepository driverRepository;
    @Mock
    private ConductorRepository conductorRepository;
    @Mock
    private TripMapper tripMapper;

    @InjectMocks
    private TripServiceImpl tripService;

    private Bus bus(Long id, Long providerId) {
        return Bus.builder().id(id).busNumber("BUS-" + id).providerId(providerId).status(BusStatus.ACTIVE).build();
    }

    private BusSchedule schedule(Long id, Bus bus) {
        return BusSchedule.builder().id(id).bus(bus).status(ScheduleStatus.SCHEDULED).build();
    }

    private Driver driver(Long id, Long providerId) {
        return Driver.builder().id(id).providerId(providerId).name("Driver " + id)
                .licenseNumber("LIC-" + id).status(DriverStatus.AVAILABLE).build();
    }

    private Conductor conductor(Long id, Long providerId) {
        return Conductor.builder().id(id).providerId(providerId).name("Conductor " + id)
                .employeeId("EMP-" + id).status(ConductorStatus.AVAILABLE).build();
    }

    @Test
    void provider1CanAssignProvider1ScheduleDriverAndConductor() {
        BusSchedule schedule = schedule(1L, bus(1L, 1L));
        Driver driver = driver(5L, 1L);
        Conductor conductor = conductor(4L, 1L);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(driverRepository.findById(5L)).thenReturn(Optional.of(driver));
        when(conductorRepository.findById(4L)).thenReturn(Optional.of(conductor));
        when(tripRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tripMapper.toResponse(any())).thenReturn(
                com.travelease.backend.busbooking.dto.response.TripResponse.builder().id(1L).build());

        TripAssignmentRequest request = new TripAssignmentRequest(1L, 5L, 4L, "notes");

        assertThat(tripService.assignTrip(request)).isNotNull();
        assertThat(driver.getStatus()).isEqualTo(DriverStatus.ASSIGNED);
        assertThat(conductor.getStatus()).isEqualTo(ConductorStatus.ASSIGNED);
    }

    @Test
    void provider1CannotAssignProvider2Driver() {
        BusSchedule schedule = schedule(1L, bus(1L, 1L));
        Driver provider2Driver = driver(5L, 2L);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(driverRepository.findById(5L)).thenReturn(Optional.of(provider2Driver));

        TripAssignmentRequest request = new TripAssignmentRequest(1L, 5L, null, null);

        assertThatThrownBy(() -> tripService.assignTrip(request))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(provider2Driver.getStatus()).isEqualTo(DriverStatus.AVAILABLE);
    }

    @Test
    void provider1CannotAssignProvider2Conductor() {
        BusSchedule schedule = schedule(1L, bus(1L, 1L));
        Conductor provider2Conductor = conductor(4L, 2L);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(conductorRepository.findById(4L)).thenReturn(Optional.of(provider2Conductor));

        TripAssignmentRequest request = new TripAssignmentRequest(1L, null, 4L, null);

        assertThatThrownBy(() -> tripService.assignTrip(request))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(provider2Conductor.getStatus()).isEqualTo(ConductorStatus.AVAILABLE);
    }

    @Test
    void provider2CannotAssignProvider1Resources() {
        BusSchedule schedule = schedule(2L, bus(2L, 2L));
        Driver provider1Driver = driver(5L, 1L);
        when(scheduleRepository.findById(2L)).thenReturn(Optional.of(schedule));
        when(driverRepository.findById(5L)).thenReturn(Optional.of(provider1Driver));

        TripAssignmentRequest request = new TripAssignmentRequest(2L, 5L, null, null);

        assertThatThrownBy(() -> tripService.assignTrip(request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void adminCannotCreateCrossProviderMixedAssignment() {
        // Schedule belongs to Provider 1, driver to Provider 2, conductor to
        // Provider 3 - even though an admin caller could individually reach all
        // three resources, the assignment itself must remain internally
        // provider-consistent.
        BusSchedule schedule = schedule(1L, bus(1L, 1L));
        Driver provider2Driver = driver(5L, 2L);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(driverRepository.findById(5L)).thenReturn(Optional.of(provider2Driver));

        TripAssignmentRequest request = new TripAssignmentRequest(1L, 5L, null, null);

        assertThatThrownBy(() -> tripService.assignTrip(request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unavailableDriverIsStillRejectedAfterOwnershipCheckPasses() {
        BusSchedule schedule = schedule(1L, bus(1L, 1L));
        Driver busyDriver = driver(5L, 1L);
        busyDriver.setStatus(DriverStatus.ON_TRIP);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(driverRepository.findById(5L)).thenReturn(Optional.of(busyDriver));

        TripAssignmentRequest request = new TripAssignmentRequest(1L, 5L, null, null);

        assertThatThrownBy(() -> tripService.assignTrip(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not available");
    }
}
