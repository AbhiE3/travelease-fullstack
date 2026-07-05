package com.travelease.backend.busbooking.repository;

import com.travelease.backend.busbooking.entity.Bus;
import com.travelease.backend.busbooking.entity.enums.BusStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusRepository extends JpaRepository<Bus, Long> {

    Optional<Bus> findByBusNumber(String busNumber);

    boolean existsByBusNumber(String busNumber);

    List<Bus> findByStatus(BusStatus status);

    long countByStatus(BusStatus status);

    List<Bus> findByProviderId(Long providerId);

    default List<Bus> findByStatusTrue() {
        return findByStatus(BusStatus.ACTIVE);
    }

    default long countByStatusTrue() {
        return countByStatus(BusStatus.ACTIVE);
    }
}
