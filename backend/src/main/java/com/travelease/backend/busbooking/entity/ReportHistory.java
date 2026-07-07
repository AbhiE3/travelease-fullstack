package com.travelease.backend.busbooking.entity;

import com.travelease.backend.busbooking.entity.enums.ReportType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "report_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_name", nullable = false)
    private String reportName;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false)
    private ReportType reportType;

    @Column(name = "generated_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime generatedAt;

    @Column(name = "generated_by")
    private String generatedBy;

    @Column(name = "applied_filters", length = 2000)
    private String appliedFilters;

    @Column(name = "export_format")
    private String exportFormat; // CSV, EXCEL, JSON

    @Column(name = "record_count")
    private Integer recordCount;

    @Column(name = "provider_id")
    private Long providerId;
}
