package com.travelease.backend.busbooking.repository;

import com.travelease.backend.busbooking.entity.ReportHistory;
import com.travelease.backend.busbooking.entity.enums.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportHistoryRepository extends JpaRepository<ReportHistory, Long>, JpaSpecificationExecutor<ReportHistory> {

    List<ReportHistory> findByReportTypeOrderByGeneratedAtDesc(ReportType reportType);

    List<ReportHistory> findByProviderIdOrderByGeneratedAtDesc(Long providerId);

    Page<ReportHistory> findByProviderIdOrderByGeneratedAtDesc(Long providerId, Pageable pageable);

    List<ReportHistory> findTop10ByOrderByGeneratedAtDesc();
}
