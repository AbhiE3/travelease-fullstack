package com.travelease.backend.busbooking.service.impl;

import com.travelease.backend.busbooking.dto.request.ConductorRequest;
import com.travelease.backend.busbooking.dto.response.ConductorResponse;
import com.travelease.backend.busbooking.entity.Conductor;
import com.travelease.backend.busbooking.entity.enums.ConductorStatus;
import com.travelease.backend.busbooking.exception.ResourceNotFoundException;
import com.travelease.backend.busbooking.mapper.ConductorMapper;
import com.travelease.backend.busbooking.repository.ConductorRepository;
import com.travelease.backend.busbooking.service.ConductorService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConductorServiceImpl implements ConductorService {

    private final ConductorRepository conductorRepository;
    private final ConductorMapper conductorMapper;

    @Override
    @Transactional
    public ConductorResponse createConductor(ConductorRequest request) {
        if (conductorRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new IllegalArgumentException("Conductor with employee ID " + request.getEmployeeId() + " already exists");
        }
        Conductor conductor = conductorMapper.toEntity(request);
        Conductor saved = conductorRepository.save(conductor);
        return conductorMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ConductorResponse updateConductor(Long id, ConductorRequest request) {
        Conductor conductor = conductorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conductor not found with id: " + id));
        conductor.setName(request.getName());
        conductor.setPhone(request.getPhone());
        conductor.setEmail(request.getEmail());
        Conductor saved = conductorRepository.save(conductor);
        return conductorMapper.toResponse(saved);
    }

    @Override
    public ConductorResponse getConductorById(Long id) {
        Conductor conductor = conductorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conductor not found with id: " + id));
        return conductorMapper.toResponse(conductor);
    }

    @Override
    public List<ConductorResponse> getConductors(Long providerId, ConductorStatus status, Pageable pageable) {
        Specification<Conductor> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (providerId != null) {
                predicates.add(cb.equal(root.get("providerId"), providerId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            predicates.add(cb.equal(root.get("active"), true));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return conductorRepository.findAll(spec, pageable).stream()
                .map(conductorMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ConductorResponse updateConductorStatus(Long id, ConductorStatus status) {
        Conductor conductor = conductorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conductor not found with id: " + id));
        conductor.setStatus(status);
        Conductor saved = conductorRepository.save(conductor);
        return conductorMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deactivateConductor(Long id) {
        Conductor conductor = conductorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conductor not found with id: " + id));
        conductor.setActive(false);
        conductor.setStatus(ConductorStatus.OFF_DUTY);
        conductorRepository.save(conductor);
    }
}
