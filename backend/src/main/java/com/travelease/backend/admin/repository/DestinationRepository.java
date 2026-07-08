package com.travelease.backend.admin.repository;

import com.travelease.backend.admin.entity.Destination;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DestinationRepository extends JpaRepository<Destination, Integer> {
}
